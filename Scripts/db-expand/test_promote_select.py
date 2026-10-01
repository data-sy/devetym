#!/usr/bin/env python3
"""
승격 자격 오라클 (W1c).

이 파일이 지키는 것은 하나다: **페이지가 되면 안 되는 행이 승격 후보에 섞이지 않는다.**

프로덕션 generated 24행 중 14행은 `not_dev_term`·`possible_typo`다. `origin`만으로 가르면
그 14행이 그대로 색인 대상이 되고, 사이트맵에 "개발 용어가 아닙니다"라는 페이지가 실린다.
그래서 자격 필터는 SQL과 파이썬 **양쪽**에 걸려 있고, 여기서 두 쪽을 다 못 박는다 —
한쪽만 검사하면 rows를 손으로 뽑는 날 다른 쪽이 조용히 없어진다.

실행:
    python3 Scripts/db-expand/test_promote_select.py
"""

from __future__ import annotations

import io
import json
import sys
from contextlib import redirect_stderr
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from authored_version import BUNDLE_FIELDS  # noqa: E402
from promote_select import (  # noqa: E402
    SELECT_SQL,
    check_bundle_collisions,
    enforce_eligibility,
    load_rows,
    project,
    rewrite_packet,
)
from validator import validate  # noqa: E402

failures: list[str] = []


def check(cond: bool, msg: str) -> None:
    if not cond:
        failures.append(msg)


def row(term_key: str, branch: str, origin: str, **payload_extra) -> dict:
    payload = {
        "keyword": term_key,
        "aliases": ["한글별칭"],
        "category": "기타",
        "summary": "테스트용 요약 문장입니다 정말로",
        "etymology": "테스트용 어원 서술" * 5,
        "namingReason": "테스트용 작명 이유 서술" * 12,
        "schemaVersion": 1,
        "promptVersion": "v2-pathA:956ba44a7c48",
    }
    payload.update(payload_extra)
    return {
        "term_key": term_key,
        "branch": branch,
        "origin": origin,
        "payload": json.dumps(payload, ensure_ascii=False),
        "prompt_version": "v2-pathA:956ba44a7c48",
        "hit_count": 0,
        "created_at": "2026-09-02T00:00:00.000Z",
    }


def test_sql_carries_both_filters() -> None:
    """SQL 한쪽만 봐도 자격이 드러나야 한다 — 사람이 손으로 조회할 때 쓰는 문장이다."""
    check("origin='generated'" in SELECT_SQL, "SELECT_SQL에 origin 필터가 없다")
    check("branch='term_entry'" in SELECT_SQL, "SELECT_SQL에 branch 필터가 없다")
    print("  SQL 자격 필터 ✓ origin + branch")


def test_python_side_filter_is_independent() -> None:
    """SQL을 안 거친 rows(손으로 뽑은 것)도 파이썬에서 다시 걸러야 한다."""
    rows = [
        row("mutex", "term_entry", "generated"),
        row("아무말", "not_dev_term", "generated"),
        row("teh", "possible_typo", "generated"),
        row("daemon", "term_entry", "authored"),
    ]
    with redirect_stderr(io.StringIO()):
        kept = enforce_eligibility(rows)
    check(len(kept) == 1, f"자격 통과가 1행이어야 하는데 {len(kept)}행")
    check(kept and kept[0]["term_key"] == "mutex", "term_entry/generated 행만 남아야 한다")
    print("  파이썬 자격 필터 ✓ not_dev_term·possible_typo·authored 배제")


def test_projection_drops_version_fields() -> None:
    """번들 엔트리는 6필드다. 버전 2필드를 옮기면 시딩 태그가 두 벌 섞인다."""
    entries = project([row("mutex", "term_entry", "generated")])
    check(len(entries) == 1, "투영 결과가 1건이 아니다")
    check(
        tuple(entries[0].keys()) == BUNDLE_FIELDS,
        f"6필드·원래 순서여야 하는데 {tuple(entries[0].keys())}",
    )
    print("  payload 투영 ✓ 6필드 · 버전 2필드 제거")


def test_bundle_collision_is_detected() -> None:
    """번들에 이미 있는 용어는 승격 대상이 아니다 — 정규화 키로 본다."""
    hits = check_bundle_collisions([{"keyword": "MU TEX"}, {"keyword": "이건없는용어"}])
    check(hits == ["MU TEX"], f"정규화 후 번들과 겹치는 것을 못 잡았다: {hits}")
    print("  번들 충돌 검출 ✓ 정규화 키 기준")


def test_rewrite_packet_names_every_failure() -> None:
    """요청서가 한 건이라도 빠뜨리면 그 용어는 조용히 승격에서 사라진다."""
    entries = [
        {
            "keyword": "toolong",
            "aliases": ["툴롱"],
            "category": "기타",
            "summary": "규격을 넘기려고 일부러 아주 길게 늘여 쓴 요약 문장입니다",
            "etymology": "짧다",
            "namingReason": "짧다",
        }
    ]
    result = validate(entries)
    check(result["failed"], "이 픽스처는 반드시 위반이어야 한다")
    packet = rewrite_packet(entries, result["failed"])
    check("## toolong" in packet, "요청서에 대상 keyword가 없다")
    for f in result["failed"]:
        check(f["rule_id"] in packet, f"요청서에 위반 사유 {f['rule_id']}가 없다")
    check("```json" in packet, "요청서에 현재 본문이 실리지 않았다")
    print(f"  리라이트 요청서 ✓ 위반 {len(result['failed'])}건 전부 실림")


def test_wrangler_and_flat_rows_both_load() -> None:
    """조회 경로가 둘이다 — wrangler --json 출력과 손으로 만든 배열."""
    tmp = Path(__file__).parent / ".test-rows.json"
    wrangler_shape = [{"results": [row("mutex", "term_entry", "generated")], "success": True}]
    try:
        tmp.write_text(json.dumps(wrangler_shape, ensure_ascii=False), encoding="utf-8")
        check(len(load_rows(tmp)) == 1, "wrangler --json 형태를 못 읽는다")
        tmp.write_text(json.dumps(wrangler_shape[0]["results"], ensure_ascii=False), encoding="utf-8")
        check(len(load_rows(tmp)) == 1, "평평한 배열을 못 읽는다")
    finally:
        tmp.unlink(missing_ok=True)
    print("  행 로딩 ✓ wrangler --json · 평평한 배열")


def main() -> int:
    test_sql_carries_both_filters()
    test_python_side_filter_is_independent()
    test_projection_drops_version_fields()
    test_bundle_collision_is_detected()
    test_rewrite_packet_names_every_failure()
    test_wrangler_and_flat_rows_both_load()
    if failures:
        print(f"FAIL — {len(failures)}건")
        for m in failures:
            print(f"  {m}")
        return 1
    print("PASS — 승격 자격 (branch 필터 양쪽 · 6필드 투영 · 충돌 검출 · 요청서 누락 0)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

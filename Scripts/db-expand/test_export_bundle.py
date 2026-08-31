#!/usr/bin/env python3
"""
익스포트 왕복 오라클 (W0c §3-6).

두 가지를 지킨다:

  1. **손편집 탐지** — 현행 `terms.json`의 센티널이 `bundle-snapshot.json`이 기록한 값과
     같아야 한다. JSON 배열에는 "직접 편집 금지" 주석을 달 수 없어서, 마커를 **읽히는 글자**가
     아니라 **깨지는 검사**로 만들었다. 아무도 안 읽는 주석보다 강하다.

  2. **완전 왕복** — terms.json → 시딩 SQL → 실 SQLite → 익스포트 → **바이트 동일**.
     D1 없이 닫힌다(D1도 SQLite다). "의미적 동일"로 느슨하게 잡지 않는 이유:
     형식이 조금씩 흐르면 커밋마다 650줄 diff가 나고, 그러면 아무도 diff를 안 읽는다 —
     ADR-0012가 완화책으로 삼은 리뷰 지점이 실제로 사라진다.

실행:
    python3 Scripts/db-expand/test_export_bundle.py
"""

from __future__ import annotations

import io
import json
import sqlite3
import sys
from contextlib import redirect_stderr
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from authored_version import bundle_prompt_version  # noqa: E402
from export_bundle import BUNDLE, SNAPSHOT, rows_to_entries, serialize  # noqa: E402
from seed_d1 import BUNDLE_CREATED_AT, build  # noqa: E402
from term_key import normalize_term_key  # noqa: E402

MIGRATIONS = Path.home() / "devetym-proxy/migrations/cache"

failures: list[str] = []


def check(cond: bool, msg: str) -> None:
    if not cond:
        failures.append(msg)


def test_snapshot_matches_bundle() -> None:
    """★ 손편집 탐지. terms.json을 손으로 고치면 여기서 깨진다."""
    if not SNAPSHOT.exists():
        failures.append(f"{SNAPSHOT.name}이 없다 — 익스포트를 한 번도 안 돌렸다")
        return
    snap = json.loads(SNAPSHOT.read_text(encoding="utf-8"))
    entries = json.loads(BUNDLE.read_text(encoding="utf-8"))
    live = bundle_prompt_version(entries)
    check(
        snap["sentinel"] == live,
        f"terms.json이 기록된 스냅샷과 다르다 — 손편집했거나 익스포트를 안 돌렸다 "
        f"(기록 {snap['sentinel']} · 실제 {live})",
    )
    check(
        snap["entries"] == len(entries),
        f"엔트리 수 불일치: 기록 {snap['entries']} · 실제 {len(entries)}",
    )
    check("편집하지" in snap.get("_marker", ""), "마커 문구가 없다")
    print(f"  스냅샷 일치 ✓ {live} · {len(entries)}행")


def test_full_round_trip() -> None:
    """terms.json → 시딩 SQL → 실 SQLite → 익스포트 → 바이트 동일."""
    if not MIGRATIONS.exists():
        print("  ⚠️ 프록시 마이그레이션 없음 — 왕복 건너뜀")
        return

    original = BUNDLE.read_text(encoding="utf-8")
    entries = json.loads(original)

    db = sqlite3.connect(":memory:")
    for m in sorted(MIGRATIONS.glob("*.sql")):
        db.executescript(m.read_text(encoding="utf-8"))

    with redirect_stderr(io.StringIO()):
        sql, _ = build(entries, BUNDLE_CREATED_AT)
    db.executescript("\n".join(l for l in sql if not l.startswith("--")))

    rows = [
        {"payload": p, "prompt_version": v}
        for p, v in db.execute(
            "SELECT payload, prompt_version FROM entries WHERE origin='authored'"
        )
    ]
    check(len(rows) == len(entries), f"authored 행 {len(rows)} ≠ {len(entries)}")

    exported, tag = rows_to_entries(rows)
    text = serialize(exported)

    check(text == original, "익스포트가 원본과 바이트 동일하지 않다 — 왕복이 안 닫혔다")
    check(
        bundle_prompt_version(exported) == tag,
        f"익스포트 결과의 센티널이 D1 태그와 다르다 ({bundle_prompt_version(exported)} ≠ {tag})",
    )
    print(f"  왕복 ✓ {len(rows)}행 · 바이트 동일 · 센티널 {tag}")


def test_projection_and_order() -> None:
    """6필드만·원래 키 순서·keyword 원문 사전순."""
    entries = json.loads(BUNDLE.read_text(encoding="utf-8"))
    keys = [list(e.keys()) for e in entries]
    check(
        all(k == ["keyword", "aliases", "category", "summary", "etymology", "namingReason"] for k in keys),
        "번들 엔트리의 키 순서가 정본 6필드 순서가 아니다",
    )
    kw = [e["keyword"] for e in entries]
    check(kw == sorted(kw), "keyword 원문 사전순이 아니다")

    # 정규화 키 정렬과 **다른 순서**임을 못 박는다 — 둘을 혼동하면 diff가 매번 터진다
    nk = [normalize_term_key(k) for k in kw]
    check(
        nk != sorted(nk),
        "정규화 키 정렬과 원문 정렬이 같아졌다 — 이 테스트의 전제가 바뀌었으니 확인 필요",
    )
    print(f"  투영·정렬 ✓ 6필드 · keyword 원문순(정규화순과 다름)")


def main() -> int:
    test_snapshot_matches_bundle()
    test_full_round_trip()
    test_projection_and_order()
    if failures:
        print(f"FAIL — {len(failures)}건")
        for m in failures:
            print(f"  {m}")
        return 1
    print("PASS — 익스포트 왕복 (바이트 동일) · 손편집 탐지")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

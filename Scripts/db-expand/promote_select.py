#!/usr/bin/env python3
"""
승격 잡 후보 선별·분류 (W1c).

웹/앱에서 AI가 만든 `origin='generated'` 행 중 **페이지가 될 자격이 있는 것**을 골라,
번들 정량 게이트로 즉시 승격분과 리라이트 대상으로 가른다. 여기서 나온 산출물은
기존 수동 라운드 런북(`docs/db-expand/runbook-manual-round.md`)의 입력으로 그대로 들어간다.

    D1 generated ──[이 스크립트]──┬─ ready.json ──→ critic(탭 B) ─┐
                                  │                                ├─→ merge.py → seed_d1.py
                                  └─ 리라이트 요청 ──→ 탭 A ───────┘        → export_bundle.py

⚠️ **`origin`만으로 가르면 안 된다.** 프로덕션 generated 24행 중 14행은 `not_dev_term`·
   `possible_typo`다 — 그것도 정상 산출물이지만 **페이지가 되면 안 되는 행**이다.
   `branch='term_entry'`가 최소 조건이고, 이 필터는 SQL과 파이썬 **양쪽**에 건다.
   한쪽만 걸면 rows를 손으로 뽑은 날 조용히 샌다.

⚠️ **번들 규격을 낮추지 않는다** (2026-09-02 사람 판정). authored가 되면 그 행은
   `export_bundle.py` 대상이라 **앱 번들 `terms.json`에도 실린다**(ADR-0012). 그래서 길이·형식
   규격을 정하는 쪽은 웹 색인이 아니라 앱이고, 위반분은 기준을 완화하는 게 아니라 **리라이트한다.**

⚠️ **버전 2필드는 버린다.** 번들 엔트리는 6필드다(`authored_version.BUNDLE_FIELDS`).
   generated payload에 실려 있는 `schemaVersion`·`promptVersion`은 D1 전용이며,
   승격분은 시딩이 authored 센티널로 **다시 매긴다** — 여기서 옮기면 태그가 두 벌 섞인다.

사용:
    # 1) 후보 조회 (프록시 repo에서 · Node 22 필요)
    cd ~/devetym-proxy && source ~/.nvm/nvm.sh && nvm use 22
    npx wrangler d1 execute devetym-cache --remote --json \\
      --command "$(python3 ~/devetym/Scripts/db-expand/promote_select.py --sql)" > /tmp/cands.json

    # 2) 선별·분류
    cd ~/devetym && python3 Scripts/db-expand/promote_select.py --rows /tmp/cands.json --out-dir /tmp/w1c

**stdout = 없음 · stderr = 판정 로그 · 산출은 --out-dir에.** 조용한 성공을 만들지 않는다.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

sys.path.insert(0, str(Path(__file__).parent))
from authored_version import BUNDLE_FIELDS  # noqa: E402
from term_key import normalize_term_key  # noqa: E402
from validator import validate  # noqa: E402

REPO = Path(__file__).resolve().parents[2]
BUNDLE = REPO / "shared/src/commonMain/composeResources/files/terms.json"

# 승격 자격의 최소 조건. `origin`은 후보 집합을, `branch`는 **페이지가 될 자격**을 가른다.
ELIGIBLE_ORIGIN = "generated"
ELIGIBLE_BRANCH = "term_entry"

SELECT_SQL = (
    "SELECT term_key, branch, origin, payload, prompt_version, hit_count, created_at "
    f"FROM entries WHERE origin='{ELIGIBLE_ORIGIN}' AND branch='{ELIGIBLE_BRANCH}' "
    "ORDER BY hit_count DESC, created_at"
)

# 리라이트 요청서에 그대로 실을 목표 범위 (validator.LEN_RULES와 같은 값 — 사람이 읽는 쪽)
LEN_TARGETS = {
    "summary": "20~30자",
    "etymology": "60~120자",
    "namingReason": "150~270자",
}


def log(msg: str = "") -> None:
    print(msg, file=sys.stderr)


def load_rows(path: Path) -> list[dict[str, Any]]:
    """wrangler `--json` 출력에서 행을 꺼낸다. 손으로 만든 평평한 배열도 받는다."""
    data = json.loads(path.read_text(encoding="utf-8"))
    if isinstance(data, list) and data and isinstance(data[0], dict) and "results" in data[0]:
        rows: list[dict[str, Any]] = []
        for block in data:
            rows.extend(block.get("results", []))
        return rows
    if isinstance(data, list):
        return data
    raise SystemExit(f"{path}에서 행을 찾지 못했다 — wrangler --json 출력이거나 JSON 배열이어야 한다")


def enforce_eligibility(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """SQL을 믿지 않고 다시 건다. 필터가 빠진 rows를 받으면 여기서 멈춘다."""
    kept, dropped = [], []
    for r in rows:
        if r.get("origin") != ELIGIBLE_ORIGIN or r.get("branch") != ELIGIBLE_BRANCH:
            dropped.append(r)
        else:
            kept.append(r)
    if dropped:
        log(f"  자격 필터에서 제외 {len(dropped)}행 — origin/branch 불일치")
        for r in dropped[:10]:
            log(f"    {r.get('term_key','?'):<22} branch={r.get('branch')} origin={r.get('origin')}")
    return kept


def project(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """payload → 번들 6필드. 버전 2필드는 버린다."""
    entries = []
    for r in rows:
        payload = json.loads(r["payload"])
        missing = [f for f in BUNDLE_FIELDS if f not in payload]
        if missing:
            raise SystemExit(f"FAIL — {r['term_key']}의 payload에 필드가 없다: {missing}")
        entries.append({f: payload[f] for f in BUNDLE_FIELDS})
    return entries


def check_bundle_collisions(entries: list[dict[str, Any]]) -> list[str]:
    """번들과 정규화 키가 겹치는 후보를 잡는다 — 승격하면 PK 충돌이거나 정본 덮어쓰기다."""
    bundle = json.loads(BUNDLE.read_text(encoding="utf-8"))
    bundle_keys = {normalize_term_key(e["keyword"]) for e in bundle}
    return [e["keyword"] for e in entries if normalize_term_key(e["keyword"]) in bundle_keys]


def rewrite_packet(entries: list[dict[str, Any]], failed: list[dict[str, Any]]) -> str:
    """탭 A(Generator)에 그대로 붙여넣는 리라이트 요청서."""
    by_kw: dict[str, list[dict[str, Any]]] = {}
    for f in failed:
        by_kw.setdefault(f["keyword"], []).append(f)
    index = {e["keyword"]: e for e in entries}

    out = [
        "# 🤖 W1c 승격 — 규격 리라이트 요청",
        "",
        "아래 entry들은 이미 서비스에 서 있는 본문이지만 번들 정량 규격을 벗어났다.",
        "**내용·사실관계는 유지하고 규격만 맞춰** 다시 써라. 새 용어를 만들지 말 것.",
        "출력은 순수 JSON 배열 하나(펜스·설명 금지), 각 객체는 6필드",
        "(`keyword`·`aliases`·`category`·`summary`·`etymology`·`namingReason`).",
        "",
        f"대상 {len(by_kw)}건.",
        "",
    ]
    for kw, viols in by_kw.items():
        e = index[kw]
        out.append(f"## {kw}")
        out.append("")
        out.append("위반:")
        for v in viols:
            out.append(f"- `{v['rule_id']}` — {v['reason']}")
        out.append("")
        out.append("현재 본문:")
        out.append("```json")
        out.append(json.dumps(e, ensure_ascii=False, indent=2))
        out.append("```")
        out.append("")
        out.append(
            "목표: "
            + " · ".join(f"{k} {v}" for k, v in LEN_TARGETS.items())
            + " · keyword는 영문 소문자/숫자/하이픈/언더스코어만 · aliases에 한글 표기 1개 이상."
        )
        out.append("")
    return "\n".join(out)


def main() -> int:
    parser = argparse.ArgumentParser(description="W1c 승격 후보 선별·분류")
    parser.add_argument("--sql", action="store_true", help="후보 조회 SQL만 출력하고 종료")
    parser.add_argument("--rows", type=Path, help="wrangler --json 출력 파일")
    parser.add_argument("--out-dir", type=Path, help="산출물 디렉토리")
    args = parser.parse_args()

    if args.sql:
        print(SELECT_SQL)
        return 0
    if not args.rows or not args.out_dir:
        parser.error("--rows 와 --out-dir 가 필요하다 (또는 --sql)")

    rows = enforce_eligibility(load_rows(args.rows))
    if not rows:
        log("승격 후보 0건 — 할 일이 없다.")
        return 0

    entries = project(rows)
    log(f"후보 {len(entries)}건 (origin={ELIGIBLE_ORIGIN} · branch={ELIGIBLE_BRANCH})")

    collisions = check_bundle_collisions(entries)
    if collisions:
        raise SystemExit(
            "FAIL — 번들에 이미 있는 용어가 후보에 섞였다: "
            f"{collisions}\n  generated 행이 authored 키와 겹치면 시딩 충돌 규칙이 이미 처리했어야 한다. "
            "D1 상태를 먼저 확인할 것."
        )

    result = validate(entries)
    ready = [e for e in entries if e["keyword"] in set(result["passed"])]
    needs_rewrite = [e for e in entries if e["keyword"] not in set(result["passed"])]

    args.out_dir.mkdir(parents=True, exist_ok=True)
    (args.out_dir / "candidates.json").write_text(
        json.dumps(entries, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (args.out_dir / "ready.json").write_text(
        json.dumps(ready, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (args.out_dir / "validator-report.json").write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    packet_path = args.out_dir / "🤖-리라이트-요청.md"
    if needs_rewrite:
        packet_path.write_text(rewrite_packet(entries, result["failed"]), encoding="utf-8")

    log("")
    log(f"  즉시 critic 대상 (정량 통과) : {len(ready)}건 → {args.out_dir/'ready.json'}")
    log(f"  리라이트 대상               : {len(needs_rewrite)}건 · 위반 {len(result['failed'])}건")
    for f in result["failed"]:
        log(f"    {f['keyword']:<16} {f['rule_id']:<18} {f['reason']}")
    if needs_rewrite:
        log(f"  리라이트 요청서            : {packet_path}")
    log("")
    log("  다음 = critic(탭 B · 임시챗) 심사 → merge.py → seed_d1.py → export_bundle.py → 웹 재배포")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

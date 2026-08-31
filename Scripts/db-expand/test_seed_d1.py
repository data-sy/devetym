#!/usr/bin/env python3
"""
시딩 회계 오라클 (W0c §3-4).

§7이 확정한 수를 **여기서 잠근다.** 이 숫자들은 §3-1 정규화 정의의 직접적 귀결이라,
정의가 어디선가 안 쓰이면 조용히 달라진다 — 그리고 조용한 변화는 D1에 붓기 전엔 안 보인다.

    entries 650 · PK 충돌 0 · aliases 1,292 · 엔트리간 충돌 3 · 자기접힘 1

특히 **PK 충돌 0**은 "행이 사라지지 않는다"와 같은 말이다. 1이라도 생기면 시딩이
650행을 넣었다고 보고하면서 실제로는 649행만 넣는다.

실행:
    python3 Scripts/db-expand/test_seed_d1.py
"""

from __future__ import annotations

import io
import json
import re
import sys
from contextlib import redirect_stderr
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from seed_d1 import BUNDLE, BUNDLE_CREATED_AT, build  # noqa: E402

# §7 확정값 — 바꾸려면 실측 근거와 함께 SSOT §7도 같이 고쳐야 한다.
EXPECTED = {
    "entries": 650,
    "aliases": 1292,
    "pk_collisions": 0,
    "cross_entry": 3,
    "self_fold": 1,
    "shadowed": 88,
}
EXPECTED_CROSS = {"집계", "분기", "샤딩"}

failures: list[str] = []


def check(cond: bool, msg: str) -> None:
    if not cond:
        failures.append(msg)


def main() -> int:
    entries = json.loads(BUNDLE.read_text(encoding="utf-8"))
    log = io.StringIO()
    with redirect_stderr(log):
        sql, n_alias = build(entries, BUNDLE_CREATED_AT)
    text = log.getvalue()

    def num(pattern: str) -> int | None:
        m = re.search(pattern, text)
        return int(m.group(1)) if m else None

    got = {
        "entries": len(entries),
        "aliases": n_alias,
        "pk_collisions": num(r"PK 충돌 (\d+)"),
        "cross_entry": num(r"엔트리간 충돌 (\d+)"),
        "self_fold": num(r"자기접힘 (\d+)"),
        "shadowed": num(r"엔트리 키에 가려짐 (\d+)"),
    }
    for k, want in EXPECTED.items():
        check(got[k] == want, f"{k}: 기대 {want} · 실제 {got[k]}")

    cross = set(re.findall(r"· '([^']+)': \w", text))
    check(cross == EXPECTED_CROSS, f"엔트리간 충돌 키: 기대 {EXPECTED_CROSS} · 실제 {cross}")

    # SQL 자체 검증 — 문장 수와 순서(aliases는 FK 때문에 반드시 뒤)
    entry_stmts = [i for i, l in enumerate(sql) if l.startswith("INSERT INTO entries")]
    alias_stmts = [i for i, l in enumerate(sql) if l.startswith("INSERT INTO aliases")]
    check(len(entry_stmts) == EXPECTED["entries"], f"entries 문장 {len(entry_stmts)}")
    check(len(alias_stmts) == EXPECTED["aliases"], f"aliases 문장 {len(alias_stmts)}")
    check(
        not entry_stmts or not alias_stmts or max(entry_stmts) < min(alias_stmts),
        "aliases 문장이 entries보다 앞에 있다 — FK 위반",
    )
    check(
        all("ON CONFLICT(term_key) DO NOTHING" in sql[i] for i in entry_stmts),
        "entries INSERT에 ON CONFLICT 가드가 빠졌다 — 재실행이 깨진다",
    )
    check(
        all("WHERE NOT EXISTS" in sql[i] for i in alias_stmts),
        "aliases INSERT에 entries-우선 가드가 빠졌다",
    )
    # 작은따옴표가 들어간 값이 이스케이프됐는지 (SQL 문법 파손 방지)
    quoted = [l for l in sql if l.startswith("INSERT") and l.count("'") % 2 != 0]
    check(not quoted, f"따옴표가 홀수인 문장 {len(quoted)}건 — 이스케이프 누락")

    print(f"  {got}")
    print(f"  엔트리간 충돌 키 {sorted(cross)}")
    print(f"  SQL 문장 entries {len(entry_stmts)} · aliases {len(alias_stmts)} · 순서 ✓")

    if failures:
        print(f"FAIL — {len(failures)}건")
        for m in failures:
            print(f"  {m}")
        return 1
    print("PASS — 시딩 회계 (§7 확정값 일치)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

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
import sqlite3
import sys
from contextlib import redirect_stderr
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from seed_d1 import BUNDLE, BUNDLE_CREATED_AT, build  # noqa: E402
from authored_version import bundle_prompt_version  # noqa: E402

MIGRATIONS = Path.home() / "devetym-proxy/migrations/cache"

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



def test_conflict_rule_on_real_sqlite(entries, sql) -> None:
    """
    생성된 SQL을 **실제 SQLite 엔진에 돌린다** (§3-5).

    문장 텍스트 검사만으로는 `DO UPDATE … WHERE`가 정말 그렇게 구는지 알 수 없다.
    D1은 SQLite이므로 같은 DDL 위에서 돌리면 동작이 같다.
    """
    if not MIGRATIONS.exists():
        print("  ⚠️ 프록시 마이그레이션 없음 — 엔진 실행 건너뜀")
        return

    db = sqlite3.connect(":memory:")
    for m in sorted(MIGRATIONS.glob("*.sql")):
        db.executescript(m.read_text(encoding="utf-8"))

    # 650과 term_key가 겹치는 generated 행 3종을 심는다 — 세 분기를 다 덮는다
    planted = [
        ("aatree", "term_entry", '{"summary":"AI판"}', 42),
        ("abaproblem", "not_dev_term", "{}", 7),
        ("abstractfactory", "possible_typo", '{"suggestion":"x"}', 3),
    ]
    db.executemany(
        "INSERT INTO entries (term_key, branch, payload, prompt_version, schema_version,"
        " created_at, hit_count) VALUES (?, ?, ?, 'v2-pathA:956ba44a7c48', 1,"
        " '2026-08-01T00:00:00.000Z', ?)",
        planted,
    )

    def run() -> None:
        db.executescript("\n".join(l for l in sql if not l.startswith("--")))

    run()
    rows = {
        r[0]: r
        for r in db.execute(
            "SELECT term_key, branch, origin, hit_count FROM entries WHERE term_key IN"
            " ('aatree','abaproblem','abstractfactory')"
        )
    }
    for key, _, _, hits in planted:
        r = rows.get(key)
        check(r is not None, f"{key} 행이 사라졌다")
        if r:
            check(r[1] == "term_entry", f"{key} branch={r[1]} — 오판 분기가 안 고쳐졌다")
            check(r[2] == "authored", f"{key} origin={r[2]} — authored가 졌다")
            check(r[3] == hits, f"{key} hit_count={r[3]} ≠ {hits} — 요청 빈도가 지워졌다")

    n_entries = db.execute("SELECT COUNT(*) FROM entries").fetchone()[0]
    n_versions = db.execute("SELECT COUNT(*) FROM entry_versions").fetchone()[0]
    n_alias = db.execute("SELECT COUNT(*) FROM aliases").fetchone()[0]
    check(n_entries == len(entries), f"entries {n_entries} ≠ {len(entries)}")
    check(n_versions == len(planted), f"entry_versions {n_versions} ≠ {len(planted)} (INV-5 보존)")

    # 밀려난 본이 원문 그대로 남았나
    archived = dict(
        db.execute("SELECT term_key, prompt_version FROM entry_versions")
    )
    check(
        all(v == "v2-pathA:956ba44a7c48" for v in archived.values()),
        f"보존된 본의 태그가 원본이 아니다: {archived}",
    )

    # 재실행 멱등 — 여기가 깨지면 entry_versions가 실행할 때마다 650씩 자란다
    run()
    check(
        db.execute("SELECT COUNT(*) FROM entry_versions").fetchone()[0] == n_versions,
        "재실행이 entry_versions를 늘렸다 — 멱등이 깨졌다",
    )
    check(db.execute("SELECT COUNT(*) FROM entries").fetchone()[0] == n_entries, "재실행이 entries를 늘렸다")
    check(db.execute("SELECT COUNT(*) FROM aliases").fetchone()[0] == n_alias, "재실행이 aliases를 늘렸다")

    # 번들이 바뀌면(센티널이 달라지면) 반영된다 — 센티널의 존재 이유
    edited = json.loads(json.dumps(entries))
    edited[0]["summary"] += " (편집)"
    with redirect_stderr(io.StringIO()):
        sql2, _ = build(edited, BUNDLE_CREATED_AT)
    db.executescript("\n".join(l for l in sql2 if not l.startswith("--")))
    new_tag = bundle_prompt_version(edited)
    check(
        db.execute(
            "SELECT COUNT(*) FROM entries WHERE origin='authored' AND prompt_version=?",
            (new_tag,),
        ).fetchone()[0]
        == len(entries),
        "번들을 고쳤는데 재시딩이 반영되지 않았다 — 선택적 무효화가 죽었다",
    )
    print(
        f"  엔진 실행 ✓ entries {n_entries} · entry_versions {n_versions} ·"
        f" 멱등 ✓ · 스냅샷 교체 ✓"
    )


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
        all("ON CONFLICT(term_key) DO UPDATE SET" in sql[i] for i in entry_stmts),
        "entries INSERT가 DO UPDATE가 아니다 — 검수 안 된 generated가 정본 자리를 지킨다 (§3-5)",
    )
    # 갱신 조건 = authored가 generated를 이긴다 + 번들 스냅샷이 바뀌었다. 둘뿐이어야 한다 —
    # 조건이 빠지면 authored끼리 매번 덮어써 재실행 멱등이 깨지고 entry_versions가 무한히 자란다.
    check(
        all(
            "WHERE entries.origin = 'generated' OR entries.prompt_version <>" in sql[i]
            for i in entry_stmts
        ),
        "DO UPDATE 갱신 조건이 어긋났다 — 멱등이 깨진다",
    )
    set_clauses = [sql[i].split("DO UPDATE SET")[-1] for i in entry_stmts]
    check(
        all("DO UPDATE SET" in sql[i] for i in entry_stmts)
        and all("hit_count" not in c for c in set_clauses),
        "DO UPDATE가 hit_count를 덮는다 — 요청 빈도 실측 원자료가 사라진다",
    )

    # INV-5: 교체 대상은 덮이기 전에 entry_versions로 간다. 보존과 갱신의 조건이 같아야 한다.
    archive_stmts = [i for i, l in enumerate(sql) if l.startswith("INSERT INTO entry_versions")]
    check(
        len(archive_stmts) == EXPECTED["entries"],
        f"entry_versions 보존 문장 {len(archive_stmts)} ≠ entries {EXPECTED['entries']}",
    )
    check(
        all(a < e for a, e in zip(archive_stmts, entry_stmts)),
        "보존 문장이 덮어쓰기보다 뒤에 있다 — 보존 없이 덮인다",
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

    test_conflict_rule_on_real_sqlite(entries, sql)

    if failures:
        print(f"FAIL — {len(failures)}건")
        for m in failures:
            print(f"  {m}")
        return 1
    print("PASS — 시딩 회계 (§7 확정값 일치)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

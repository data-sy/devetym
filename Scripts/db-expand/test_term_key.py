#!/usr/bin/env python3
"""
`normalize_term_key` 케이스 테이블 + **세 지점 동치의 실행 오라클** (W0c §3-1).

두 겹으로 지킨다:
  1. 케이스 테이블 — devetym `NormalizeKeywordTest.kt` · devetym-proxy `test/term-key.test.js`와
     같은 케이스를 미러링한다. **하나를 고치면 셋을 다 고쳐야 한다.**
  2. 교차 실행 — 실 번들 650의 keyword·aliases 전량 + 유니코드 경계 문자를 파이썬 구현과
     **JS 구현에 실제로 통과시켜** 키를 바이트 비교한다. 미러링한 표가 서로 어긋나는 것까지 잡는다.
     (Kotlin은 자기 테스트가 같은 표를 들고 있고, 이 파일은 JS↔Python 축을 맡는다.)

실행:
    python3 Scripts/db-expand/test_term_key.py
"""

from __future__ import annotations

import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from term_key import normalize_term_key as f  # noqa: E402

REPO = Path(__file__).resolve().parents[2]
BUNDLE = REPO / "shared/src/commonMain/composeResources/files/terms.json"
PROXY_SRC = Path.home() / "devetym-proxy/src/index.js"

# Kotlin Char.isWhitespace()가 자르는 / 자르지 않는 집합 (NormalizeKeywordTest 정본)
TRIMMED = (
    list(range(0x0009, 0x000E)) + list(range(0x001C, 0x0021)) + [0x00A0, 0x1680]
    + list(range(0x2000, 0x200B)) + [0x2028, 0x2029, 0x202F, 0x205F, 0x3000]
)
NOT_TRIMMED = [0x0085, 0x180E, 0x200B, 0xFEFF]

failures: list[str] = []


def eq(expected: str, actual: str, label: str) -> None:
    if expected != actual:
        failures.append(f"{label}: expected {expected!r}, got {actual!r}")


def pad(cp: int, s: str) -> str:
    return f"{chr(cp)}{s}{chr(cp)}"


def test_case_table() -> None:
    eq("react", f("React"), "대문자입력_소문자키")
    eq("rest", f("REST"), "대문자입력_소문자키")
    eq("mutex", f("  mutex  "), "공백패딩_트림")
    eq("mutex", f("\t\n mutex \r\n"), "공백패딩_트림")
    eq("mutex", f(pad(0x00A0, "Mutex")), "NBSP패딩_트림")
    eq(pad(0xFEFF, "mutex"), f(pad(0xFEFF, "Mutex")), "BOM패딩_트림하지않음")
    eq("mutex", f(pad(0x001F, "Mutex")), "U001F패딩_트림")
    eq("뮤텍스", f(pad(0x3000, "뮤텍스")), "U3000패딩_트림")
    for cp in TRIMMED:
        eq("go", f(pad(cp, "Go")), f"U+{cp:04X} 미트림")
    for cp in NOT_TRIMMED:
        eq(pad(cp, "go"), f(pad(cp, "Go")), f"U+{cp:04X} 과트림")

    # ── W0c §3-1: 구분자는 트림이 아니라 삭제 ─────────────────────────────
    eq("mutualexclusion", f("  Mutual Exclusion  "), "내부공백_삭제")
    eq("aatree", f("aa-tree"), "하이픈_삭제")
    eq("bplustree", f("b_plus_tree"), "언더스코어_삭제")
    for v in ["aa-tree", "AA tree", "AA-Tree", "aatree", "aa_tree", "  aa  tree  "]:
        eq("aatree", f(v), f"표기변이_동일키 {v!r}")
    eq("추상팩토리", f("추상 팩토리"), "한글별칭_공백무관")
    eq("추상팩토리", f("추상팩토리"), "한글별칭_공백무관")
    eq("추상팩토리", f(pad(0x3000, "추상 팩토리")), "한글별칭_전각공백")
    for cp in TRIMMED:
        eq("golang", f(f"Go{chr(cp)}Lang"), f"U+{cp:04X} 내부 미삭제")
    for cp in NOT_TRIMMED:
        eq(f"go{chr(cp)}lang", f(f"Go{chr(cp)}Lang"), f"U+{cp:04X} 내부 과삭제")
    eq("", f(""), "빈문자열")
    eq("", f("   "), "공백만")


def test_cross_impl_js() -> None:
    """실 번들 전량 + 경계 문자를 JS 구현에 통과시켜 파이썬 결과와 바이트 비교."""
    if not PROXY_SRC.exists():
        print(f"  SKIP 교차 실행 — {PROXY_SRC} 없음")
        return
    if shutil.which("node") is None:
        print("  SKIP 교차 실행 — node 없음")
        return

    inputs: list[str] = []
    if BUNDLE.exists():
        for e in json.loads(BUNDLE.read_text(encoding="utf-8")):
            inputs.append(e["keyword"])
            inputs.extend(e.get("aliases", []))
            # 사용자가 칠 법한 표기 변이도 같이 태운다
            inputs.append(e["keyword"].replace("-", " ").replace("_", " "))
            inputs.append(e["keyword"].replace("-", "").replace("_", ""))
    for cp in TRIMMED + NOT_TRIMMED:
        inputs.append(pad(cp, "Go"))
        inputs.append(f"Go{chr(cp)}Lang")

    with tempfile.TemporaryDirectory() as d:
        inp = Path(d) / "in.json"
        script = Path(d) / "run.mjs"
        inp.write_text(json.dumps(inputs), encoding="utf-8")
        script.write_text(
            f'import {{ readFileSync }} from "node:fs";\n'
            f'import {{ normalizeTermKey }} from "{PROXY_SRC}";\n'
            f'const xs = JSON.parse(readFileSync("{inp}", "utf8"));\n'
            f"process.stdout.write(JSON.stringify(xs.map(normalizeTermKey)));\n",
            encoding="utf-8",
        )
        proc = subprocess.run(
            ["node", str(script)], capture_output=True, text=True, check=False
        )
        if proc.returncode != 0:
            failures.append(f"JS 실행 실패: {proc.stderr.strip()[:400]}")
            return
        js_keys = json.loads(proc.stdout)

    mismatches = [
        (x, p, j)
        for x, j in zip(inputs, js_keys)
        if (p := f(x)) != j
    ]
    for x, p, j in mismatches[:10]:
        failures.append(f"JS↔Python 불일치 {x!r}: py={p!r} js={j!r}")
    if len(mismatches) > 10:
        failures.append(f"... 외 {len(mismatches) - 10}건 불일치")
    print(f"  교차 실행 {len(inputs)}건 · 불일치 {len(mismatches)}건")


def main() -> int:
    test_case_table()
    test_cross_impl_js()
    if failures:
        print(f"FAIL — {len(failures)}건")
        for msg in failures:
            print(f"  {msg}")
        return 1
    print("PASS — term_key 케이스 테이블 + JS 교차 실행")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""
term_key 정규화 — **파이프라인 측 정본** (W0c §3-1, 2026-08-26 확정).

세 지점이 같은 입력에 같은 키를 내야 한다:

    1. 앱      shared/src/commonMain/kotlin/com/robin/devetym/data/AppJson.kt  normalizeKeyword
    2. Worker  ~/devetym-proxy/src/index.js                                    normalizeTermKey
    3. 파이프라인  이 파일                                                       normalize_term_key

정의: 구분자 — 공백류 ∪ {하이픈, 언더스코어} — 를 **양끝만이 아니라 내부까지 전부 삭제**한 뒤
lowercase. `"aa-tree"` · `"AA tree"` · `"aatree"` → `"aatree"`.

⚠️ **`str.isspace()`를 쓰면 안 된다.** Kotlin `Char.isWhitespace()`와 집합이 다르다 —
   가장 큰 차이는 U+0085(NEL)로, 파이썬은 공백으로 보지만 Kotlin은 **자르지 않는다.**
   아래 WS는 devetym `NormalizeKeywordTest`가 JVM·iosSimulatorArm64에서 U+0000~U+FFFF를
   전수 실측해 고정한 집합이며, 그 테스트가 정본이다.

⚠️ 갈라져도 조용하다 — 각 지점은 자기 일관적이라 미스가 나지 않는다. 증상은 INV-12 승격 잡이
   흘린 term_key를 클라가 영영 조회 못 하는 누수다. `test_term_key.py`가 이쪽 오라클이다.
"""

from __future__ import annotations

# Kotlin `Char.isWhitespace()`가 자르는 집합 전체. JS 측 `WS` 상수와 같아야 한다.
_WHITESPACE: frozenset[str] = frozenset(
    [chr(cp) for cp in range(0x0009, 0x000D + 1)]
    + [chr(cp) for cp in range(0x001C, 0x0020 + 1)]
    + [chr(0x00A0), chr(0x1680)]
    + [chr(cp) for cp in range(0x2000, 0x200A + 1)]
    + [chr(0x2028), chr(0x2029), chr(0x202F), chr(0x205F), chr(0x3000)]
)

# 삭제 대상 = 공백류 ∪ {하이픈, 언더스코어}
_SEPARATORS: frozenset[str] = _WHITESPACE | frozenset(["-", "_"])


def normalize_term_key(s: str) -> str:
    """구분자를 전부 삭제한 뒤 lowercase. 세 지점 공통 정의."""
    return "".join(c for c in s if c not in _SEPARATORS).lower()

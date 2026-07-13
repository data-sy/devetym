#!/usr/bin/env python3
"""
DB 번들 확장 — Deterministic validator.

신규 batch entry(round-NNN.json) 전용. 머지 산출물 전체엔 적용하지 않음
(legacy 500은 grandfather — docs/db-expand/spec.md 확정 결정 참조).

Scripts/generate_db.py:162-189의 validate()를 fork:
  - 추가: 길이 검증 (SUMMARY_LEN / ETYMOLOGY_LEN / NAMING_LEN)
  - 제거: MIN_TOTAL 검사 (per-batch validator에 부적합)

사용법:
    python validator.py <input.json>
    stdout에 {"passed": [keyword, ...], "failed": [{keyword, rule_id, reason}, ...]} JSON 출력
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

HANGUL_RE = re.compile(r"[가-힣]")
KEYWORD_RE = re.compile(r"^[a-z0-9][a-z0-9_-]*$")
ALLOWED_CATEGORIES = {"동시성", "자료구조", "네트워크", "DB", "패턴", "기타"}
REQUIRED_FIELDS = ("keyword", "aliases", "category", "summary", "etymology", "namingReason")

LEN_RULES = {
    "summary": ("SUMMARY_LEN", 20, 30),
    "etymology": ("ETYMOLOGY_LEN", 60, 120),
    "namingReason": ("NAMING_LEN", 150, 270),
}


def _add(failed: list, keyword: str, rule_id: str, reason: str) -> None:
    failed.append({"keyword": keyword, "rule_id": rule_id, "reason": reason})


def check_null_guard(entry: dict, failed: list) -> None:
    kw = entry.get("keyword", "?")
    for f in REQUIRED_FIELDS:
        v = entry.get(f)
        if v is None:
            _add(failed, kw, "NULL_GUARD", f"필드 '{f}' 누락 또는 null")
        elif isinstance(v, str) and not v.strip():
            _add(failed, kw, "NULL_GUARD", f"필드 '{f}' 빈 문자열")


def check_keyword_format(entry: dict, failed: list) -> None:
    kw = entry.get("keyword", "")
    if not isinstance(kw, str) or not KEYWORD_RE.match(kw):
        _add(failed, kw or "?", "KEYWORD_FORMAT",
             f"keyword '{kw}' 형식 위반 (영문 소문자/숫자/하이픈/언더스코어만, 첫 글자는 영숫자)")


def check_category_enum(entry: dict, failed: list) -> None:
    kw = entry.get("keyword", "?")
    cat = entry.get("category")
    if cat not in ALLOWED_CATEGORIES:
        _add(failed, kw, "CATEGORY_ENUM",
             f"category '{cat}' — 허용값 {sorted(ALLOWED_CATEGORIES)} 외")


def check_lengths(entry: dict, failed: list) -> None:
    kw = entry.get("keyword", "?")
    for field, (rule_id, lo, hi) in LEN_RULES.items():
        v = entry.get(field)
        if not isinstance(v, str):
            continue  # null_guard에서 잡힘
        n = len(v)
        if n < lo or n > hi:
            _add(failed, kw, rule_id, f"{field} {n}자 — {lo}~{hi}자 범위 위반")


def check_aliases(entry: dict, failed: list) -> None:
    kw = entry.get("keyword", "?")
    aliases = entry.get("aliases")
    if not isinstance(aliases, list) or len(aliases) < 1:
        _add(failed, kw, "ALIAS_MIN1", "aliases는 최소 1개 필요")
        return
    if not any(isinstance(a, str) and HANGUL_RE.search(a) for a in aliases):
        _add(failed, kw, "HANGUL_ALIAS_MIN1", "한글 표기 alias 최소 1개 필요")


def check_keyword_unique(entries: list, failed: list) -> None:
    counts: dict[str, int] = {}
    for e in entries:
        kw = e.get("keyword", "")
        if isinstance(kw, str):
            counts[kw] = counts.get(kw, 0) + 1
    for kw, n in counts.items():
        if n > 1:
            _add(failed, kw, "KEYWORD_UNIQUE", f"keyword '{kw}' batch 내 {n}회 중복")


def validate(entries: list[dict[str, Any]]) -> dict[str, Any]:
    failed: list[dict[str, Any]] = []
    for entry in entries:
        check_null_guard(entry, failed)
        check_keyword_format(entry, failed)
        check_category_enum(entry, failed)
        check_lengths(entry, failed)
        check_aliases(entry, failed)
    check_keyword_unique(entries, failed)

    failed_keywords = {f["keyword"] for f in failed}
    passed = [
        e.get("keyword", "?") for e in entries
        if e.get("keyword") not in failed_keywords
    ]
    return {"passed": passed, "failed": failed}


def main() -> int:
    parser = argparse.ArgumentParser(description="DB 번들 확장 batch validator (신규 entry 전용)")
    parser.add_argument("input", type=Path, help="신규 batch JSON 파일")
    args = parser.parse_args()

    entries = json.loads(args.input.read_text(encoding="utf-8"))
    if not isinstance(entries, list):
        print(json.dumps({"error": "입력은 JSON 배열이어야 합니다"}, ensure_ascii=False))
        return 1

    result = validate(entries)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())

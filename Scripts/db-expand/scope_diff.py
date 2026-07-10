#!/usr/bin/env python3
"""
DB 번들 확장 — scope_diff: 재생성이 대상 외 entry를 건드렸는지 검출.

새 흐름(Phase 5): Generator → validator → critic(v2) → 재생성 → **scope_diff** → 머지.
재생성은 validator/critic이 failed로 지목한 keyword만 고쳐야 한다. 그 외 entry가
바뀌었으면(scope_leak) 또는 고쳐야 할 entry가 안 바뀌었으면(missing_change) 신호다.
round-001은 사람이 눈으로 확인했으나(10개), Phase 6(30~50)부터는 이 도구로 대체한다.

비교 규칙:
  - 비교 대상 필드: keyword, category, summary, etymology, namingReason, aliases
  - aliases는 list라 순서 노이즈가 있으므로 **정렬 후** 비교 (순서만 다른 건 변경으로 보지 않음)
  - keyword를 동일성 키로 사용. before/after 간 keyword가 추가/삭제되면 added/removed로 분리 보고

사용법:
    python scope_diff.py <before.json> <after.json> <failed_keywords>

    failed_keywords 인자는 다음 중 하나:
      - JSON 파일 경로: ["kw1", "kw2"] 형태 배열, 또는
        validator/critic 출력 {"failed": [{"keyword": ...}, ...]} 형태
      - 쉼표로 구분한 keyword 문자열 (예: "priority-inversion,sni")

    stdout에 JSON 출력:
      {
        "expected_changed": [...],   # 고쳐야 했던 keyword (= failed_keywords)
        "actual_changed":   [...],   # 실제로 내용이 바뀐 keyword
        "scope_leak":       [...],   # 안 건드려야 하는데 바뀐 것 (actual - expected)
        "missing_change":   [...],   # 고쳐야 하는데 안 바뀐 것 (expected - actual)
        "added":            [...],   # after에만 있는 keyword (재생성이 새로 만든 것)
        "removed":          [...],   # before에만 있는 keyword (재생성이 떨군 것)
        "clean": bool                # scope_leak·missing_change·added·removed 전부 비면 True
      }

종료 코드: clean이면 0, 아니면 2 (CI/루프에서 게이트로 사용 가능).
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

# 동일성 비교에 쓰는 스키마 필드. aliases는 list라 별도 정규화한다.
COMPARE_FIELDS = ("keyword", "category", "summary", "etymology", "namingReason")


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def index_by_keyword(entries: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    if not isinstance(entries, list):
        sys.exit("before/after 입력은 JSON 배열이어야 합니다")
    index: dict[str, dict[str, Any]] = {}
    for e in entries:
        kw = e.get("keyword")
        if not isinstance(kw, str):
            sys.exit(f"keyword 누락/비문자열 entry 발견: {e!r}")
        if kw in index:
            sys.exit(f"입력 내 keyword 중복: {kw!r}")
        index[kw] = e
    return index


def normalize(entry: dict[str, Any]) -> tuple:
    """순서 노이즈를 제거한 비교용 정규형. aliases는 정렬해 순서 차이를 무시."""
    scalar = tuple(entry.get(f) for f in COMPARE_FIELDS)
    aliases = entry.get("aliases")
    if isinstance(aliases, list):
        aliases_norm: tuple = tuple(sorted(str(a) for a in aliases))
    else:
        aliases_norm = ("<non-list>", repr(aliases))
    return scalar + (aliases_norm,)


def parse_failed_keywords(arg: str) -> list[str]:
    """파일 경로면 JSON으로, 아니면 쉼표 구분 문자열로 해석. 빈 입력은 빈 목록."""
    if not arg.strip():
        return []
    path = Path(arg)
    if path.is_file():
        data = load_json(path)
        if isinstance(data, dict) and "failed" in data:
            # validator/critic 출력 형태 {"failed": [{"keyword": ...}, ...]}
            kws = [f.get("keyword") for f in data["failed"] if isinstance(f, dict)]
            return sorted({k for k in kws if isinstance(k, str)})
        if isinstance(data, list):
            return sorted({str(k) for k in data})
        sys.exit("failed_keywords 파일은 배열 또는 {'failed': [...]} 형태여야 합니다")
    # 파일이 아니면 쉼표 구분 문자열
    return sorted({s.strip() for s in arg.split(",") if s.strip()})


def scope_diff(
    before: list[dict[str, Any]],
    after: list[dict[str, Any]],
    failed_keywords: list[str],
) -> dict[str, Any]:
    bi = index_by_keyword(before)
    ai = index_by_keyword(after)

    before_keys = set(bi)
    after_keys = set(ai)

    added = sorted(after_keys - before_keys)
    removed = sorted(before_keys - after_keys)

    # 양쪽에 다 있는 keyword 중 내용이 바뀐 것
    common = before_keys & after_keys
    actual_changed = sorted(
        kw for kw in common if normalize(bi[kw]) != normalize(ai[kw])
    )

    expected = sorted(set(failed_keywords))
    expected_set = set(expected)
    changed_set = set(actual_changed)

    scope_leak = sorted(changed_set - expected_set)
    missing_change = sorted(expected_set - changed_set)

    clean = not (scope_leak or missing_change or added or removed)

    return {
        "expected_changed": expected,
        "actual_changed": actual_changed,
        "scope_leak": scope_leak,
        "missing_change": missing_change,
        "added": added,
        "removed": removed,
        "clean": clean,
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="재생성 scope leak 검출 (before/after/failed_keywords 비교)"
    )
    parser.add_argument("before", type=Path, help="재생성 전 batch JSON")
    parser.add_argument("after", type=Path, help="재생성 후 batch JSON")
    parser.add_argument(
        "failed_keywords",
        help="고쳐야 했던 keyword: JSON 파일 경로(배열 또는 {failed:[...]}) 또는 쉼표 구분 문자열",
    )
    args = parser.parse_args()

    before = load_json(args.before)
    after = load_json(args.after)
    failed = parse_failed_keywords(args.failed_keywords)

    result = scope_diff(before, after, failed)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result["clean"] else 2


if __name__ == "__main__":
    sys.exit(main())

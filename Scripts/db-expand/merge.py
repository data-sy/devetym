#!/usr/bin/env python3
"""
DB 번들 확장 — terms.json 머지 helper.

Scripts/generate_db.py의 load_existing() + 점진적 저장 로직을 fork.

사용법:
    python merge.py <existing.json> <batch.json> <output.json>

동작:
    1) existing 로드 (shared/src/commonMain/composeResources/files/terms.json)
    2) batch와 keyword 충돌 검사 (Phase 0-2 dedup 안전망 — 충돌 시 종료)
    3) merged = existing + batch, keyword.lower() 알파벳 정렬
    4) output(terms.next.json)에 저장
    5) swap은 사용자가 직접 (smoke test 통과 후)
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


def load_json_array(path: Path) -> list[dict[str, Any]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, list):
        sys.exit(f"{path}는 JSON 배열이어야 합니다")
    return data


def main() -> int:
    parser = argparse.ArgumentParser(description="terms.json + 신규 batch 머지")
    parser.add_argument("existing", type=Path, help="기존 terms.json")
    parser.add_argument("batch", type=Path, help="신규 batch (validator 통과 산출물)")
    parser.add_argument("output", type=Path, help="머지 결과 (예: terms.next.json)")
    args = parser.parse_args()

    existing = load_json_array(args.existing)
    batch = load_json_array(args.batch)

    existing_keys = {e["keyword"] for e in existing}
    conflicts = [b["keyword"] for b in batch if b.get("keyword") in existing_keys]
    if conflicts:
        sys.exit(f"keyword 충돌 — Phase 0-2 dedup 점검 필요: {conflicts}")

    merged = existing + batch
    merged.sort(key=lambda e: e["keyword"].lower())

    args.output.write_text(
        json.dumps(merged, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"머지 완료: {len(existing)} + {len(batch)} = {len(merged)} → {args.output}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

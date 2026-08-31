#!/usr/bin/env python3
"""
authored 센티널·payload 오라클 (W0c §3-3).

지키는 것 셋:
  1. 태그가 **내용에만** 반응한다 — 포맷을 바꿔도 안 변하고, 한 글자만 고쳐도 변한다.
  2. authored payload가 generated payload와 **같은 모양**이다(버전 2필드 포함).
  3. 실 번들 650 전수가 Worker shape 게이트·정본 카테고리 집합을 통과한다.
     (여기서 걸러야 §3-4 시딩이 D1에 못 들어가는 행을 들고 가지 않는다)

실행:
    python3 Scripts/db-expand/test_authored_version.py
"""

from __future__ import annotations

import copy
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from authored_version import (  # noqa: E402
    AUTHORED_PREFIX,
    GENERATED_PREFIX,
    SCHEMA_VERSION,
    authored_payload,
    bundle_prompt_version,
    validate_authored,
)

REPO = Path(__file__).resolve().parents[2]
BUNDLE = REPO / "shared/src/commonMain/composeResources/files/terms.json"
PROXY_SRC = Path.home() / "devetym-proxy/src/index.js"

failures: list[str] = []


def check(cond: bool, msg: str) -> None:
    if not cond:
        failures.append(msg)


def test_tag_shape() -> None:
    entries = json.loads(BUNDLE.read_text(encoding="utf-8"))
    tag = bundle_prompt_version(entries)
    check(
        re.fullmatch(r"authored:[0-9a-f]{12}", tag) is not None,
        f"태그 형식이 어긋남: {tag!r}",
    )
    # 두 갈래가 접두로 갈린다 — 한쪽이 다른 쪽의 접두면 LIKE 분리 조회가 무너진다
    check(
        not AUTHORED_PREFIX.startswith(GENERATED_PREFIX)
        and not GENERATED_PREFIX.startswith(AUTHORED_PREFIX),
        "authored/generated 접두가 겹친다",
    )
    print(f"  태그 = {tag}")


def test_tag_is_content_only() -> None:
    entries = json.loads(BUNDLE.read_text(encoding="utf-8"))
    base = bundle_prompt_version(entries)

    # 포맷만 바꾼 사본 — 들여쓰기·키 순서·ASCII 이스케이프를 전부 뒤집는다
    reformatted = json.loads(
        json.dumps(entries, indent=4, ensure_ascii=True, sort_keys=True)
    )
    check(
        bundle_prompt_version(reformatted) == base,
        "포맷만 바꿨는데 태그가 변했다 — 650행이 통째로 오탐된다",
    )

    # 내용 한 글자 — 반드시 변해야 한다
    edited = copy.deepcopy(entries)
    edited[0]["summary"] = edited[0]["summary"] + "."
    check(
        bundle_prompt_version(edited) != base,
        "내용을 고쳤는데 태그가 그대로다 — 선택적 무효화가 죽는다",
    )

    # 순서만 바뀐 번들은 **다른** 스냅샷으로 본다(D1 행 집합은 같지만 파일은 다르다).
    # 이걸 같게 만들려면 엔트리별 해시로 가야 하는데, 그러면 "스냅샷 X 전체 무효화"를 잃는다.
    shuffled = list(reversed(entries))
    check(
        bundle_prompt_version(shuffled) != base,
        "순서 변화가 태그에 안 잡힌다(설계상 잡혀야 함)",
    )
    print("  포맷 무관 ✓ · 내용 변화 감지 ✓")


def test_payload_shape_matches_generated() -> None:
    entries = json.loads(BUNDLE.read_text(encoding="utf-8"))
    tag = bundle_prompt_version(entries)
    p = authored_payload(entries[0], tag)

    check(p["schemaVersion"] == SCHEMA_VERSION, "schemaVersion 불일치")
    check(p["promptVersion"] == tag, "promptVersion이 payload에 안 실렸다")
    check(
        set(p) == set(entries[0]) | {"schemaVersion", "promptVersion"},
        f"payload 필드 집합이 번들+버전2필드가 아니다: {sorted(set(p))}",
    )

    # Worker 상수와 실제로 대조 — 미러링한 숫자가 조용히 갈라지는 걸 막는다
    if PROXY_SRC.exists():
        src = PROXY_SRC.read_text(encoding="utf-8")
        m = re.search(r"const SCHEMA_VERSION = (\d+);", src)
        check(m is not None, "Worker SCHEMA_VERSION 상수를 못 찾음")
        if m:
            check(
                int(m.group(1)) == SCHEMA_VERSION,
                f"Worker SCHEMA_VERSION={m.group(1)} ≠ 파이프라인 {SCHEMA_VERSION}",
            )
        m2 = re.search(r'const PROMPT_VERSION_PREFIX = "([^"]+)"', src)
        if m2:
            check(
                m2.group(1) == GENERATED_PREFIX,
                f"Worker 접두 {m2.group(1)!r} ≠ 파이프라인 {GENERATED_PREFIX!r}",
            )
    else:
        print("  ⚠️ 프록시 소스 없음 — Worker 상수 대조 건너뜀")
    print("  payload 8필드 · Worker 상수 대조 ✓")


def test_bundle_650_all_valid() -> None:
    entries = json.loads(BUNDLE.read_text(encoding="utf-8"))
    bad = [(e.get("keyword"), validate_authored(e)) for e in entries]
    bad = [(k, p) for k, p in bad if p]
    for k, p in bad[:10]:
        failures.append(f"번들 엔트리 부적합 {k!r}: {'; '.join(p)}")
    if len(bad) > 10:
        failures.append(f"... 외 {len(bad) - 10}건")
    print(f"  번들 {len(entries)}건 · 부적합 {len(bad)}건")


def main() -> int:
    test_tag_shape()
    test_tag_is_content_only()
    test_payload_shape_matches_generated()
    test_bundle_650_all_valid()
    if failures:
        print(f"FAIL — {len(failures)}건")
        for msg in failures:
            print(f"  {msg}")
        return 1
    print("PASS — authored 센티널 · payload 대칭 · 번들 650 전수")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

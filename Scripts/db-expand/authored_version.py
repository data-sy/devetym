#!/usr/bin/env python3
"""
authored 행의 버전 태깅 — **파이프라인 측 정본** (W0c §3-3, 2026-08-26 확정).

`entries.prompt_version`은 `NOT NULL`이고, 그 값은 "이 행이 무엇으로부터 만들어졌나"다.
INV-9의 목적은 원천이 바뀌었을 때 **옛 원천으로 만든 행만 골라 다시 만드는 것**이다.

    generated  원천 = system 프롬프트  →  "v2-pathA:" + sha256(프롬프트)[:12]
    authored   원천 = 번들 terms.json  →  "authored:" + sha256(정규화 JSON)[:12]   ← 이 파일

⚠️ **손으로 매기는 번호(`authored:db-expand-v1`)를 쓰지 않는다.** ADR-0012가 예시로 적어 둔
   형태지만 Decision이 아니라 미해결 비용으로 적힌 것이고, 같은 문제에 대해 Worker 측은
   이미 그 방식을 기각했다(`~/devetym-proxy/src/index.js` `derivePromptVersion` 주석):
   원천을 고치고 번호를 안 올리면 **신·구 산출물이 한 태그로 섞이고, 섞였다는 걸 알아챌
   수단이 없다.** 내용 해시는 잊을 수가 없다.

⚠️ **원본 바이트가 아니라 정규화 JSON을 해싱한다.** 들여쓰기·키 순서·트레일링 개행만 바뀌어도
   태그가 뒤집히면 650행이 통째로 "원천이 바뀐 행"으로 오탐된다. 실측(08-26):
   같은 번들의 원본 바이트 해시 656cd69d7a4c ≠ 정규화 해시 efa8f264dc67.

⚠️ **컬럼만 채우면 반쪽이다.** 캐시 히트는 payload JSON을 앱에 그대로 돌려주고
   (`src/index.js` `synthesizeResponse`), generated payload 안에는 schemaVersion·promptVersion이
   들어 있다. authored payload가 그 둘을 빼면 **같은 용어가 전달 경로에 따라 두 모양**이 된다.
   `authored_payload()`가 이 대칭을 강제한다.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

# Worker `PROMPT_VERSION_PREFIX`("v2-pathA:")와 **접두가 겹치지 않아야** 두 갈래가 갈린다.
AUTHORED_PREFIX = "authored:"
GENERATED_PREFIX = "v2-pathA:"

# Worker `SCHEMA_VERSION`(src/index.js)과 같은 값이어야 한다. payload shape이 동일하므로 1.
SCHEMA_VERSION = 1

# 번들 엔트리가 싣는 필드 — 시딩된 payload는 여기에 버전 2필드를 더한 8필드다.
BUNDLE_FIELDS = ("keyword", "aliases", "category", "summary", "etymology", "namingReason")


def canonical_bytes(entries: list[dict]) -> bytes:
    """포맷 차이를 걷어낸 표현. 키 정렬·구분자 고정·비ASCII 보존."""
    return json.dumps(
        entries, sort_keys=True, ensure_ascii=False, separators=(",", ":")
    ).encode("utf-8")


def bundle_prompt_version(bundle: Path | list[dict]) -> str:
    """번들 스냅샷의 authored 센티널. 같은 내용 → 같은 값, 포맷 무관."""
    entries = (
        json.loads(Path(bundle).read_text(encoding="utf-8"))
        if isinstance(bundle, (str, Path))
        else bundle
    )
    digest = hashlib.sha256(canonical_bytes(entries)).hexdigest()
    return AUTHORED_PREFIX + digest[:12]


def authored_payload(entry: dict, prompt_version: str) -> dict:
    """
    번들 엔트리 → D1 payload. generated payload와 **같은 모양**이 되게 버전 2필드를 싣는다.

    Worker `normalizePayload`의 authored 짝이다. category clamp는 하지 않는다 —
    번들은 검수를 거친 정본이라 clamp가 조용히 값을 바꾸면 그게 사고다.
    대신 `validate_authored`가 집합 밖 값을 **실패로** 만든다.
    """
    payload = {k: entry[k] for k in BUNDLE_FIELDS if k in entry}
    payload["schemaVersion"] = SCHEMA_VERSION
    payload["promptVersion"] = prompt_version
    return payload


# Worker `CANONICAL_CATEGORIES`와 같은 집합.
CANONICAL_CATEGORIES = frozenset(["동시성", "자료구조", "네트워크", "DB", "패턴", "기타"])

# Worker `passesShapeGate(term_entry)`가 요구하는 필드.
REQUIRED_STRINGS = ("keyword", "category", "summary", "etymology", "namingReason")


def validate_authored(entry: dict) -> list[str]:
    """Worker shape 게이트 + 카테고리 정본 집합을 파이프라인 쪽에서 미리 건다."""
    problems = []
    for k in REQUIRED_STRINGS:
        v = entry.get(k)
        if not isinstance(v, str) or not v:
            problems.append(f"{k} 누락/비문자열")
    aliases = entry.get("aliases", [])
    if not isinstance(aliases, list):
        problems.append("aliases 비배열")
    if entry.get("category") not in CANONICAL_CATEGORIES:
        problems.append(f"category 정본6 밖: {entry.get('category')!r}")
    return problems


if __name__ == "__main__":
    repo = Path(__file__).resolve().parents[2]
    bundle = repo / "shared/src/commonMain/composeResources/files/terms.json"
    print(bundle_prompt_version(bundle))

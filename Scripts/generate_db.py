#!/usr/bin/env python3
"""
DevEtym 번들 DB 배치 생성 스크립트.

사용법:
    export ANTHROPIC_API_KEY=sk-ant-...
    python Scripts/generate_db.py \\
        --input shared/src/commonMain/composeResources/files/terms.json \\
        --output shared/src/commonMain/composeResources/files/terms.json \\
        --keywords Scripts/db-expand/keywords-round-004.txt   # keyword 목록(한 줄 1개)

동작:
    1) --keywords 파일에서 영문 소문자 keyword 목록을 읽는다
    2) --input 파일의 기존 용어는 그대로 보존한다 (keyword/aliases 변경 금지)
    3) 기존 목록에 없는 keyword만 Claude API에 배치 요청한다
    4) 응답을 검증한 뒤 --output에 저장한다

검증 규칙 (실패 시 비정상 종료):
    - 모든 용어에 aliases >= 1
    - 각 용어에 한글 표기 alias 최소 1개
    - keyword는 영문 소문자 + 하이픈/언더스코어만 허용
    - 모든 필드 비어있지 않음
    - category는 6개 고정 값 중 하나 ("동시성", "자료구조", "네트워크", "DB", "패턴", "기타")
    - keyword 중복 없음
    - 최종 JSON 유효성 (json.loads 통과)
    - 총 200개 이상
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
from pathlib import Path
from typing import Any

try:
    import urllib.request
    import urllib.error
except ImportError:  # pragma: no cover
    sys.exit("urllib is required")

CLAUDE_MODEL = "claude-sonnet-4-6"
API_URL = "https://api.anthropic.com/v1/messages"
API_VERSION = "2023-06-01"

SYSTEM_PROMPT = """당신은 개발 용어의 어원과 작명 이유를 한국어로 설명하는 사전 데이터 제공자입니다.

[독자와 목표]
- 독자는 한국 개발자이며, 라틴어·그리스어 등 어원 배경지식이 없다고 가정합니다.
- 어원을 나열하는 것이 아니라, "그 어원이 왜 이 개발 개념의 이름이 되었는가"를 납득시키는 것이 목표입니다.

[응답 형식 — 매우 중요]
- 응답은 반드시 JSON 배열로 시작하고 끝납니다: `[ {...}, {...}, ... ]`
- 배열 각 요소는 아래 필드를 가진 객체입니다: keyword, aliases, category, summary, etymology, namingReason
- 마크다운(```), 부연 설명, 그 외 어떤 텍스트도 포함하지 마세요.
- keyword는 입력과 동일한 영문 소문자 표기를 그대로 사용하세요.

[필드별 작성 기준]
- aliases: 한글 표기 최소 1개 필수. 풀네임이 있으면 함께 포함.
- category: 아래 6개 중 하나만 사용: "동시성", "자료구조", "네트워크", "DB", "패턴", "기타"
- summary: 20~30자. 한 줄 요약. "무엇을 하는/무엇인" 수준.
- etymology: 60~120자. 원어(언어·원형)와 그 뜻, 구성 요소(어근·접두사)를 서술.
- namingReason: 150~300자. 반드시 "어원상의 의미 → 개발 현장에서의 실제 쓰임"으로 다리를 놓을 것. 최초 등장 시점·명명자 등 역사적 맥락이 있으면 함께 기술.
- 톤: 건조하고 정확하게. "~이다", "~을 뜻한다" 같은 서술형. 감탄사·과장된 형용사·수식어 남발 금지.

[정확성 원칙]
- 어원이 불확실한 경우 etymology 서두를 "정확한 어원은 불분명하나"로 시작해 알려진 설만 서술하세요.
- 추측이나 민간어원(folk etymology)을 사실처럼 단정하지 마세요.
- 약어(acronym)의 경우 반드시 각 글자가 무엇의 약자인지 풀어서 명시하세요.

[카테고리 규칙]
- 6개 분류에 애매하게 걸치는 경우 가장 핵심적인 분류를 선택하세요.
- 어느 분류에도 명확히 속하지 않으면 "기타"를 사용하세요.

[모범 답안 — 배치 형식 예시]
입력: ["mutex", "jpa", "daemon"]
응답:
[
  {"keyword":"mutex","aliases":["뮤텍스","mutual exclusion"],"category":"동시성","summary":"여러 스레드의 동시 접근을 막는 잠금 장치","etymology":"라틴어 mutuus(상호의)와 exclusio(배제)를 합친 영어 'mutual exclusion'의 축약어. 서로 다른 주체가 서로를 배제하는 상태를 뜻한다.","namingReason":"한 스레드가 공유 자원을 사용하는 동안 다른 스레드의 접근을 '상호 배제'하여 경쟁 조건(race condition)을 막는 동기화 기본형이다. 어원의 '서로를 배제한다'는 의미가 동시성 제어 메커니즘에 그대로 옮겨졌다. 한 번에 오직 하나의 소유자만 락을 쥘 수 있다는 설계 원칙이 여기서 나왔다."},
  {"keyword":"jpa","aliases":["Java Persistence API","자바 영속성 API"],"category":"DB","summary":"자바 객체를 DB에 매핑하는 영속성 표준 명세","etymology":"Java Persistence API의 약어. Java(자바 언어), Persistence(영속성, 프로그램 종료 후에도 데이터가 유지되는 성질), API(응용 프로그래밍 인터페이스)로 구성된 순수 두문자어.","namingReason":"Persistence(영속성)는 메모리상의 객체를 디스크에 '지속'시킨다는 의미로, 객체 지향 언어와 관계형 DB 사이의 매핑 규약을 지칭한다. Java EE 시절 ORM 표준으로 제정되어 Hibernate·EclipseLink 등이 이 명세를 구현한다. 'Persistence'라는 단어 선택 자체가 ORM의 본질인 '객체 생존 기간의 연장'을 드러낸다."},
  {"keyword":"daemon","aliases":["데몬","demon"],"category":"기타","summary":"백그라운드에서 지속 실행되는 프로세스","etymology":"그리스어 δαίμων(daimōn)에서 유래. 본래 '신과 인간 사이의 중개 영혼'을 뜻하는 종교·철학 용어로, 사람 눈에 보이지 않으면서 일을 대신 처리하는 존재를 가리켰다.","namingReason":"1963년 MIT의 Project MAC에서 Maxwell의 악마(Maxwell's demon) 사고실험에 영감을 받아 명명되었다. 사용자 상호작용 없이 시스템 뒤편에서 스스로 작업을 처리하는 프로세스를 '보이지 않는 중개자'라는 원의미에 빗댄 은유적 전이다. Unix 관습에 따라 프로세스 이름 끝에 'd'를 붙인다(httpd, sshd)."}
]
"""

HANGUL_RE = re.compile(r"[\uac00-\ud7a3]")
KEYWORD_RE = re.compile(r"^[a-z0-9][a-z0-9_-]*$")
REQUIRED_FIELDS = ("keyword", "aliases", "category", "summary", "etymology", "namingReason")
ALLOWED_CATEGORIES = {"동시성", "자료구조", "네트워크", "DB", "패턴", "기타"}
MIN_TOTAL = 200


def load_existing(path: Path) -> list[dict[str, Any]]:
    if not path.exists():
        return []
    return json.loads(path.read_text(encoding="utf-8"))


def read_keywords(path: Path) -> list[str]:
    lines = path.read_text(encoding="utf-8").splitlines()
    return [ln.strip() for ln in lines if ln.strip() and not ln.strip().startswith("#")]


def chunked(seq: list[str], size: int) -> list[list[str]]:
    return [seq[i:i + size] for i in range(0, len(seq), size)]


def call_claude(api_key: str, batch: list[str]) -> list[dict[str, Any]]:
    user_prompt = (
        "다음 개발 용어들의 어원 데이터를 JSON 배열로 반환하세요. "
        "keyword는 입력과 동일하게 쓰세요.\n\n"
        + "\n".join(f"- {k}" for k in batch)
    )
    body = json.dumps({
        "model": CLAUDE_MODEL,
        "max_tokens": 8192,
        "system": [
            {
                "type": "text",
                "text": SYSTEM_PROMPT,
                "cache_control": {"type": "ephemeral"},
            }
        ],
        "messages": [{"role": "user", "content": user_prompt}],
    }).encode("utf-8")
    req = urllib.request.Request(
        API_URL,
        data=body,
        headers={
            "x-api-key": api_key,
            "anthropic-version": API_VERSION,
            "content-type": "application/json",
        },
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        payload = json.loads(resp.read().decode("utf-8"))
    text = payload["content"][0]["text"].strip()
    text = re.sub(r"^```(?:json)?\s*", "", text)
    text = re.sub(r"\s*```$", "", text)
    return json.loads(text)


def call_claude_with_retry(api_key: str, batch: list[str], retries: int = 1) -> list[dict[str, Any]]:
    """JSONDecodeError에 한해 재시도. 그 외 에러는 즉시 전파."""
    last_error: Exception | None = None
    for attempt in range(retries + 1):
        try:
            return call_claude(api_key, batch)
        except json.JSONDecodeError as e:
            last_error = e
            if attempt < retries:
                print(f"  JSON 파싱 실패 (시도 {attempt + 1}/{retries + 1}), 재시도: {e}")
                time.sleep(2.0)
                continue
            raise
    raise last_error  # unreachable


def validate(terms: list[dict[str, Any]]) -> list[str]:
    errors: list[str] = []
    seen: set[str] = set()
    for i, t in enumerate(terms):
        tag = f"[{i}] {t.get('keyword', '?')}"
        for f in REQUIRED_FIELDS:
            v = t.get(f)
            if v is None or (isinstance(v, str) and not v.strip()):
                errors.append(f"{tag}: '{f}' 비어있음")
            if f == "aliases" and (not isinstance(v, list) or len(v) < 1):
                errors.append(f"{tag}: aliases는 최소 1개 필요")
        kw = t.get("keyword", "")
        if kw in seen:
            errors.append(f"{tag}: keyword 중복")
        seen.add(kw)
        if not KEYWORD_RE.match(kw):
            errors.append(f"{tag}: keyword 형식 오류 (영문 소문자/숫자/하이픈/언더스코어만)")
        aliases = t.get("aliases") or []
        if not any(isinstance(a, str) and HANGUL_RE.search(a) for a in aliases):
            errors.append(f"{tag}: 한글 표기 alias 최소 1개 필요")
        category = t.get("category")
        if category not in ALLOWED_CATEGORIES:
            errors.append(
                f"{tag}: category '{category}' — 허용값 {sorted(ALLOWED_CATEGORIES)} 중 하나여야 함"
            )
    if len(terms) < MIN_TOTAL:
        errors.append(f"총 {len(terms)}개 — 최소 {MIN_TOTAL}개 필요")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--keywords", type=Path,
                        help="생성할 keyword 목록 파일 (없으면 검증만 수행)")
    parser.add_argument("--batch-size", type=int, default=10)
    parser.add_argument("--sleep", type=float, default=1.0,
                        help="배치 간 대기 (초)")
    parser.add_argument("--validate-only", action="store_true")
    args = parser.parse_args()

    existing = load_existing(args.input)
    existing_keys = {t["keyword"] for t in existing}
    merged: list[dict[str, Any]] = list(existing)

    if not args.validate_only and args.keywords:
        api_key = os.environ.get("ANTHROPIC_API_KEY", "").strip()
        if not api_key:
            sys.exit("ANTHROPIC_API_KEY 환경변수가 비어있습니다")
        requested = read_keywords(args.keywords)
        pending = [k for k in requested if k not in existing_keys]
        print(f"기존 {len(existing)}개 + 신규 {len(pending)}개 요청")
        skipped_batches: list[list[str]] = []
        for batch_idx, batch in enumerate(chunked(pending, args.batch_size), 1):
            total_batches = (len(pending) + args.batch_size - 1) // args.batch_size
            print(f"  [{batch_idx}/{total_batches}] 배치 생성: {batch}")
            try:
                generated = call_claude_with_retry(api_key, batch, retries=1)
            except json.JSONDecodeError as e:
                print(f"  ⚠️ JSON 파싱 재시도도 실패 — 배치 스킵: {e}", file=sys.stderr)
                skipped_batches.append(batch)
                continue
            except (urllib.error.URLError, KeyError) as e:
                # 네트워크·응답 구조 에러는 중단 (재시도 의미 없음)
                print(f"API 호출 실패 (중단): {e}", file=sys.stderr)
                break
            for t in generated:
                if t.get("keyword") in existing_keys:
                    continue
                merged.append(t)
                existing_keys.add(t["keyword"])
            # 점진적 저장 — 매 배치 후 파일 쓰기 (중간 실패 대비)
            args.output.write_text(
                json.dumps(merged, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8",
            )
            time.sleep(args.sleep)

        if skipped_batches:
            print("\n⚠️ 스킵된 배치 — 수동 재실행 필요:", file=sys.stderr)
            for b in skipped_batches:
                print(f"  - {b}", file=sys.stderr)

    errors = validate(merged)
    if errors:
        print("검증 실패:", file=sys.stderr)
        for e in errors:
            print(f"  - {e}", file=sys.stderr)
        return 1

    args.output.write_text(
        json.dumps(merged, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"저장 완료: {args.output} ({len(merged)}개)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

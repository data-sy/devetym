"""
Raw API 응답에서 분석용 메트릭을 추출.

각 메트릭의 의미:
- branch_correct: 모델이 호출한 도구가 expected_branch와 일치하는가
- aliases_has_qualifier: aliases에 한정 수식어 prefix 변형이 있는가 (약점 3 검증)
- naming_len: namingReason 자수 (약점 1 검증)
- naming_last_sentence: namingReason의 마지막 문장 (약점 1 사후 분석)
- typo_suggestion_correct: possible_typo의 suggestion이 기대값과 일치하는가
"""

import re
from typing import Optional


# 한정 수식어 prefix 후보 (약점 3 검증용)
# alias가 이 prefix 중 하나로 시작하면 한정 수식어 변형으로 판정.
QUALIFIER_PREFIXES = [
    # 영어
    "HTTP ", "HTTPS ", "TCP ", "UDP ", "Web ", "Java ", "Python ",
    "DB ", "SQL ", "NoSQL ", "RESTful ", "REST ",
    # 한글
    "웹 ", "자바 ", "파이썬 ", "네트워크 ", "데이터베이스 ",
]


def extract_tool_use(content: list) -> Optional[dict]:
    """API 응답 content에서 tool_use 블록 찾기. 여러 개면 첫 번째."""
    for block in content:
        if block.get("type") == "tool_use":
            return block
    return None


def extract_thinking(content: list) -> str:
    """API 응답 content에서 thinking 블록 텍스트 추출.
    redacted_thinking이면 placeholder로 표시. 여러 thinking 블록은 줄바꿈으로 연결.
    """
    parts = []
    for block in content:
        btype = block.get("type")
        if btype == "thinking":
            parts.append(block.get("thinking", ""))
        elif btype == "redacted_thinking":
            parts.append("[REDACTED_THINKING]")
    return "\n".join(parts)


def classify_branch(tool_name: str) -> str:
    """tool 이름을 분기 라벨로 변환."""
    return {
        "return_term_entry": "term_entry",
        "return_not_dev_term": "not_dev_term",
        "return_possible_typo": "possible_typo",
    }.get(tool_name, "unknown")


def split_last_sentence(text: str) -> tuple[str, str]:
    """한국어 텍스트를 (본문 앞부분, 마지막 문장)으로 분리.

    종결 부호('.', '!', '?') 뒤에 공백 또는 문자열 끝이 오는 위치로 분리.
    문장이 한 개뿐이면 ("", 전체 텍스트) 반환.
    """
    text = text.strip()
    if not text:
        return "", ""

    # 종결 부호 + (공백 또는 끝) 패턴의 모든 위치
    positions = [m.end() for m in re.finditer(r'[.!?](?:\s|$)', text)]

    if len(positions) < 2:
        # 종결 부호가 0개 또는 1개 → 문장 1개로 취급
        return "", text

    # 마지막에서 두 번째 종결 위치가 본문/마지막 문장 경계
    boundary = positions[-2]
    body = text[:boundary].strip()
    last = text[boundary:].strip()
    return body, last


def _is_acronym_expansion(alias: str, keyword: str) -> bool:
    """alias가 keyword(약어)의 영어 풀네임인지 판단.

    예: alias='Java Persistence API', keyword='jpa' → True (J·P·A 매칭)
        alias='HTTP cookie', keyword='cookie' → False (단어 첫 글자 H·c ≠ c·o·o·k·i·e)
    """
    words = alias.strip().split()
    keyword_lower = keyword.strip().lower()
    if len(words) != len(keyword_lower) or len(words) < 2:
        return False
    return all(
        w and w[0].isascii() and w[0].isalpha() and w[0].lower() == c
        for w, c in zip(words, keyword_lower)
    )


def aliases_has_qualifier(aliases: list[str], keyword: str) -> bool:
    """aliases 배열에 한정 수식어 prefix 변형이 있는지 검출.

    영어 약어 풀네임은 v1 alias 룰 (2)에 부합하므로 검출 제외.
    예: ['쿠키', 'HTTP cookie', '웹 쿠키'] → True
        ['Java Persistence API', '자바 영속성 API'] → False (전자는 약어 풀네임)
        ['뮤텍스', 'mutual exclusion'] → False
    """
    if not aliases:
        return False
    for alias in aliases:
        if _is_acronym_expansion(alias, keyword):
            continue
        for q in QUALIFIER_PREFIXES:
            if alias.startswith(q):
                return True
    return False


def compute_metrics(response_json: dict, expected: dict) -> dict:
    """API 응답 raw에서 메트릭 dict 추출. CSV 한 행에 해당.

    Args:
        response_json: API response의 dict 형태 (response.model_dump())
        expected: keywords.py의 keyword spec

    Returns:
        모든 메트릭이 채워진 dict (분기에 따라 일부는 0 또는 빈 값)
    """
    content = response_json.get("content", [])
    usage = response_json.get("usage", {})

    tool_block = extract_tool_use(content)
    thinking_text = extract_thinking(content)

    metrics = {
        "keyword": expected["keyword"],
        "group": expected["group"],
        "expected_branch": expected["expected_branch"],
        "actual_branch": "",
        "branch_correct": False,
        # term_entry 한정 필드
        "category": "",
        "aliases": "",
        "aliases_count": 0,
        "aliases_has_qualifier": False,
        "summary_len": 0,
        "etymology_len": 0,
        "naming_len": 0,
        "naming_under_270": False,
        "naming_last_sentence": "",
        "naming_last_sentence_len": 0,
        # possible_typo 한정
        "typo_suggestion": "",
        "typo_suggestion_correct": None,
        # 사용량
        "thinking_chars": len(thinking_text),
        "input_tokens": usage.get("input_tokens", 0),
        "output_tokens": usage.get("output_tokens", 0),
        "cache_read_tokens": usage.get("cache_read_input_tokens", 0),
        "cache_create_tokens": usage.get("cache_creation_input_tokens", 0),
        "stop_reason": response_json.get("stop_reason", ""),
    }

    if tool_block is None:
        metrics["actual_branch"] = "no_tool_use"
        return metrics

    branch = classify_branch(tool_block.get("name", ""))
    metrics["actual_branch"] = branch
    metrics["branch_correct"] = (branch == expected["expected_branch"])

    tool_input = tool_block.get("input", {}) or {}

    if branch == "term_entry":
        aliases = tool_input.get("aliases", []) or []
        metrics["category"] = tool_input.get("category", "")
        metrics["aliases"] = " | ".join(aliases)  # CSV 친화적
        metrics["aliases_count"] = len(aliases)
        metrics["aliases_has_qualifier"] = aliases_has_qualifier(aliases, expected["keyword"])

        summary = tool_input.get("summary", "") or ""
        etymology = tool_input.get("etymology", "") or ""
        naming = tool_input.get("namingReason", "") or ""

        metrics["summary_len"] = len(summary)
        metrics["etymology_len"] = len(etymology)
        metrics["naming_len"] = len(naming)
        metrics["naming_under_270"] = len(naming) <= 270

        _, last_sent = split_last_sentence(naming)
        metrics["naming_last_sentence"] = last_sent
        metrics["naming_last_sentence_len"] = len(last_sent)

    elif branch == "possible_typo":
        suggestion = (tool_input.get("suggestion", "") or "").strip().lower()
        expected_sugg = (expected.get("expected_suggestion", "") or "").strip().lower()
        metrics["typo_suggestion"] = suggestion
        metrics["typo_suggestion_correct"] = (suggestion == expected_sugg)

    return metrics

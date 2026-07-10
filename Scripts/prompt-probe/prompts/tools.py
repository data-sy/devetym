"""
Claude tool 정의 — 세 도구 모두 8 cell 공통.

TermEntry 스키마는 devetym KMP의 shared/.../data/remote/ClaudePrompt.kt·ClaudeDto.kt와 동기화.
"""

ALLOWED_CATEGORIES = ["동시성", "자료구조", "네트워크", "DB", "패턴", "기타"]

TOOLS = [
    {
        "name": "return_term_entry",
        "description": "입력이 개발 용어로 판단될 때 호출합니다. 어원과 작명 이유를 각 필드에 채워 반환합니다.",
        "input_schema": {
            "type": "object",
            "properties": {
                "keyword": {
                    "type": "string",
                    "description": "영문 소문자 표기. 입력이 한글이거나 대문자여도 정규화하여 넣습니다.",
                },
                "aliases": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "한글 표기나 풀네임 등 대체 이름의 배열",
                },
                "category": {
                    "type": "string",
                    "enum": ALLOWED_CATEGORIES,
                },
                "summary": {
                    "type": "string",
                    "description": "20~30자 분량의 한 줄 요약",
                },
                "etymology": {
                    "type": "string",
                    "description": "60~120자 분량. 원어(언어·원형)와 뜻, 구성 요소(어근·접두사)를 서술.",
                },
                "namingReason": {
                    "type": "string",
                    "description": "150~270자 분량. 어원상 의미와 개발 현장에서의 실제 쓰임 사이에 다리를 놓는 설명.",
                },
            },
            "required": ["keyword", "aliases", "category", "summary", "etymology", "namingReason"],
        },
    },
    {
        "name": "return_not_dev_term",
        "description": "입력이 개발 용어가 아닐 때 호출합니다. 입력값은 없습니다.",
        "input_schema": {
            "type": "object",
            "properties": {},
        },
    },
    {
        "name": "return_possible_typo",
        "description": "입력이 개발 용어가 아니지만 개발 용어의 오타로 추정될 때 호출합니다.",
        "input_schema": {
            "type": "object",
            "properties": {
                "suggestion": {
                    "type": "string",
                    "description": "오타를 교정한 올바른 개발 용어",
                },
            },
            "required": ["suggestion"],
        },
    },
]

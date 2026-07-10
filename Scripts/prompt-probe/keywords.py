"""
v2 측정 keyword set — 15개.

세 그룹으로 분리:
- in_shot (4):
    few-shot 예시에 포함된 키워드. §7.3에서 짚었듯 이 그룹은 거의 그대로
    베껴짐. 효과 측정용이 아니라 회귀(regression) 안정성 체크용.

- out_of_shot (6):
    효과 측정 메인 그룹. closing·alias_strict 처방의 직접 타겟 + 다양성 분산.

- branch_check (5):
    비개발어 2 + 오타 3. selfcheck의 분기 정확도 검증용.
    개발 용어가 아니므로 expected_branch가 not_dev_term 또는 possible_typo.

총 호출 수: 8 cell × 15 keyword = 120.
"""

KEYWORDS = [
    # ─────────────────────────────────────────────────
    # in_shot — 회귀 안정성 (4개)
    # ─────────────────────────────────────────────────
    {"keyword": "mutex",      "group": "in_shot",      "expected_branch": "term_entry"},
    {"keyword": "jpa",        "group": "in_shot",      "expected_branch": "term_entry"},
    {"keyword": "daemon",     "group": "in_shot",      "expected_branch": "term_entry"},
    {"keyword": "bug",        "group": "in_shot",      "expected_branch": "term_entry"},

    # ─────────────────────────────────────────────────
    # out_of_shot — 효과 측정 (6개)
    # idempotent/cookie/ping은 §6에서 namingReason 초과 확인됨 → closing 직접 타겟.
    # cookie는 alias_strict 타겟도 겸함.
    # semaphore는 어원 풍부한 신규 closing 타겟.
    # request는 한정 수식어 유혹이 강해 alias_strict 타겟.
    # boolean은 인명 유래라 selfcheck의 명명자 처리 검증에 유용.
    # null은 짧고 단순해 회귀 floor 체크 역할.
    # ─────────────────────────────────────────────────
    {"keyword": "idempotent", "group": "out_of_shot",  "expected_branch": "term_entry"},
    {"keyword": "cookie",     "group": "out_of_shot",  "expected_branch": "term_entry"},
    {"keyword": "semaphore",  "group": "out_of_shot",  "expected_branch": "term_entry"},
    {"keyword": "request",    "group": "out_of_shot",  "expected_branch": "term_entry"},
    {"keyword": "boolean",    "group": "out_of_shot",  "expected_branch": "term_entry"},
    {"keyword": "null",       "group": "out_of_shot",  "expected_branch": "term_entry"},

    # ─────────────────────────────────────────────────
    # branch_check — selfcheck 분기 정확도 (5개)
    # 비개발어 2: apple, lunch
    # 오타 3: mutext→mutex, redus→redis, semafore→semaphore
    # ─────────────────────────────────────────────────
    {"keyword": "apple",      "group": "branch_check", "expected_branch": "not_dev_term"},
    {"keyword": "lunch",      "group": "branch_check", "expected_branch": "not_dev_term"},
    {"keyword": "mutext",     "group": "branch_check", "expected_branch": "possible_typo",
     "expected_suggestion": "mutex"},
    {"keyword": "redus",      "group": "branch_check", "expected_branch": "possible_typo",
     "expected_suggestion": "redis"},
    {"keyword": "semafore",   "group": "branch_check", "expected_branch": "possible_typo",
     "expected_suggestion": "semaphore"},
]

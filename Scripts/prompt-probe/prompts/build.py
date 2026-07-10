"""
시스템 프롬프트 조립 — closing × selfcheck × alias_strict 의 부분집합으로
factorial cell 생성. cell 이름 규칙은 cell_name() 참고.

**v2 ship 이후 baseline 변경**: null_guard 룰이 production system prompt에
항상 포함되므로 build_prompt 안에 hardcode. factorial 차원이 아님. 즉
"baseline" cell도 null_guard를 포함하며, 이는 v2 production 그대로다.

cell 이름과 코드네임의 의미는 prompts/components.py 상단 docstring 참고.
"""

from prompts.components import (
    PERSONA,
    THINKING_BLOCK_SELFCHECK,
    GOAL_AND_TOOL_SECTION,
    NULL_GUARD_EXTRA,
    build_field_criteria,
    CLOSING_EXTRA,
    ALIAS_STRICT_EXTRA,
    ACCURACY_AND_CATEGORY,
    FEW_SHOT_EXAMPLES,
)


def build_prompt(use_closing: bool, use_selfcheck: bool, use_alias_strict: bool) -> str:
    """세 변경의 on/off 조합으로 시스템 프롬프트 한 variant를 조립.

    null_guard는 v2 baseline이라 hardcode로 항상 포함. factorial 차원이 아님.

    Args:
        use_closing: 약점 1 (namingReason 마무리 문장 제약) 포함 여부
        use_selfcheck: 약점 2 (thinking 단계 자기검수) 포함 여부
        use_alias_strict: 약점 3 (aliases 한정 수식어 부정 예시) 포함 여부
    """
    thinking = THINKING_BLOCK_SELFCHECK if use_selfcheck else ""
    alias_extra = ALIAS_STRICT_EXTRA if use_alias_strict else ""
    closing_extra = CLOSING_EXTRA if use_closing else ""

    field_criteria = build_field_criteria(alias_extra, closing_extra)

    return (
        PERSONA
        + thinking
        + GOAL_AND_TOOL_SECTION
        + NULL_GUARD_EXTRA  # v2 ship 이후 baseline. factorial 차원이 아님.
        + field_criteria
        + ACCURACY_AND_CATEGORY
        + FEW_SHOT_EXAMPLES
    )


def cell_name(use_closing: bool, use_selfcheck: bool, use_alias_strict: bool) -> str:
    """코드네임 조합으로 cell 이름 생성. 셋 다 false면 'baseline'.

    이름 순서는 코드네임 알파벳순이 아니라 약점 번호 순서(1·2·3 = closing·selfcheck·alias_strict).
    """
    parts = []
    if use_closing:
        parts.append("closing")
    if use_selfcheck:
        parts.append("selfcheck")
    if use_alias_strict:
        parts.append("alias_strict")
    return "__".join(parts) if parts else "baseline"


# v2 acceptance 측정: alias_strict cell 1개만 활성화.
# null_guard는 build_prompt 안에서 hardcode이므로 이 cell의 실제 내용은
# v2 production prompt와 동일 (alias_strict + null_guard).
# 나머지 7개는 v3 라운드 시작 시 복원해 8 cell factorial로 사용.
CELL_CONFIGS = [
    # (False, False, False),  # baseline — v3에서 복원
    # (True,  False, False),  # closing — v3에서 복원
    # (False, True,  False),  # selfcheck — v3에서 복원
    (False, False, True ),  # alias_strict (v2 acceptance용; null_guard 자동 포함)
    # (True,  True,  False),  # closing__selfcheck — v3에서 복원
    # (True,  False, True ),  # closing__alias_strict — v3에서 복원
    # (False, True,  True ),  # selfcheck__alias_strict — v3에서 복원
    # (True,  True,  True ),  # closing__selfcheck__alias_strict — v3에서 복원
]


# cell 이름 → 시스템 프롬프트 풀텍스트
CELLS: dict[str, str] = {
    cell_name(c, s, a): build_prompt(c, s, a)
    for c, s, a in CELL_CONFIGS
}

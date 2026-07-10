"""
활성화된 cell이 의도대로 조립됐는지 sanity check.

측정 시작 전 항상 실행해 prompt assembly 버그로 측정 무효화되는 사고를 방지.
독립 실행도 가능: `python -m prompts.verify`

활성 cell 수는 build.py의 CELL_CONFIGS 길이에 따라 결정됨 (8 cell factorial
전체일 수도 있고, acceptance 측정처럼 일부만 활성일 수도 있음).
"""

import hashlib
from prompts.build import CELLS


# 각 코드네임이 켜졌을 때 prompt에 반드시 등장해야 하는 시그니처 문구.
# 부분 문자열만 매칭(전문 일치 X) — 추후 표현이 약간 바뀌어도 robust.
MARKERS = {
    "closing": "마지막 문장은 앞에서 다루지 않은 새 정보",
    "selfcheck": "[응답 절차 — thinking 단계에서 처리]",
    "alias_strict": "한정 수식어를 붙인 변형",
}


def verify_cells() -> bool:
    """모든 cell이 의도된 마커 포함/배제 상태인지 검증. 통과하면 True 반환."""
    errors = []

    for name, prompt in CELLS.items():
        active = set() if name == "baseline" else set(name.split("__"))

        for marker_key, marker_text in MARKERS.items():
            should_have = marker_key in active
            actually_has = marker_text in prompt

            if should_have and not actually_has:
                errors.append(
                    f"[{name}] '{marker_key}' 켜져 있는데 마커 누락: {marker_text!r}"
                )
            if not should_have and actually_has:
                errors.append(
                    f"[{name}] '{marker_key}' 꺼져 있는데 마커 발견: {marker_text!r}"
                )

    # 활성화된 cell의 hash가 모두 unique한지 (중복 cell 없는지)
    n_cells = len(CELLS)
    hashes = {hashlib.sha256(p.encode("utf-8")).hexdigest(): name for name, p in CELLS.items()}
    if len(hashes) != n_cells:
        errors.append(f"중복 cell 발견: unique hash {len(hashes)}개 ({n_cells} 기대)")

    if errors:
        print("Sanity check FAILED:")
        for e in errors:
            print(f"  - {e}")
        return False

    print(f"Sanity check passed: {n_cells} cells, all unique, all markers correct.")
    print()
    print(f"{'cell':<45s} {'길이':>5s}  {'hash[:12]':>12s}")
    print("-" * 70)
    for name, prompt in CELLS.items():
        h = hashlib.sha256(prompt.encode("utf-8")).hexdigest()[:12]
        print(f"{name:<45s} {len(prompt):5d}  {h}")
    return True


if __name__ == "__main__":
    import sys
    sys.exit(0 if verify_cells() else 1)

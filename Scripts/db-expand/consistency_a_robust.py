#!/usr/bin/env python3
"""
Phase 2A 교차검증 — 샘플 운빨 배제 (코드만, API 불필요).
consistency_a.py의 측정 함수를 import해 재사용 → "샘플링" 축만 변화시킨다.
  1) 전수 베이스라인: 500개 전부로 gate 지표 재계산 + legacy 비순응률 확정.
  2) 민감도 스윕: per_cat·offset을 바꿔 다수의 다른 샘플을 만들고 gate 판정이 뒤집히는지 확인.
핵심: gate 1(신규 고정)·gate 3(신규 tone=0 바닥)은 sampling 불변. 표적은 gate 2뿐.
사용법: python consistency_a_robust.py <round.json> <terms.json>
"""
from __future__ import annotations
import argparse, json, statistics, sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))   # 같은 폴더의 consistency_a import
import consistency_a as ca                         # 측정 정의 재사용 (수정 안 함)


def sample_offset(terms, per_cat, offset):
    """sample_by_category의 변형 — 균등 간격 시작점을 offset만큼 회전. 결정론적, 샘플만 달라짐."""
    by_cat = {}
    for t in terms:
        by_cat.setdefault(t.get("category", "?"), []).append(t)
    picked = []
    for cat in sorted(by_cat):
        entries = sorted(by_cat[cat], key=lambda e: e.get("keyword", ""))
        n = len(entries)
        if n <= per_cat:
            picked.extend(entries); continue
        idxs = [(round(i * (n - 1) / (per_cat - 1)) + offset) % n for i in range(per_cat)]
        picked.extend(entries[i] for i in sorted(set(idxs)))
    return picked


def gate_verdicts(new, baseline):
    """spec Phase 2 (A) gate 판정을 명시적으로 인코딩 (보고서는 손으로 판정했던 부분)."""
    new_alias = statistics.median(ca.alias_counts(new))
    base_alias = statistics.median(ca.alias_counts(baseline))
    new_tone = ca.tone_freq(new)["per_1000_chars"]
    base_tone = ca.tone_freq(baseline)["per_1000_chars"]
    new_val = ca.validator_pass_rate(new)["rate"]
    return {
        "gate1_validator_100": new_val == 1.0,
        "gate2_alias_median_equal": new_alias == base_alias,
        "gate3_tone_not_increased": new_tone <= base_tone,
        "_detail": {"new_alias": new_alias, "base_alias": base_alias,
                    "new_tone_per_1k": new_tone, "base_tone_per_1k": base_tone,
                    "new_validator_rate": new_val},
    }


def main():
    ap = argparse.ArgumentParser(description="Phase 2A 샘플 운빨 교차검증 (코드만)")
    ap.add_argument("round", type=Path)
    ap.add_argument("terms", type=Path)
    ap.add_argument("--sweep-per-cat", type=int, nargs="+", default=[3, 4, 5, 8, 10])
    ap.add_argument("--sweep-offsets", type=int, nargs="+", default=[0, 1, 2, 3, 5])
    args = ap.parse_args()

    new = ca.load(args.round)
    terms = ca.load(args.terms)

    # 1) 전수 베이스라인 (gate 2의 진짜 기준 + 관찰사항 확정)
    full = gate_verdicts(new, terms)
    full["_detail"]["legacy_validator_full"] = ca.validator_pass_rate(terms)
    full["_detail"]["legacy_alias_dist"] = dict(Counter(ca.alias_counts(terms)))
    full["_detail"]["legacy_tone"] = ca.tone_freq(terms)

    # 2) 샘플링 스윕 — 다른 샘플들에서 판정이 뒤집히나
    rows = []
    for pc in args.sweep_per_cat:
        for off in args.sweep_offsets:
            s = sample_offset(terms, pc, off)
            v = gate_verdicts(new, s)
            rows.append({"per_cat": pc, "offset": off, "sample_n": len(s),
                         "gate2": v["gate2_alias_median_equal"],
                         "gate3": v["gate3_tone_not_increased"],
                         "base_alias_median": v["_detail"]["base_alias"],
                         "base_tone_per_1k": v["_detail"]["base_tone_per_1k"]})

    gate2_stable = all(r["gate2"] for r in rows)
    gate3_stable = all(r["gate3"] for r in rows)

    report = {
        "full_population": full,
        "sampling_sweep": {
            "n_samples_tried": len(rows),
            "gate2_stable_across_all": gate2_stable,
            "gate3_stable_across_all": gate3_stable,
            "gate2_flips": [r for r in rows if not r["gate2"]],
            "rows": rows,
        },
        "verdict": {
            "robust": bool(gate2_stable and gate3_stable
                           and full["gate1_validator_100"]
                           and full["gate2_alias_median_equal"]
                           and full["gate3_tone_not_increased"]),
            "note": "robust=True면 원래 30샘플 PASS는 샘플 선택과 무관한 결론.",
        },
    }
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""
Phase 2A — 기존 terms.json 베이스라인 vs 신규 batch 일관성 점검 (코드만, API 불필요).

spec.md Phase 2 (A) 정본:
  drift gate (실패 시 Phase 1 회귀):
    - validator 통과율: 신규 batch는 100% 필수. legacy sample은 grandfather(47~63% 비순응 예상) → 비교 대상 아님, 기록만.
    - alias 개수 중앙값: 신규 == sample
    - 톤 빈도(부사·감탄사·과장 형용사): 신규가 sample 대비 명백히 증가하지 않음
  informational only (gate 아님):
    - 길이 평균·분포: 베이스라인이 길이 룰 비순응이라 ±% 비교 신뢰도 낮음 → 시계열 추적용 기록만

샘플링은 결정론적이다 (재현 가능):
  카테고리별로 keyword 알파벳 정렬 후 균등 간격(evenly spaced)으로 per_cat개 추출.

사용법:
    python consistency_a.py <round.json> <terms.json> [--per-cat 5]
    stdout에 metrics JSON 출력.
"""

from __future__ import annotations

import argparse
import json
import re
import statistics
from collections import Counter
from pathlib import Path
from typing import Any

# validator.py와 동일한 길이 필드
TEXT_FIELDS = ("summary", "etymology", "namingReason")

# 톤 어휘 — "이름이냐 설명이냐" 원칙에 어긋나는 과장·홍보 톤 표지.
# 사전 항목은 사실 서술이어야 하므로 아래 어휘 빈도가 신규에서 명백히 늘면 톤 drift 신호.
TONE_LEXICON = {
    "부사": [
        "매우", "굉장히", "너무", "정말", "진짜", "아주", "무척", "상당히",
        "훨씬", "엄청", "극히", "대단히", "몹시", "특히나", "워낙", "유난히",
    ],
    "감탄사": [
        "와우", "오호", "아하", "자,", "와,", "어머",
    ],
    "과장형용사": [
        "놀라운", "놀랍게", "강력한", "강력하게", "압도적", "혁신적", "획기적",
        "엄청난", "어마어마", "탁월한", "뛰어난", "막강한", "대단한", "환상적",
        "완벽한", "완벽하게", "최고의", "독보적", "경이로운", "눈부신",
    ],
}


def load(path: Path) -> list[dict[str, Any]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, list):
        raise SystemExit(f"{path}: JSON 배열이 아님")
    return data


def sample_by_category(terms: list[dict], per_cat: int) -> list[dict]:
    """카테고리별 keyword 정렬 후 균등 간격 per_cat개. 결정론적."""
    by_cat: dict[str, list[dict]] = {}
    for t in terms:
        by_cat.setdefault(t.get("category", "?"), []).append(t)
    picked: list[dict] = []
    for cat in sorted(by_cat):
        entries = sorted(by_cat[cat], key=lambda e: e.get("keyword", ""))
        n = len(entries)
        if n <= per_cat:
            picked.extend(entries)
            continue
        # 균등 간격 인덱스 (양끝 포함 분포)
        idxs = [round(i * (n - 1) / (per_cat - 1)) for i in range(per_cat)]
        picked.extend(entries[i] for i in sorted(set(idxs)))
    return picked


def text_of(entry: dict) -> str:
    return " ".join(str(entry.get(f, "")) for f in TEXT_FIELDS)


def alias_counts(entries: list[dict]) -> list[int]:
    return [len(e.get("aliases") or []) for e in entries]


def length_stats(entries: list[dict]) -> dict[str, dict[str, float]]:
    out: dict[str, dict[str, float]] = {}
    for f in TEXT_FIELDS:
        vals = [len(str(e.get(f, ""))) for e in entries]
        out[f] = {
            "mean": round(statistics.mean(vals), 1),
            "median": statistics.median(vals),
            "min": min(vals),
            "max": max(vals),
        }
    return out


def tone_freq(entries: list[dict]) -> dict[str, Any]:
    """카테고리별 어휘 매칭 횟수, entry당·1000자당 정규화 빈도."""
    total_chars = sum(len(text_of(e)) for e in entries)
    n = len(entries)
    cat_hits: dict[str, int] = {}
    hit_examples: dict[str, Counter] = {}
    for cat, words in TONE_LEXICON.items():
        hits = 0
        ex: Counter = Counter()
        for e in entries:
            txt = text_of(e)
            for w in words:
                c = txt.count(w)
                if c:
                    hits += c
                    ex[w] += c
        cat_hits[cat] = hits
        hit_examples[cat] = ex
    total_hits = sum(cat_hits.values())
    return {
        "by_category": cat_hits,
        "examples": {k: dict(v) for k, v in hit_examples.items() if v},
        "total_hits": total_hits,
        "per_entry": round(total_hits / n, 3) if n else 0.0,
        "per_1000_chars": round(total_hits / total_chars * 1000, 3) if total_chars else 0.0,
        "n_entries": n,
        "total_chars": total_chars,
    }


def validator_pass_rate(entries: list[dict]) -> dict[str, Any]:
    """validator.py를 import하여 통과율 계산."""
    import importlib.util

    vpath = Path(__file__).with_name("validator.py")
    spec = importlib.util.spec_from_file_location("validator", vpath)
    if spec is None or spec.loader is None:
        raise SystemExit("validator.py 로드 실패")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    result = mod.validate(entries)
    n = len(entries)
    passed = len(result["passed"])
    # 실패 룰 분포
    rule_dist = Counter(f["rule_id"] for f in result["failed"])
    return {
        "n": n,
        "passed": passed,
        "rate": round(passed / n, 3) if n else 0.0,
        "failed_rule_dist": dict(rule_dist),
    }


def main() -> int:
    ap = argparse.ArgumentParser(description="Phase 2A 일관성 점검 (코드만)")
    ap.add_argument("round", type=Path, help="신규 batch JSON (round-NNN.json)")
    ap.add_argument("terms", type=Path, help="기존 terms.json")
    ap.add_argument("--per-cat", type=int, default=5, help="카테고리별 샘플 개수")
    args = ap.parse_args()

    new = load(args.round)
    terms = load(args.terms)
    sample = sample_by_category(terms, args.per_cat)

    report = {
        "params": {"per_cat": args.per_cat, "new_n": len(new), "sample_n": len(sample),
                   "terms_total": len(terms)},
        "sample_keywords": [e.get("keyword") for e in sample],
        "validator": {
            "new": validator_pass_rate(new),
            "sample_legacy": validator_pass_rate(sample),
        },
        "alias_count": {
            "new_median": statistics.median(alias_counts(new)),
            "sample_median": statistics.median(alias_counts(sample)),
            "new_dist": dict(Counter(alias_counts(new))),
            "sample_dist": dict(Counter(alias_counts(sample))),
        },
        "tone": {
            "new": tone_freq(new),
            "sample": tone_freq(sample),
        },
        "length_informational": {
            "new": length_stats(new),
            "sample": length_stats(sample),
        },
    }
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

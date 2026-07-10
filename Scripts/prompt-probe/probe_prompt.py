"""
v2 직교성 검증 측정 — 8 cell × 15 keyword.

실행:
    export ANTHROPIC_API_KEY=sk-ant-...
    python probe_prompt.py

결과 저장 (results/{YYYY-MM-DD_HHMM}/):
    manifest.json           # run 메타데이터
    prompts_used.json       # hash → 시스템 프롬프트 풀텍스트 매핑
    raw/{cell}__{keyword}.json   # API 응답 전체 + 메타
    metrics/per_response.csv     # 응답별 메트릭
    metrics/summary.csv          # cell별 집계

총 120 API 호출. 예상 시간 ~15-20분 (직렬, latency 평균 8초 가정).
예상 비용 ~$1-2 (Sonnet 4.6, cache 활용 시).
"""

import os
import sys
import json
import csv
import time
import hashlib
import datetime
from pathlib import Path
from collections import defaultdict

try:
    import anthropic
except ImportError:
    print("ERROR: anthropic SDK가 설치되어 있지 않습니다. `pip install anthropic`")
    sys.exit(1)

from prompts.build import CELLS
from prompts.tools import TOOLS
from prompts.verify import verify_cells
from keywords import KEYWORDS
from metrics import compute_metrics


# ──────────────────────────────────────────────────────────────────
# 설정 — 본인의 production 코드(shared/.../Constants.kt Constants)와 동기화
# ──────────────────────────────────────────────────────────────────

MODEL = "claude-sonnet-4-6"     # ⚠️ 본인 Constants.claudeModel과 일치 확인 필요
MAX_TOKENS = 4096
THINKING_BUDGET = 2000          # shared/.../ClaudePrompt.kt와 동일

# 재시도 정책
MAX_RETRIES = 3
RETRY_DELAY_SEC = 5

# ──────────────────────────────────────────────────────────────────


def prompt_hash(prompt: str) -> str:
    return hashlib.sha256(prompt.encode("utf-8")).hexdigest()


def call_claude(client, system_prompt: str, keyword: str) -> tuple[dict, int]:
    """한 API 호출. system은 cache_control ephemeral로 캐싱.

    Returns:
        (response_dict, latency_ms)
    """
    start = time.time()

    response = client.messages.create(
        model=MODEL,
        max_tokens=MAX_TOKENS,
        thinking={"type": "enabled", "budget_tokens": THINKING_BUDGET},
        system=[{
            "type": "text",
            "text": system_prompt,
            "cache_control": {"type": "ephemeral"},
        }],
        tools=TOOLS,
        tool_choice={"type": "auto"},
        messages=[{"role": "user", "content": keyword}],
    )

    latency_ms = int((time.time() - start) * 1000)
    # SDK 응답을 JSON 직렬화 가능한 dict로 변환
    return response.model_dump(mode="json"), latency_ms


def call_with_retry(client, system_prompt: str, keyword: str) -> tuple[dict, int]:
    """call_claude를 rate limit/일시 오류에 대한 백오프로 감쌈."""
    for attempt in range(MAX_RETRIES):
        try:
            return call_claude(client, system_prompt, keyword)
        except anthropic.RateLimitError as e:
            wait = RETRY_DELAY_SEC * (attempt + 1)
            print(f"\n  ⚠ Rate limit. {wait}초 후 재시도 ({attempt + 1}/{MAX_RETRIES})...")
            time.sleep(wait)
        except (anthropic.APIConnectionError, anthropic.InternalServerError) as e:
            wait = RETRY_DELAY_SEC * (attempt + 1)
            print(f"\n  ⚠ {type(e).__name__}. {wait}초 후 재시도 ({attempt + 1}/{MAX_RETRIES})...")
            time.sleep(wait)
    raise RuntimeError(f"{MAX_RETRIES}회 재시도 실패")


def save_manifest(out_dir: Path, run_id: str) -> None:
    manifest = {
        "run_id": run_id,
        "timestamp": datetime.datetime.now().isoformat(),
        "model": MODEL,
        "max_tokens": MAX_TOKENS,
        "thinking_budget": THINKING_BUDGET,
        "cells": list(CELLS.keys()),
        "n_cells": len(CELLS),
        "keywords": [k["keyword"] for k in KEYWORDS],
        "n_keywords": len(KEYWORDS),
        "total_calls": len(CELLS) * len(KEYWORDS),
    }
    (out_dir / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def save_prompts_used(out_dir: Path) -> dict[str, str]:
    """8 cell의 prompt 풀텍스트를 hash 매핑 형태로 저장.

    Returns:
        cell_name → prompt_hash 매핑 (raw 파일에서 hash로 참조)
    """
    prompts_used = {}
    cell_to_hash = {}
    for name, prompt in CELLS.items():
        h = prompt_hash(prompt)
        prompts_used[h] = {
            "cell": name,
            "length": len(prompt),
            "text": prompt,
        }
        cell_to_hash[name] = h
    (out_dir / "prompts_used.json").write_text(
        json.dumps(prompts_used, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    return cell_to_hash


def save_raw_response(
    raw_dir: Path,
    cell_name: str,
    keyword_spec: dict,
    response: dict,
    latency_ms: int,
    cell_hash: str,
) -> None:
    """단일 raw 응답을 파일로 저장. prompt 풀텍스트는 prompts_used.json에서 hash로 참조."""
    record = {
        "meta": {
            "cell": cell_name,
            "keyword": keyword_spec["keyword"],
            "group": keyword_spec["group"],
            "prompt_hash": cell_hash,
            "model": MODEL,
            "timestamp": datetime.datetime.now().isoformat(),
            "latency_ms": latency_ms,
        },
        "expected": keyword_spec,
        "response": response,
    }
    path = raw_dir / f"{cell_name}__{keyword_spec['keyword']}.json"
    path.write_text(
        json.dumps(record, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def write_per_response_csv(rows: list[dict], path: Path) -> None:
    """per_response.csv 저장. cell·keyword·group을 맨 앞으로."""
    if not rows:
        return

    # 첫 행 기준 컬럼 + 앞쪽 컬럼 강제 배치
    front = ["cell", "keyword", "group", "expected_branch", "actual_branch", "branch_correct"]
    base_keys = list(rows[0].keys())
    rest = [k for k in base_keys if k not in front]
    fieldnames = front + rest

    with path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def write_summary_csv(rows: list[dict], path: Path) -> None:
    """cell별 핵심 메트릭 집계."""
    by_cell = defaultdict(list)
    for row in rows:
        by_cell[row["cell"]].append(row)

    summary_rows = []
    for cell_name in CELLS.keys():  # CELLS 순서 유지
        cell_rows = by_cell.get(cell_name, [])
        if not cell_rows:
            continue

        term_rows = [r for r in cell_rows if r["actual_branch"] == "term_entry"]
        out_of_shot_term = [r for r in term_rows if r["group"] == "out_of_shot"]
        in_shot_term = [r for r in term_rows if r["group"] == "in_shot"]

        # closing 효과 메트릭 — out_of_shot 그룹의 naming 길이
        oos_naming_lens = [r["naming_len"] for r in out_of_shot_term if r["naming_len"] > 0]
        oos_under_270 = sum(1 for r in out_of_shot_term if r["naming_under_270"])

        # alias_strict 효과 메트릭 — 한정 수식어 검출 비율 (term_entry 응답 한정)
        alias_qual = sum(1 for r in term_rows if r["aliases_has_qualifier"])

        # selfcheck 효과 메트릭 — 분기 정확도 (전체 keyword 기준)
        branch_correct = sum(1 for r in cell_rows if r["branch_correct"])

        # latency·thinking 사용량
        latencies = [r["latency_ms"] for r in cell_rows]
        thinking_chars = [r["thinking_chars"] for r in cell_rows]

        summary_rows.append({
            "cell": cell_name,
            "n_total": len(cell_rows),
            "branch_correct": f"{branch_correct}/{len(cell_rows)}",
            "oos_naming_avg": f"{sum(oos_naming_lens)/len(oos_naming_lens):.1f}" if oos_naming_lens else "N/A",
            "oos_naming_max": max(oos_naming_lens) if oos_naming_lens else 0,
            "oos_under_270": f"{oos_under_270}/{len(out_of_shot_term)}",
            "in_shot_naming_avg": f"{sum(r['naming_len'] for r in in_shot_term)/len(in_shot_term):.1f}" if in_shot_term else "N/A",
            "aliases_qualifier": f"{alias_qual}/{len(term_rows)}",
            "latency_avg_ms": f"{sum(latencies)/len(latencies):.0f}",
            "thinking_chars_avg": f"{sum(thinking_chars)/len(thinking_chars):.0f}",
        })

    if not summary_rows:
        return
    fieldnames = list(summary_rows[0].keys())
    with path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(summary_rows)


def run_measurement():
    # 환경변수 확인
    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        print("ERROR: 환경변수 ANTHROPIC_API_KEY가 설정되어 있지 않습니다.")
        sys.exit(1)

    # Sanity check 먼저
    print("=== Sanity check ===")
    if not verify_cells():
        print("\nSanity check 실패. 측정 중단.")
        sys.exit(1)
    print()

    # 결과 폴더 준비
    run_id = datetime.datetime.now().strftime("%Y-%m-%d_%H%M")
    out_dir = Path("results") / run_id
    raw_dir = out_dir / "raw"
    metrics_dir = out_dir / "metrics"
    raw_dir.mkdir(parents=True, exist_ok=True)
    metrics_dir.mkdir(parents=True, exist_ok=True)

    save_manifest(out_dir, run_id)
    cell_to_hash = save_prompts_used(out_dir)

    # 측정
    client = anthropic.Anthropic(api_key=api_key)
    total = len(CELLS) * len(KEYWORDS)
    idx = 0
    per_response_rows = []

    print(f"=== 측정 시작 (총 {total} 호출) ===")
    print(f"결과 저장 위치: {out_dir}\n")

    overall_start = time.time()

    for cell_name, prompt in CELLS.items():
        cell_hash = cell_to_hash[cell_name]
        print(f"--- Cell: {cell_name} ({len(prompt)}자, hash={cell_hash[:8]}) ---")

        for kw_spec in KEYWORDS:
            idx += 1
            keyword = kw_spec["keyword"]
            print(f"  [{idx:3d}/{total}] {keyword:12s}", end=" ", flush=True)

            try:
                response, latency_ms = call_with_retry(client, prompt, keyword)
            except Exception as e:
                print(f"FAILED: {e}")
                continue

            save_raw_response(raw_dir, cell_name, kw_spec, response, latency_ms, cell_hash)

            m = compute_metrics(response, kw_spec)
            m["cell"] = cell_name
            m["latency_ms"] = latency_ms
            per_response_rows.append(m)

            # 콘솔 한 줄 요약
            cache_status = (
                "CREATE" if m["cache_create_tokens"] > 0
                else "READ" if m["cache_read_tokens"] > 0
                else "none"
            )
            branch_mark = "✓" if m["branch_correct"] else "✗"
            naming_info = f"naming={m['naming_len']:3d}" if m["naming_len"] > 0 else "         "
            print(
                f"{branch_mark} {m['actual_branch']:13s} {naming_info}  "
                f"{latency_ms/1000:5.1f}s  cache={cache_status}"
            )

        print()

    elapsed = time.time() - overall_start

    # 메트릭 파일 저장
    write_per_response_csv(per_response_rows, metrics_dir / "per_response.csv")
    write_summary_csv(per_response_rows, metrics_dir / "summary.csv")

    print(f"=== 측정 완료 ===")
    print(f"총 소요 시간: {elapsed/60:.1f}분")
    print(f"성공 호출: {len(per_response_rows)}/{total}")
    print(f"결과: {out_dir}/")
    print(f"  - manifest.json")
    print(f"  - prompts_used.json")
    print(f"  - raw/  ({len(per_response_rows)} 파일)")
    print(f"  - metrics/per_response.csv")
    print(f"  - metrics/summary.csv")


if __name__ == "__main__":
    run_measurement()

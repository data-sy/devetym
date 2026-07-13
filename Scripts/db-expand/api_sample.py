#!/usr/bin/env python3
"""
Phase 2B — chat↔API drift 검증 (API 필요).

같은 v2-batch.md system prompt + round-001의 keyword 일부를 Anthropic API로 재실행해,
claude.ai 채팅 탭에서 생성한 round-001.json과 비교한다.

production 일치 (drift를 재려면 생성 조건이 production과 같아야 함):
  - 모델 id: Scripts/generate_db.py(CLAUDE_MODEL) / shared/.../Constants.kt(Constants.claudeModel)
            = "claude-sonnet-4-6"
  - anthropic-version: 2023-06-01
  - max_tokens 8192, system에 cache_control ephemeral — generate_db.py call_claude와 동일
  - raw urllib (production 파이프라인과 동일, SDK 미사용)
다른 점: system prompt = generate_db.py SYSTEM_PROMPT 가 아니라 prompts/v2-batch.md 본문
        (chat 탭이 쓴 프롬프트와 같아야 chat↔API 비교가 성립).

drift threshold (spec Phase 2 B):
  - validator 통과율: API 출력도 신규 batch이므로 100% 기대 (round-001과 동일 게이트)
  - 같은 keyword pair의 길이 편차 ±15% 이내 (summary/etymology/namingReason)

사용법:
    export ANTHROPIC_API_KEY=sk-ant-...
    python3 api_sample.py --round docs/db-expand/rounds/round-001.json --n 5
    python3 api_sample.py --round ... --keywords sni,hsts,sharding
    # 이미 받은 API 응답으로 비교만:
    python3 api_sample.py --round ... --compare-only out/round-001-api.json

stdout: 비교 metrics JSON. --out 지정 시 API 원응답도 파일로 저장.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.request
import urllib.error
from pathlib import Path
from typing import Any

# production 값과 일치 (generate_db.py / shared/.../Constants.kt Constants.claudeModel)
CLAUDE_MODEL = "claude-sonnet-4-6"
API_URL = "https://api.anthropic.com/v1/messages"
API_VERSION = "2023-06-01"
MAX_TOKENS = 8192

TEXT_FIELDS = ("summary", "etymology", "namingReason")
LEN_DRIFT_THRESHOLD = 0.15  # ±15%

PROMPT_PATH = Path(__file__).with_name("prompts") / "v2-batch.md"


def extract_system_prompt(md_path: Path) -> str:
    """v2-batch.md에서 '---' 펜스 이후 본문(=탭에 paste하는 system instruction)만 추출.

    파일은 헤더(설명) → '---' → 본문(EOF까지) 구조. 닫는 '---'가 있으면 그 사이,
    없으면 첫 '---' 이후 EOF까지를 본문으로 본다.
    """
    lines = md_path.read_text(encoding="utf-8").splitlines()
    fences = [i for i, ln in enumerate(lines) if ln.strip() == "---"]
    if not fences:
        raise SystemExit(f"{md_path}: '---' 펜스를 찾지 못함")
    start = fences[0] + 1
    end = fences[1] if len(fences) >= 2 else len(lines)
    return "\n".join(lines[start:end]).strip()


def call_claude(api_key: str, keywords: list[str], system_prompt: str) -> list[dict[str, Any]]:
    """generate_db.py call_claude와 동일한 호출. system prompt만 교체, 입력은 keyword 리스트."""
    # v2-batch.md few-shot의 입력 형식(JSON 배열)과 동일하게 제시
    user_prompt = json.dumps(keywords, ensure_ascii=False)
    body = json.dumps({
        "model": CLAUDE_MODEL,
        "max_tokens": MAX_TOKENS,
        "system": [
            {"type": "text", "text": system_prompt, "cache_control": {"type": "ephemeral"}}
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


def validator_pass(entries: list[dict]) -> dict[str, Any]:
    """validator.py import 후 통과율."""
    import importlib.util
    from collections import Counter

    vpath = Path(__file__).with_name("validator.py")
    spec = importlib.util.spec_from_file_location("validator", vpath)
    if spec is None or spec.loader is None:
        raise SystemExit("validator.py 로드 실패")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    result = mod.validate(entries)
    n = len(entries)
    return {
        "n": n,
        "passed": len(result["passed"]),
        "rate": round(len(result["passed"]) / n, 3) if n else 0.0,
        "failed": result["failed"],
        "failed_rule_dist": dict(Counter(f["rule_id"] for f in result["failed"])),
    }


def compare_lengths(chat: list[dict], api: list[dict]) -> dict[str, Any]:
    """같은 keyword pair의 필드 길이 편차. |api-chat|/chat 가 ±15% 초과면 위반."""
    chat_by_kw = {e["keyword"]: e for e in chat}
    pairs = []
    violations = []
    for a in api:
        kw = a.get("keyword")
        c = chat_by_kw.get(kw)
        if c is None:
            continue
        row: dict[str, Any] = {"keyword": kw}
        for f in TEXT_FIELDS:
            clen = len(str(c.get(f, "")))
            alen = len(str(a.get(f, "")))
            dev = abs(alen - clen) / clen if clen else None
            row[f] = {"chat": clen, "api": alen,
                      "dev_pct": round(dev * 100, 1) if dev is not None else None}
            if dev is not None and dev > LEN_DRIFT_THRESHOLD:
                violations.append({"keyword": kw, "field": f,
                                   "chat": clen, "api": alen, "dev_pct": round(dev * 100, 1)})
        pairs.append(row)
    return {"pairs": pairs, "violations": violations,
            "compared_keywords": [p["keyword"] for p in pairs],
            "missing_in_api": [kw for kw in chat_by_kw if kw not in {a.get("keyword") for a in api}]}


def main() -> int:
    ap = argparse.ArgumentParser(description="Phase 2B chat↔API drift 검증")
    ap.add_argument("--round", type=Path, required=True, help="비교 기준 round-NNN.json (chat 산출물)")
    ap.add_argument("--n", type=int, default=5, help="앞에서부터 샘플할 keyword 수 (--keywords 없을 때)")
    ap.add_argument("--keywords", type=str, default=None, help="쉼표구분 keyword 명시 (예: sni,hsts)")
    ap.add_argument("--out", type=Path, default=None, help="API 원응답 저장 경로")
    ap.add_argument("--compare-only", type=Path, default=None,
                    help="API 미호출, 이 파일(API 응답 JSON)로 비교만")
    args = ap.parse_args()

    chat = json.loads(args.round.read_text(encoding="utf-8"))
    if not isinstance(chat, list):
        raise SystemExit("--round 는 JSON 배열이어야 함")

    if args.keywords:
        keywords = [k.strip() for k in args.keywords.split(",") if k.strip()]
    else:
        keywords = [e["keyword"] for e in chat][: args.n]

    if args.compare_only:
        api = json.loads(args.compare_only.read_text(encoding="utf-8"))
    else:
        api_key = os.environ.get("ANTHROPIC_API_KEY", "").strip()
        if not api_key:
            sys.exit("ANTHROPIC_API_KEY 환경변수가 비어있습니다")
        system_prompt = extract_system_prompt(PROMPT_PATH)
        print(f"[api_sample] {CLAUDE_MODEL} 호출: {keywords}", file=sys.stderr)
        api = call_claude(api_key, keywords, system_prompt)
        if args.out:
            args.out.parent.mkdir(parents=True, exist_ok=True)
            args.out.write_text(json.dumps(api, ensure_ascii=False, indent=2), encoding="utf-8")
            print(f"[api_sample] API 원응답 저장: {args.out}", file=sys.stderr)

    # chat 쪽도 같은 keyword만 추려 validator 통과율 비교
    chat_subset = [e for e in chat if e.get("keyword") in set(keywords)]
    report = {
        "params": {"model": CLAUDE_MODEL, "version": API_VERSION, "max_tokens": MAX_TOKENS,
                   "keywords": keywords},
        "validator": {
            "api": validator_pass(api),
            "chat_subset": validator_pass(chat_subset),
        },
        "length_drift": compare_lengths(chat, api),
        "threshold": {"len_drift_pct": LEN_DRIFT_THRESHOLD * 100},
    }
    report["verdict"] = {
        "validator_api_100": report["validator"]["api"]["rate"] == 1.0,
        "len_drift_within_15pct": len(report["length_drift"]["violations"]) == 0,
    }
    report["verdict"]["pass"] = all(report["verdict"].values())
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

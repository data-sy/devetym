#!/usr/bin/env python3
"""
DevEtym 비용 리포트 — Anthropic Admin API로 조직 비용/사용량 조회.

docs/cost/cost-management-decision.md의 "가시성 층 정본". 스크립트·프록시 어디에도
로깅을 심지 않고, Anthropic이 계산한 조직 단위 비용을 주기 조회한다.

사용법:
    export ANTHROPIC_ADMIN_KEY=sk-ant-admin01-...   # Admin 키 (일반 API 키 아님)
    python Scripts/cost/report.py                    # 이번 달 1일~오늘
    python Scripts/cost/report.py --days 7           # 최근 7일
    python Scripts/cost/report.py --month 2026-06    # 특정 월
    python Scripts/cost/report.py --budget 25        # 예산 대비 % 표시 (기본 $25)

전제:
    - Admin 키는 Console → Settings → Organization에서 조직을 만든 뒤 발급 가능.
      ⚠️ "개인 계정(individual account)" 상태에서는 Admin API를 쓸 수 없다 —
      조직 설정이 선행 사람 작업 (decision doc "지금 당장" 체크리스트).
    - 데이터 신선도: 호출 후 ~5분 내 반영. 폴링은 분당 1회 이하 권장(우린 수동 실행).
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

API_BASE = "https://api.anthropic.com/v1/organizations"
API_VERSION = "2023-06-01"
DEFAULT_BUDGET_USD = 25.0  # 결정 문서의 임시 월 상한. 실측 후 조정.


def admin_get(admin_key: str, path: str, params: dict) -> dict:
    """Admin API GET 한 번. 배열 파라미터는 key[]=v 형태."""
    pairs: list[tuple[str, str]] = []
    for k, v in params.items():
        if isinstance(v, list):
            pairs.extend((f"{k}[]", item) for item in v)
        elif v is not None:
            pairs.append((k, str(v)))
    url = f"{API_BASE}/{path}?{urllib.parse.urlencode(pairs)}"
    req = urllib.request.Request(url, headers={
        "x-api-key": admin_key,
        "anthropic-version": API_VERSION,
        "User-Agent": "devetym-cost-report/1.0",
    })
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", errors="replace")
        if e.code in (401, 403):
            sys.exit(
                "Admin API 인증 실패 (HTTP {code}).\n"
                "확인할 것:\n"
                "  1. ANTHROPIC_ADMIN_KEY가 sk-ant-admin... 으로 시작하는 Admin 키인가?\n"
                "     (일반 sk-ant-api... 키는 이 API를 못 쓴다)\n"
                "  2. Console이 아직 '개인 계정'이면 Admin API 자체가 막혀 있다 —\n"
                "     Console → Settings → Organization에서 조직을 먼저 만들 것.\n"
                "원문: {detail}".format(code=e.code, detail=detail[:300])
            )
        sys.exit(f"Admin API 오류 (HTTP {e.code}): {detail[:300]}")


def paged(admin_key: str, path: str, params: dict) -> list[dict]:
    """has_more/next_page 페이지네이션을 따라가며 data 버킷을 전부 모은다."""
    buckets: list[dict] = []
    page = None
    while True:
        payload = admin_get(admin_key, path, {**params, "page": page})
        buckets.extend(payload.get("data", []))
        if not payload.get("has_more"):
            return buckets
        page = payload.get("next_page")


def workspace_names(admin_key: str) -> dict[str, str]:
    """workspace_id → 이름 매핑. 실패해도 리포트는 계속(ID로 표시)."""
    try:
        payload = admin_get(admin_key, "workspaces", {"limit": 100})
    except SystemExit:
        raise
    except Exception:
        return {}
    return {
        w["id"]: w.get("name", w["id"])
        for w in payload.get("data", [])
        if isinstance(w, dict) and "id" in w
    }


def iso(d: dt.date) -> str:
    return f"{d.isoformat()}T00:00:00Z"


def date_range(args: argparse.Namespace) -> tuple[dt.date, dt.date]:
    today = dt.date.today()
    if args.month:
        first = dt.date.fromisoformat(f"{args.month}-01")
        # 다음 달 1일 (ending_at은 exclusive)
        nxt = (first.replace(day=28) + dt.timedelta(days=4)).replace(day=1)
        return first, min(nxt, today + dt.timedelta(days=1))
    if args.days:
        return today - dt.timedelta(days=args.days), today + dt.timedelta(days=1)
    return today.replace(day=1), today + dt.timedelta(days=1)  # 이번 달 1일~오늘


def cents_to_usd(amount: str | float | int) -> float:
    """비용은 최저 단위(센트)의 십진 문자열로 온다."""
    try:
        return float(amount) / 100.0
    except (TypeError, ValueError):
        return 0.0


def collect_costs(buckets: list[dict], names: dict[str, str]) -> tuple[dict[str, float], dict[str, float]]:
    """버킷들을 (워크스페이스별 합계, 일별 합계)로 접는다."""
    by_ws: dict[str, float] = {}
    by_day: dict[str, float] = {}
    for bucket in buckets:
        day = str(bucket.get("starting_at", ""))[:10]
        for row in bucket.get("results", []):
            usd = cents_to_usd(row.get("amount"))
            ws_id = row.get("workspace_id")
            ws = names.get(ws_id, ws_id) if ws_id else "(기본 워크스페이스)"
            by_ws[ws] = by_ws.get(ws, 0.0) + usd
            by_day[day] = by_day.get(day, 0.0) + usd
    return by_ws, by_day


def sum_tokens(buckets: list[dict]) -> dict[str, int]:
    """usage 버킷에서 *_tokens 필드를 필드명별로 합산 (스키마 드리프트에 관대)."""
    totals: dict[str, int] = {}
    for bucket in buckets:
        for row in bucket.get("results", []):
            for key, val in row.items():
                if key.endswith("_tokens") and isinstance(val, (int, float)):
                    totals[key] = totals.get(key, 0) + int(val)
    return totals


def main() -> int:
    ap = argparse.ArgumentParser(description="Anthropic 조직 비용 리포트")
    ap.add_argument("--days", type=int, help="최근 N일 (기본: 이번 달 1일~오늘)")
    ap.add_argument("--month", help="특정 월 YYYY-MM")
    ap.add_argument("--budget", type=float, default=DEFAULT_BUDGET_USD,
                    help=f"월 예산(USD, 기본 {DEFAULT_BUDGET_USD}) — 대비 %% 표시")
    args = ap.parse_args()

    admin_key = os.environ.get("ANTHROPIC_ADMIN_KEY", "").strip()
    if not admin_key:
        sys.exit(
            "ANTHROPIC_ADMIN_KEY 환경변수가 비어있습니다.\n"
            "Console → Settings → Organization → Admin keys에서 발급 (sk-ant-admin...).\n"
            "개인 계정 상태면 먼저 조직을 만들어야 합니다."
        )

    start, end = date_range(args)
    print(f"기간: {start} ~ {end - dt.timedelta(days=1)} (UTC 일 단위)\n")

    names = workspace_names(admin_key)

    # 1) 비용 — 워크스페이스별로 묶어 표면(dev-scripts vs proxy-runtime) 분리
    cost_buckets = paged(admin_key, "cost_report", {
        "starting_at": iso(start),
        "ending_at": iso(end),
        "group_by": ["workspace_id"],
        "bucket_width": "1d",
    })
    by_ws, by_day = collect_costs(cost_buckets, names)
    total = sum(by_ws.values())

    print("── 표면별 비용 (워크스페이스) ──")
    if not by_ws:
        print("  기록된 비용 없음 ($0.00)")
    for ws, usd in sorted(by_ws.items(), key=lambda x: -x[1]):
        print(f"  {ws:24s} ${usd:8.2f}")
    print(f"  {'합계':24s} ${total:8.2f}   (예산 ${args.budget:.0f} 대비 {total / args.budget * 100:.0f}%)")

    if by_day:
        print("\n── 일별 추이 ──")
        for day in sorted(by_day):
            bar = "█" * min(40, round(by_day[day] / max(by_day.values()) * 40)) if max(by_day.values()) > 0 else ""
            print(f"  {day}  ${by_day[day]:7.2f}  {bar}")

    # 2) 사용량 — 토큰 합계 (캐시 효율 확인용)
    usage_buckets = paged(admin_key, "usage_report/messages", {
        "starting_at": iso(start),
        "ending_at": iso(end),
        "bucket_width": "1d",
    })
    tokens = sum_tokens(usage_buckets)
    if tokens:
        print("\n── 토큰 사용량 합계 ──")
        for key in sorted(tokens):
            print(f"  {key:32s} {tokens[key]:>12,}")
        cached = sum(v for k, v in tokens.items() if "cache_read" in k)
        uncached = sum(v for k, v in tokens.items() if "input" in k and "cache" not in k)
        if cached + uncached > 0:
            print(f"  → 입력 캐시 적중 비율: {cached / (cached + uncached) * 100:.0f}% (높을수록 절감 중)")

    return 0


if __name__ == "__main__":
    sys.exit(main())

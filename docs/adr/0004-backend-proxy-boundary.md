# ADR 0004: 백엔드 프록시 경계 계승 — Cloudflare Worker, 클라이언트가 프롬프트 소유

## Status
Accepted (2026-07-04) — 프록시 계약 자체는 기존 백엔드([`devetym-proxy`](https://github.com/data-sy/devetym-proxy))에서 계승.

## Context
AI 폴백(번들 DB 미스 시 Claude 생성)은 **얇은 프록시**를 거친다 — 앱 → Cloudflare Worker → Claude API. 키는 서버에만 있고 앱에는 없다. 프록시는 **키 주입 + 기기당 일일 호출 한도**를 강제한다(디컴파일로 키 탈취·비용 폭증 방어).

이 백엔드 계약은 **플랫폼과 무관**하다 — 클라이언트가 Swift(URLSession)에서 Kotlin(Ktor)으로 바뀌어도 서버는 그대로다. 결정할 것은 "CMP 클라이언트가 이 계약을 어떻게 계승하는가"와 "프롬프트·도구 스키마를 어디에 두는가"다.

관찰된 현행 계약:
- 클라이언트가 **Anthropic Messages API 요청 본문을 통째로 구성**한다 — 모델·`max_tokens`·`thinking`·`system`(프롬프트)·`tools`·`tool_choice`.
- 익명 **`X-Device-Id`** 헤더로 한도 카운터를 식별한다.
- 한도 초과는 **HTTP `429`**로 통지.
- 응답은 **tool_use 3분기**(`return_term_entry`/`return_not_dev_term`/`return_possible_typo`)로 파싱.

## Decision
**기존 프록시 계약을 그대로 계승한다.**
- CMP 클라이언트(Ktor)가 동일한 요청 본문 형태를 구성하고 `X-Device-Id`를 실어 보낸다. `429` → `DailyLimitExceeded`.
- **시스템 프롬프트·도구 스키마는 클라이언트(`commonMain`)에 둔다** — 현행 계약을 유지. 두 클라이언트(iOS·CMP)가 **같은 프록시**를 공유한다.
- 프롬프트를 서버로 옮기는 것은 이 ADR의 범위 밖(향후 재검토 항목).

## Consequences

### Positive
- 서버 재작성 0 — `devetym-proxy` 백엔드를 두 앱이 공유한다.
- 프롬프트·도구 스키마가 `commonMain`에 있어 **Android·iOS가 동일 프롬프트**를 자동 공유한다(품질 일관성).
- 온디바이스 키 없음 → 디컴파일 키 탈취 방어를 두 플랫폼에서 동일하게 확보.

### Negative
- 프롬프트가 클라이언트에 있어 **프롬프트 개정 시 앱 재배포**가 필요하다(서버 핫픽스 불가).
- 클라이언트가 Anthropic 본문 스키마를 알아야 해 프록시가 완전 불투명 통로는 아니다(계약 결합).

### Neutral
- 기기당 일일 한도 시작값(10회)·카운터 저장소(KV) 등 정책 수치는 백엔드 소관 — iOS ADR-0001을 따른다.

## Alternatives Considered
1. **프롬프트·도구 스키마를 서버(Worker)로 이전** — 향후 후보(기각 아님, 유보). 프롬프트 핫픽스가 가능해지나, 프록시가 두꺼워지고 프롬프트 버저닝·배포를 서버에서 관리해야 한다. 품질 튜닝 빈도가 높아지면 재검토.
2. **온디바이스에 키 유지** — 기각. 디컴파일 탈취·비용 폭증. iOS ADR-0001에서 이미 폐기된 방향.
3. **프록시 없이 직접 호출** — 기각. 키 노출·한도 강제 불가.

## References
- 계승 원본: **iOS ADR-0001**(백엔드 프록시 호스팅 — Cloudflare Workers + KV), repo [`dev-etymology`](https://github.com/data-sy/dev-etymology) PR #26
- 백엔드 repo: [`devetym-proxy`](https://github.com/data-sy/devetym-proxy)(비공개)
- 설계: [`../architecture.md`](../architecture.md) §4.3

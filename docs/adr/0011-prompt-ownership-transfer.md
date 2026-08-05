# ADR 0011: 프롬프트 소유권 이전 — `commonMain` → Worker

## Status
**Proposed** (2026-08-05) — 설계 위원회 산출. **사람 비준 대기.**

근거 정본: [`../design/web-transition-design.md`](../design/web-transition-design.md) 결정 D8 · 충돌 C3.

## Context

[ADR-0004](0004-backend-proxy-boundary.md)는 **시스템 프롬프트·도구 스키마를 클라이언트(`commonMain`)에 두기로** 결정했고, 그 결정을 [ADR-0006](0006-server-cache-boundary.md) §6이 그대로 계승했다. 당시 근거는 두 클라이언트(iOS Swift·CMP)가 같은 프록시를 공유하므로 프롬프트가 `commonMain`에 있으면 **Android·iOS가 동일 프롬프트를 자동 공유**한다는 것이었다.

ADR-0004는 동시에 이렇게 유보해 두었다:

> **프롬프트를 서버로 옮기는 것은 이 ADR의 범위 밖(향후 재검토 항목).**
> *"품질 튜닝 빈도가 높아지면 재검토."*

**웹 표면 추가([ADR-0009](0009-web-framework-rendering.md))가 그 재검토 트리거를 발생시킨다.** 웹이 프록시를 직접 호출하려면 시스템 프롬프트·도구 스키마(`ClaudePrompt.kt`, 177줄)를 TypeScript로 **복제**해야 하고, 그 순간 프롬프트 정본이 2개가 된다.

이것은 웹 트랙 전체를 관통하는 원칙과 충돌한다 — **"용어 하나를 고칠 때 수정 지점을 1로 유지한다"**(설계서 §5). 프롬프트가 2벌이면 한쪽만 고쳐도 빌드는 통과하고 테스트도 통과하며, **차이는 생성된 어원 설명의 품질 차이로만 조용히 드러난다.** 앱과 웹이 같은 단어에 다른 설명을 내는 실패 모드(설계서 F3)의 직행 경로다.

## Decision

**시스템 프롬프트와 도구 스키마의 소유권을 클라이언트에서 `devetym-proxy`(Worker)로 이전한다.**

- 프롬프트 정본은 서버 1곳. 클라이언트(앱·웹)는 **질의 키워드만** 보낸다.
- 프록시가 프롬프트를 주입해 Anthropic으로 forward한다 — 키 주입과 같은 자리에서 같은 방식으로.
- 앱은 계속 자기 프롬프트를 실어 보낼 수 있고(하위 호환), 서버가 이를 무시하거나 서버 정본을 우선 적용한다. **`INV-1`(앱 코드 무변경)이 유지된다.**
- 프롬프트 버전은 기존 `promptVersion` 필드(INV-9)로 태깅해 캐시 무효화와 연동한다.

## Consequences

### Positive
- **프롬프트 정본 1개.** 앱·웹이 같은 단어에 다른 설명을 내는 경로가 구조적으로 닫힌다.
- **프롬프트 핫픽스가 가능해진다** — ADR-0004가 Negative로 기록했던 *"프롬프트 개정 시 앱 재배포 필요, 서버 핫픽스 불가"*가 해소된다.
- 웹이 얇아진다. 클라이언트가 Anthropic 본문 스키마를 알 필요가 없어져 계약 결합도 함께 낮아진다.
- 품질 튜닝 사이클이 스토어 심사 리드타임에서 분리된다.

### Negative
- 프록시가 두꺼워진다 — ADR-0004가 이 대안을 유보한 원래 이유다.
- 프롬프트 버저닝·배포를 서버에서 관리해야 한다.
- 프롬프트가 `commonMain`에 있어 얻던 것(Kotlin 타입 검사, 앱 테스트에서의 직접 검증)을 잃는다. `ClaudeApiTest`·`ClaudePrompt` 관련 계약 테스트의 재배치가 필요하다.
- **착수 시점이 열린 질문이다**(설계서 Q5): W1a에 넣으면 W1이 커지고, W2로 미루면 그 사이 웹이 프롬프트를 일시 복제해 드리프트 창이 열린다.

### Neutral
- 이 ADR은 **ADR-0004 §"Alternatives Considered 1"과 ADR-0006 §6의 유보를 해소**한다. 두 ADR을 대체(supersede)하지는 않는다 — 프롬프트 위치 결정만 갱신하고, 나머지 계약(`X-Device-Id`·429·402·tool_use 3분기)은 그대로다.

## Alternatives Considered

1. **현행 유지 — 웹이 프롬프트를 복제** — 서버 변경 0. 기각: 정본이 2개가 되고 드리프트가 조용히 샌다. 이 트랙의 "수정 지점 1" 원칙과 정면 충돌.
2. **웹이 프롬프트를 서버에서 내려받아 사용** — 정본은 1개로 유지하면서 프록시를 얇게 둔다. **유효한 절충**이며, 이전 비용이 크다고 판명되면 후퇴안으로 채택한다.
3. **웹을 앱의 프록시 계약이 아닌 별도 엔드포인트로 분리** — 표면별 프롬프트 최적화가 가능해지나 정본이 2개가 되는 문제는 그대로다. 기각.

## References
- 설계 정본: [`../design/web-transition-design.md`](../design/web-transition-design.md) D8 · C3 · Q5
- 유보 출처: [ADR-0004](0004-backend-proxy-boundary.md) Decision §3 · Alternatives 1
- 계승 확인: [ADR-0006](0006-server-cache-boundary.md) Decision §6
- 관련 ADR: [ADR-0009](0009-web-framework-rendering.md) · [ADR-0010](0010-web-abuse-prevention.md) · [ADR-0007](0007-ai-prompt-quality.md)(프롬프트 품질)
- 클라이언트 현행: `shared/src/commonMain/kotlin/com/robin/devetym/data/remote/ClaudePrompt.kt`

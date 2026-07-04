# ADR 0006: 서버 read-through 캐시 경계 — 프록시를 캐시로 확장, 캐시는 M1~M8 빌트인

## Status
Accepted (2026-07-05) — **[ADR-0004](0004-backend-proxy-boundary.md)를 대체(supersedes)**. 서버 코드는 기존 [`devetym-proxy`](https://github.com/data-sy/devetym-proxy)를 read-through 캐시로 확장.

## Context
ADR-0004는 백엔드를 **얇은 프록시**(키 주입 + 기기당 한도)로 계승했다. 그러나 AI 생성 어원 항목은 용어당 정본이 사실상 불변이라, 매 요청 Claude를 호출하는 것은 낭비다. 재사용으로 API 비용을 줄이고 응답 지연을 없애려면 서버가 생성 결과를 **캐싱해 재배달**해야 한다.

이 캐시·딜리버리 작업은 원래 별도 트랙(고유 마일스톤)으로 분리돼 있었으나, **"나중 마이그레이션·리팩토링·출시 후 없이 처음부터 빌트인"** 방침으로 전환했다. 즉 CMP 클라이언트(M1~M8)를 3계층 read-through 계약에 맞춰 **처음부터** 짓는다. 이 전환은 ADR-0004의 "프록시 계약 그대로"를 무효화하므로 새 ADR로 기록한다.

확정 아키텍처 불변식(재론 금지)은 [`../cache-delivery-milestones.md`](../cache-delivery-milestones.md) §1의 **INV-1~13**. 이 ADR은 그 중 클라이언트-서버 경계에 관한 결정을 못박는다.

## Decision
**서버(`devetym-proxy`)를 read-through 캐시로 확장하고, 클라이언트는 그 계약에 맞춰 처음부터 짓는다.**

1. **3계층 read-through (INV-1)** — 조회 순서 `클라이언트 로컬/번들 → 서버 D1 캐시 → Claude API`. API는 최후 수단이며 미스 시 생성 결과를 **D1에 write-back**한다.
2. **write-once + 멱등 read (INV-2)** — 용어당 정본 1회 생성 후 동결. read path 재생성 금지. 저장은 term key unique 제약 + `INSERT ... ON CONFLICT DO NOTHING`으로 first-write-wins (INV-4).
3. **클라이언트는 캐시에 투명 (INV-1)** — 클라 입장의 `Source`는 여전히 `BUNDLE` vs 네트워크(→`AI`). D1 히트든 갓 생성이든 서버 내부 사정이며 클라 모델을 늘리지 않는다.
4. **버전 태깅 (INV-9)** — 모든 entry가 prompt/schema 버전을 실어 선택적 무효화·재생성이 가능. 클라 `TermEntry.schemaVersion`/`promptVersion` 옵셔널 필드에 왕복 수용(M1 반영 완료).
5. **local-first pinning (INV-6·INV-11)** — 사용자가 본 항목은 기기에 저장돼 그 사용자에겐 불변. 서버 진화는 명시적 새로고침한 사용자·아직 안 본 사용자에게만. 서버는 SSOT가 아니라 freshness 담당 — 서버가 죽어도 앱은 로컬/번들로 살아있다.
6. **프롬프트·도구 스키마 위치는 ADR-0004 유지** — `commonMain` 소유, 두 클라이언트가 같은 프록시 공유. 서버 이전은 여전히 유보(향후 재검토).

**빌트인 배치(락 지점)**: M2 DB 스키마에 pinning·version 컬럼 / M3 클라를 read-through 계약에 작성 + 서버 캐시 신규 구축 / M4 Repository를 3계층으로 / M8 승격 잡·팩 동기화 메커니즘. 상세는 ROADMAP M2~M8의 🔗 항목.

## Consequences

### Positive
- **API 비용 단조 감소** — 재사용 + 릴리즈별 번들 승격 플라이휠(INV-12).
- **지연 0·오프라인 우선 유지** — 로컬/번들이 1계층, 서버는 freshness 층(INV-11).
- **리팩토링 0** — 클라를 처음부터 3계층으로 지어 나중 계약 교체·DB 마이그레이션이 없다.

### Negative
- **서버 순수 신규 구축 필요** — `devetym-proxy`에 D1 스키마·read-through·single-flight(DO)·게이트를 새로 짓는다(클라 리팩토링은 아니나 서버 작업량 실재).
- **프록시가 더 두꺼워짐** — 얇은 통로에서 캐시·동시성·게이트를 지닌 계층으로. 운영 복잡도 증가.
- 여전히 프롬프트 개정 시 앱 재배포(ADR-0004 제약 계승) — 단, 버전 태깅으로 서버측 선택적 무효화는 가능해짐.

### Neutral
- single-flight(Durable Objects)·품질 게이트(validator write-시점·critic 승격-시점)는 서버 내부 하드닝 — 클라 계약에 영향 없음(INV-7).
- 기기당 한도·spend 캡 등 남용 안전선은 ADR-0004에서 계승·확장(INV-10).

## Alternatives Considered
1. **별도 트랙 + 출시 후 캐시** (직전 방침) — 기각. 클라를 2계층으로 짓고 나중 3계층으로 갈아엎는 리팩토링·DB 마이그레이션 발생. "바로 사용" 요구와 배치됨.
2. **얇은 프록시 유지 (ADR-0004)** — 대체됨. 매 요청 Claude 호출로 비용·지연 낭비.
3. **클라이언트 로컬 캐시만, 서버 캐시 없음** — 기각. 사용자 간 재사용 불가(한 사람이 생성한 정본을 다른 사람이 못 씀) → API 비용 절감 폭 작음.

## References
- 불변식 정본: [`../cache-delivery-milestones.md`](../cache-delivery-milestones.md) §1 (INV-1~13)
- 대체 대상: [ADR-0004](0004-backend-proxy-boundary.md)
- 로컬 저장: [ADR-0003](0003-local-storage.md)(pinning 컬럼이 얹힘)
- 서버 repo: [`devetym-proxy`](https://github.com/data-sy/devetym-proxy)(비공개)
- 빌트인 배치: [`../../ROADMAP.md`](../../ROADMAP.md) M2~M8

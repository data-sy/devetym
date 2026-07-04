# ADR 0002: 코드 관용구 원칙 — 리터럴 포팅 금지, 코틀린 관용 우선

## Status
Accepted (2026-07-04)

## Context
이 앱의 설계는 동일 제품의 iOS(SwiftUI) 구현에서 **검증된 데이터 흐름과 불변식을 계승**한다([ADR-0001](0001-cross-platform-framework.md)). 계승은 리스크를 낮추지만, Swift 관용구를 문자 그대로 옮기면 **"Swift-in-Kotlin"**(코틀린 문법을 쓰지만 사고는 Swift인 코드)이 굳어진다. 특히 Swift 코드의 일부는 언어/프레임워크가 다르면 존재 이유가 사라지는 **우회 패턴**이다.

"일단 1:1로 옮기고 나중에 코틀린스럽게 리팩터"는 실패하기 쉽다 — Swift-in-Kotlin이 먼저 굳고, 뒤의 정리 패스는 거의 완결되지 않는다.

## Decision
관용구 전환을 **세 층위로 나누고 각기 다른 시점에** 처리한다. 별도의 "탈-Swift화 단계"를 두지 않는다.

1. **구조·철학 레벨 → 설계 단계에서 확정.** protocol→`interface`, `enum`→`sealed`, `@Published`→`StateFlow`, EnvironmentKey→Koin, `@MainActor`→코루틴 디스패처. (이미 [`architecture.md`](../architecture.md)에 반영됨. 리터럴 포팅이 아니라 각 언어 관용을 택한 상태.)
2. **미시·관용구 레벨 → 각 레이어 구현 시 자연 발생.** scope 함수(`let`/`apply`), null 안전(`?.`/`?:`/`requireNotNull` — `guard let` 자리), `when` 식, 컬렉션 연산자(`firstOrNull`/`map`), `copy`, 확장 함수, 후행 람다. 처음부터 코틀린으로 쓰면 흘러든다.
3. **우회 패턴 → 번역하지 말고 삭제.** Swift/SwiftData 제약 때문에만 존재하던 것은 옮기지 않는다. 그 **부재**가 코틀린다움이다:
   - `@Query` 회피 → 데이터 변경 후 **수동 재조회** ⟹ 코틀린은 `Flow`가 관용 → 규칙 자체가 소멸.
   - `@MainActor` 도배(SwiftData mainContext 제약) ⟹ 코루틴 디스패처로 대체, 대부분 불필요.
   - EnvironmentKey로 프로토콜을 주입하던 우회 ⟹ Koin에서 일반 주입.

**계승하는 것은 흐름·불변식(의미), 옮기지 않는 것은 문법·우회(형식).**

## Consequences

### Positive
- 각 레이어 구현의 **완료 정의(DoD)**에 "코틀린 관용구·철학 준수"가 들어가 리뷰 기준이 명확해진다.
- 우회 패턴이 소멸해 코드가 오히려 단순해진다(수동 재조회·@MainActor 도배 제거).

### Negative
- iOS 코드를 참조하는 사람이 "왜 이 규칙이 사라졌나"를 이해하려면 이 ADR을 읽어야 한다.
- 무엇이 "검증된 불변식"이고 무엇이 "제거 대상 우회"인지 **판단이 필요**하다(자동 아님).

### Neutral
- iOS와 CMP의 코드는 구조는 대응하되 표면은 의도적으로 다르다 — 1:1 대조를 기대하면 안 된다.

## Alternatives Considered
1. **리터럴 포팅 후 리팩터** — 기각. Swift-in-Kotlin이 고착되고 정리 패스가 미완결로 남는 전형적 실패.
2. **계보 무시·완전 재설계** — 기각. 검증된 흐름·불변식(리스크 절감 자산)을 버리게 된다.

## 판단 가이드 (계승 vs 삭제)
- **계승**: 제품 규칙(fetch 3단 순서, lazy 저장·upsert, aliases 보존, tool_use 3분기, 프록시 계약). 언어와 무관하게 참인 것.
- **삭제**: 특정 언어/프레임워크의 제약을 우회하려 생긴 것. 코틀린에서 그 제약이 없으면 규칙도 없앤다.

## References
- 관련 ADR: [ADR-0001](0001-cross-platform-framework.md)
- 설계 계보 대응표: [`../architecture.md`](../architecture.md) 부록
- 배경 시리즈: CMP 개념지도 Vol.1~2(코틀린 관용)·Vol.5(불변 상태·재구성)

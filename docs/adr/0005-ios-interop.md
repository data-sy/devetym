# ADR 0005: iOS interop 도구 — SKIE

## Status
Accepted (2026-07-04)

## Context
[ADR-0001](0001-cross-platform-framework.md)에서 Compose Multiplatform을 택했다. `shared` 코틀린
코드를 iOS/Swift로 **어떻게 노출하느냐**가 남은 결정이다([architecture §interop](../architecture.md) §285·§297).
후보 둘: **SKIE**(Touchlab) vs **Swift Export**(JetBrains 공식).

devetym의 iOS 경계 성격이 이 결정을 좌우한다:
- 경계는 **suspend `fetch` + `Flow`/`StateFlow`**(반응형, [ADR-0002](0002-code-idiom-principle.md) — 수동 재조회 없이 Flow 갱신).
  → **async/Flow 경계에서 어느 쪽이 깨끗한 Swift `async`/`AsyncSequence`를 주는가**가 핵심 기준.
- 출시할 앱이므로 **성숙도/리스크**가 편의성보다 위. experimental을 코어 경계에 얹는 비용을 경계한다.
- **버전 정렬**이 KMP 최대 실패원([ADR-0001](0001-cross-platform-framework.md) Negative, vol6 §7) — 선택한 Kotlin/CMP와 정합해야 한다.

**웹으로 2026-07 현재 사실을 확인**했다(어시스턴트 컷오프 2026-01은 이 영역에서 stale). 확인 결과는 아래 References의 URL.

### 확인된 버전 매트릭스 (2026-07, M0 골격 투입용 — 빌드로 실측 검증)

| 구성요소 | 버전 | 근거 |
|---|---|---|
| Kotlin | **2.3.21** | SKIE 상한(아래) — CMP 권장선 ≥2.2.20 충족 |
| Compose Multiplatform | **1.11.1** (stable) | compose-compatibility-and-versioning |
| Compose Compiler Gradle 플러그인 | Kotlin과 **동일 버전** (2.3.21) | 동 문서 |
| Android Gradle Plugin | **8.13.0** | Kotlin 2.3.x 지원 AGP 8.2.2–8.13.0 상한 |
| Gradle | **8.13** | Kotlin 2.3.x(7.6.3–9.0.0) ∩ AGP 8.13 |
| SKIE | **0.10.12** (2026-05) | github.com/touchlab/SKIE/releases |
| **SKIE ↔ Kotlin 지원 상한** | **2.3.21** ⚠️ | **빌드 실측**(플러그인이 2.4.0 거부) |

- ⚠️ **실측 정정**: 웹 조사에서 본 "SKIE↔Kotlin 2.0.0–2.4.0"은 낙관적/부정확했다. SKIE 0.10.12 플러그인은
  **Kotlin 2.4.0을 명시적으로 거부**하고 상한이 **2.3.21**이다(빌드 에러로 확인). 이게 정본이다.
- 그래서 Kotlin을 **2.3.21**로 고정한다. CMP 1.11.1 요구(최소 2.1.0, iOS/web 권장 2.2.20+)를 충족하고
  SKIE 상한과 정확히 맞는 최상단 지점이다. 이것이 이 ADR의 결정("interop 품질 우선")이 SKIE 채택 시 감수하는
  **Negative(Kotlin 최신판 추종 지연)의 구체적 실체**다.
- ⚠️ **Kotlin을 SKIE 상한(2.3.21) 위로 앞질러 올리지 말 것.** 2.4.x로 가려면 SKIE가 그 버전을 지원하는지
  먼저 확인하고 함께 올린다(정본은 [ROADMAP](../../ROADMAP.md) M0).

## Decision
**SKIE (표준 Obj-C export + SKIE 오버레이).**

`shared`를 iOS로 노출할 때 KMP 기본 경로인 **Obj-C export** 위에 SKIE를 Gradle 플러그인으로 얹는다.
devetym의 suspend/Flow 경계를 두 도구 다 **기술적으로는** 매핑하지만(2026-07 기준 Swift Export도 alpha에서
suspend→`async`·`Flow`→`asAsyncSequence()`를 커버하게 됨), **성숙도·타입 커버리지·경계 정합**에서 SKIE가 앞선다.

## Consequences

### Positive
- **경계 정합이 정확**하다: suspend → 진짜 Swift `async`(양방향 취소·멀티스레드), `Flow`/`StateFlow` →
  `AsyncSequence`(**제네릭 타입 정보 보존**), sealed → Swift `enum`(`onEnum(of:)`로 exhaustive switch).
  devetym의 `fetch`+반응형 상태 모델링에 그대로 들어맞는다.
- **성숙도**: experimental이 아니다. 공개 KMP 라이브러리 ~1000개 + 수백 개 수작성 테스트로 검증, 실전 프로덕션 다수.
- **KMP 기본에서 이탈이 적다**: SKIE는 기본 Obj-C export 위에 얹히는 오버레이 — 표준 경로를 벗어나지 않는다.
- **버전 정합 확인됨**: SKIE 0.10.12가 Kotlin 2.4.0을 지원, CMP 1.11.1과 함께 현재 shipping 조합.

### Negative
- **락인 성격의 아키텍처 선택**: Swift Export와 Obj-C export는 **상호배타**(한 프로젝트에서 혼용 불가).
  Swift Export로 옮기려면 노출 경로 전체를 바꿔야 한다(단, 둘 다 같은 Kotlin을 소비하므로 마이그레이션은 가능).
- **Kotlin 최신판 추종 지연**: SKIE가 새 Kotlin을 지원하기까지 수일~수주 → Kotlin 업그레이드가 SKIE에 묶인다.
- **써드파티 의존**(Touchlab) 추가. 다만 오픈소스이고 유지보수 활발.

### Neutral
- SKIE의 **Flows-in-SwiftUI·Combine**은 preview 단계 — 코어 경계(suspend·Flow·sealed)는 preview가 아니므로
  devetym 사용면에는 영향 없다. 이 두 preview 기능은 채택 대상이 아니다.
- iOS는 여전히 Obj-C 브릿지 경유(SKIE가 Obj-C export를 개선하는 방식). Swift Export의 "Obj-C 없는" 아키텍처는 미채택.

## Alternatives Considered

1. **Swift Export (JetBrains 공식)** — **기각(현시점)**. 2026-07 기준 여전히 **Alpha**이고 breaking change 예고.
   suspend→`async`·`Flow`→`AsyncSequence`는 오게 됐으나, **제네릭 미지원**(상한으로 type-erase — `Flow<도메인타입>`
   경계 ergonomics 손상), **sealed→enum 미지원**(enum class만 export), default 인자 미문서, `List/Set/Map` 상속 타입 무시 등
   구조적 제약. "출시 앱의 코어 경계"에 얹기엔 리스크가 크다. Touchlab도 이 프로필(suspend+Flow 신규 프로덕션)엔 SKIE 권고.
2. **KMP-NativeCoroutines** — 기각. suspend/Flow 브릿지는 되나 sealed→enum·default 인자·전반적 Swift ergonomics를
   SKIE만큼 커버하지 못한다. SKIE가 상위집합.
3. **순수 Obj-C export (interop 도구 없음)** — 기각. suspend가 completion handler로, `Flow`가 콜백으로 노출되어
   Swift `async`/`AsyncSequence` 경계가 서지 않는다. ADR-0002의 반응형 관용구를 iOS에서 살릴 수 없다.

## 권고가 뒤집히는 트리거
- **Swift Export가 Beta/Stable로 오면서 제네릭 + 코루틴/Flow를 프로덕션 수준으로 커버**하면 → 재평가.
  Obj-C 없는 더 깨끗한 아키텍처 이점이 실현되고, JetBrains 공식 경로로 수렴하는 편이 장기적으로 유리할 수 있다.
  (상호배타이므로 전환은 노출 경로 교체를 수반 — 경계가 얇게 유지되도록 설계.)
- **SKIE 유지보수가 정체**되거나 Kotlin 최신판 추종 지연이 릴리스를 막을 만큼 커지면 → 재고.

## References
- 확인 출처(2026-07):
  - Swift export 현황: [kotlinlang.org/docs/native-swift-export.html](https://kotlinlang.org/docs/native-swift-export.html) — Alpha, 제네릭 미지원, enum class만
  - SKIE 호환/기능: [skie.touchlab.co/intro](https://skie.touchlab.co/intro) · [skie.touchlab.co/features](https://skie.touchlab.co/features/) — Kotlin 2.0.0–2.4.0
  - Touchlab 포지셔닝: [touchlab.co/the-future-of-kmps-ios-interop](https://touchlab.co/the-future-of-kmps-ios-interop) — 상호배타·신규 프로덕션엔 SKIE 권고
  - 버전 정렬: [CMP compatibility & versioning](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html) · [Kotlin releases](https://kotlinlang.org/docs/releases.html) · [SKIE releases](https://github.com/touchlab/SKIE/releases)
- 관련 ADR: [ADR-0001](0001-cross-platform-framework.md)(CMP 결정 — 이 ADR이 그 Negative "interop 도구 선택"을 해소), [ADR-0002](0002-code-idiom-principle.md)(반응형 관용구)
- 설계: [architecture §interop](../architecture.md) · 진행 정본: [ROADMAP](../../ROADMAP.md) M0

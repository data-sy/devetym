# ADR 0001: 크로스플랫폼 프레임워크 — Compose Multiplatform

## Status
Accepted (2026-07-04)

## Context
개발 용어 어원 사전을 **Android·iOS 양쪽에** 제공하려 한다. 1인 개발 규모이고, 동일 제품이 이미 iOS(SwiftUI)로 설계·검증된 상태다 — 데이터 흐름(단방향 3층: UI → ViewModel → Repository → 소스)과 설계 불변식(`fetch` 3단, lazy 저장, tool_use 3분기, 프록시 계약)이 확정돼 있다.

제약과 요구:
- **UI·로직 이중 구현 비용을 최소화**해야 한다 (1인 유지보수).
- 이 앱의 UI는 **단순**하다(검색·상세 카드·목록 3탭) → 플랫폼 특화 UI 요구가 약하다.
- iOS **출시 경험은 자산**이다(App Store 서명·심사 익숙). 새로 배울 건 주로 Android 배포.
- Kotlin/Compose **학습 곡선은 감수 가능**하다(개념지도 시리즈로 준비됨).

## Decision
**Compose Multiplatform (UI까지 공유).** 로직뿐 아니라 화면(Composable)까지 `commonMain` 한 벌에 두고, 플랫폼별로 갈리는 건 네트워크 엔진·DB 드라이버 같은 얇은 조각(`expect`/`actual`)뿐이다.

## Consequences

### Positive
- **한 벌의 UI·로직**으로 두 플랫폼 → 1인 유지보수 비용 최소. UI가 단순해 공유 이점이 이중 유지 비용을 크게 상회한다.
- 이미 확정된 iOS 설계 흐름을 **개념 이전** 수준으로 계승 가능(재작성은 로컬 DB·UI 두 곳에 국한).
- 전 스택 Kotlin 단일 언어 → 모델·직렬화·상태를 한 언어로.

### Negative
- **배포 파이프라인은 여전히 플랫폼별**(Gradle+Play, Xcode+App Store). CMP가 지워주는 건 UI·로직 이중 구현이지 서명·심사가 아니다.
- **버전 정렬 리스크** — Kotlin ↔ CMP ↔ 플러그인 호환 조합을 맞춰야 하고, iOS 노출용 interop 도구(SKIE/Swift Export) 선택이 추가된다.
- iOS는 AOT(Kotlin/Native)라 **빌드 시간이 길다**.

### Neutral
- iOS 셸은 SwiftUI 진입점이 `ComposeView`를 호스팅하는 얇은 층으로 남는다(완전 무-SwiftUI는 아님).

## Alternatives Considered

1. **KMP-only (로직만 공유 + UI는 SwiftUI/Compose 네이티브 2벌)** — 기각. UI를 여전히 두 벌 유지해야 한다. SwiftUI 자산 재활용 이점은 있으나, 이 앱 UI가 단순해 재활용 이득 < 이중 유지 비용.
2. **Flutter** — 기각. Dart 별도 생태계로 Swift 흐름 계승 이점과 네이티브 iOS 자산 연속성을 잃는다. 위젯이 각 플랫폼 룩과 미묘하게 어긋난다.
3. **React Native** — 기각. JS 브리지·생태계 파편화, 타입 안전성이 약해 sealed 상태 모델링 등 설계 의도를 살리기 어렵다.
4. **네이티브 2벌 (SwiftUI + Jetpack Compose 각자)** — 기각. 이중 구현 비용이 최대. 1인 규모에서 두 코드베이스 동기화는 비현실적.

## 권고가 뒤집히는 트리거
- 플랫폼 특화 UI(위젯·워치·복잡한 네이티브 SDK 통합)가 **핵심 가치**가 되면 → 네이티브 재고.
- 결국 **iOS만 유지**하기로 하면 → CMP 불필요, SwiftUI 단독이 단순.
- 팀이 커지고 웹까지 한 UI로 노리면 → Flutter/웹 지향 스택 재비교.

## References
- 설계: [`../architecture.md`](../architecture.md)
- 관련 ADR: [ADR-0002](0002-code-idiom-principle.md)(관용구 원칙), [ADR-0004](0004-backend-proxy-boundary.md)(프록시 계승)
- 배경 시리즈: CMP 개념지도 Vol.0(모드 A/B)·Vol.6(이행 순서)

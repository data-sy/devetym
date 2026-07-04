# iosApp — iOS 셸

얇은 SwiftUI 셸이다(architecture §3). 화면·로직은 `:shared`(Compose)가 갖고, 여기선 `MainViewController`를
`ComposeView`로 호스팅하고 startKoin만 부른다.

## 구성
- `iosApp/iOSApp.swift` — 앱 진입. `AppModuleKt.doInitKoin()`으로 Koin 배선.
- `iosApp/ContentView.swift` — `MainViewControllerKt.MainViewController()`를 SwiftUI에 호스팅.
- `iosApp/Info.plist` — 번들 메타.

## Shared 프레임워크 (SKIE 경유, ADR-0005)
`:shared`는 iOS용 정적 프레임워크 `Shared`를 낸다. SKIE가 이 프레임워크의 Swift API를 개선한다
(suspend→async/await, Flow→AsyncSequence, sealed→enum).

**M0 iOS 컴파일 오라클**(Xcode 없이 검증):
```
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```
→ Kotlin/Native AOT + SKIE 변환이 도는지 확인. green 루프의 iOS 축.

## Xcode 프로젝트 (남은 셸 단계)
전체 앱 실행(시뮬레이터)은 `iosApp.xcodeproj`가 필요하다. 아직 커밋 안 됨 — 다음 방법 중 하나:
1. Xcode에서 iOS App 생성(Interface: SwiftUI) 후 위 `.swift`/`.plist`로 교체,
2. Build Phases에 Run Script 추가:
   ```
   cd "$SRCROOT/.."
   ./gradlew :shared:embedAndSignAppleFrameworkForXcode
   ```
   그리고 Framework Search Paths에 `$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)` 추가.

> M0 DoD는 여기까지 셸 배선을 정의하고, 실제 시뮬레이터 실행 확인은 Xcode 프로젝트 생성 후. 프레임워크 링크는 위 gradle 태스크로 자동 검증된다.

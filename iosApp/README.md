# iosApp — iOS 셸

얇은 SwiftUI 셸이다(architecture §3). 화면·로직은 `:shared`(Compose)가 갖고, 여기선 `MainViewController`를
`ComposeView`로 호스팅하고 startKoin만 부른다.

## 구성
- `iosApp/iOSApp.swift` — 앱 진입. `AppModuleKt.doInitKoin()`으로 Koin 배선.
- `iosApp/ContentView.swift` — `MainViewControllerKt.MainViewController()`를 SwiftUI에 호스팅.
- `iosApp/Info.plist` — 번들 메타. ⚠️ `CADisableMinimumFrameDurationOnPhone=true` 필수
  (Compose iOS의 고주사율 성능 가드 — 없으면 실행 즉시 abort).
- `project.yml` — Xcode 프로젝트 **정본 스펙**(XcodeGen). `iosApp.xcodeproj`는 이걸로 생성되며 repo에 함께 커밋한다.

## Shared 프레임워크 (SKIE 경유, ADR-0005)
`:shared`는 iOS용 정적 프레임워크 `Shared`를 낸다. SKIE가 이 프레임워크의 Swift API를 개선한다
(suspend→async/await, Flow→AsyncSequence, sealed→enum). Xcode 빌드 시 `project.yml`의 preBuildScript가
`./gradlew :shared:embedAndSignAppleFrameworkForXcode`를 돌려 프레임워크를 빌드·임베드한다.

## 빌드·실행 (시뮬레이터, Apple Silicon)
```bash
# (프로젝트 재생성이 필요할 때만) brew install xcodegen && xcodegen generate --spec iosApp/project.yml --project iosApp
UDID=$(xcrun simctl list devices available | grep -m1 'iPhone 16 (' | grep -oE '[0-9A-F-]{36}')
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator \
  -destination "platform=iOS Simulator,id=$UDID" -derivedDataPath iosApp/build/DerivedData \
  CODE_SIGNING_ALLOWED=NO build
xcrun simctl boot "$UDID"; open -a Simulator
xcrun simctl install "$UDID" iosApp/build/DerivedData/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch "$UDID" com.oddmuffin.devetym
```
✅ M0 검증됨: 시뮬레이터에서 공유 Compose 화면("Hello, iOS …")이 렌더된다.

> 실기기 배포(서명·프로비저닝)는 이후 단계. `.xcodeproj`는 커밋돼 있어 xcodegen 없이도 빌드된다
> (스펙을 바꿀 때만 xcodegen으로 재생성).

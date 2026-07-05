# M9 스모크 대본 — 시뮬레이터/에뮬레이터 우선 (2026-07-05)

> **성격: 티어형 게이트 지도.** 사용자가 **실기기 없이 시뮬레이터/에뮬레이터로** 최대한 닫는 방향으로 재구성.
> 이 Mac에는 **Xcode 26.6 + iPhone 시뮬 다수**가 있어 iOS는 즉시 구동 가능(아래 실증). Android 에뮬은 1회 셋업 필요.
>
> **왜 재구성했나**: `[사람]` 게이트의 대부분(첫 기동·코어 플로우·외관 전환·아이콘 렌더·온보딩·라이선스)은 **실
> 하드웨어가 아니라 앱 런타임**이 오라클이다 → **시뮬/에뮬로 닫힌다**. 진짜 실기기 전용은 **하드웨어 감각**
> (실제 메일앱 전송·앱간 클립보드·실 DPI 아이콘 선명도·햅틱)과 **외부 대면**(코드서명·심사)뿐이다.

---

## Tier 0 · `[AI]` 이미 닫힘 (자동, 사람 0)
- 4축 green(199 테스트) — 그래프 해석 로직·DB 쿼리 정확성·seam 구성·대비비·iOS 키부재 다크.
- **✅ iOS 시뮬 첫 기동 실증(2026-07-05)**: 실 앱이 iPhone 16 시뮬에서 크래시 없이 부팅, **실 `iosPlatformModule`
  그래프 해석**·온보딩 렌더 확인(스크린샷). §3-1 iOS 잔여(실-그래프 완전성)의 **첫 기동 부분이 시뮬로 닫힘**.
- **🐛 시뮬이 잡은 실 결함 2건(수정 완료)**:
  1. **iOS 앱 링크 불가** — SQLiter(네이티브 DB) cinterop 래퍼가 sqlite3 미링크로 undefined. 4축(프레임워크
     link)은 통과하나 Xcode 앱 빌드는 실패. → `project.yml`/`pbxproj` OTHER_LDFLAGS에 `-lsqlite3` 추가.
     **M0~M8이 Xcode 앱 빌드를 한 번도 안 돌려 미발견** — 시뮬 게이트의 첫 수확.
  2. **온보딩 저대비** — 온보딩이 Scaffold 이전 early-return이라 테마 배경 미도색 → 다크 기본인데 흰 배경 +
     밝은 text 토큰이 거의 안 보임. → `OnboardingScreen`에 `.background(colors.bg)` 추가. 다크 렌더 확인.

## Tier 1 · `[시뮬/에뮬]` — 이 Mac서 구동 가능 (AI 빌드·기동, 사람 탭)
> AI가 빌드·부팅·스크린샷까지 자동화. **인터랙션(탭·입력)은 사람이 시뮬 위에서** 하거나 XCUITest/idb 도입 시 자동화.

### iOS 시뮬레이터 — 재현 절차 (검증됨)
```bash
SIM=$(xcrun simctl list devices available | grep "iPhone 16 (" | grep -o "[0-9A-F-]\{36\}" | head -1)
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination "id=$SIM" -derivedDataPath iosApp/build/dd build
xcrun simctl boot "$SIM"; open -a Simulator
xcrun simctl install "$SIM" iosApp/build/dd/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch "$SIM" com.robin.devetym
xcrun simctl io "$SIM" screenshot shot.png     # 임의 시점 캡처
```
시뮬로 닫는 항목(사람이 시뮬 화면에서 탭·확인):
- ☐ 온보딩 2단계 → 완료 → 재기동 시 스킵(**영속 게이트** 실동작)
- ☐ 첫 기동 후 **메인 앱 다크 배경**(§3-3(v) 시각 확인 — Tier0서 온보딩은 이미 확인)
- ☐ 검색: 번들 히트(`mutex`)·alias(`Arne Andersson tree`→`aa-tree`)·미스→AI 경로
- ☐ 상세→북마크 토글→북마크 탭 즉시 반영→**재기동 후 유지(실 디스크 SQLite 영속, B1 디스크 잔여도 시뮬서 확인)**
- ☐ 히스토리 누적·삭제
- ☐ 외관 3모드 실전환(재구성이 색 바꾸는가)
- ☐ 라이선스 화면 OFL 실스크롤 · 스플래시
- ☐ 아이콘: 시뮬 홈스크린(스프링보드) 렌더 — [아이콘 시트](m9-icon-render-sheet.html) 대조
- ☐ VoiceOver: 시뮬 Accessibility Inspector로 라벨 훑기([접근성 대본](m9-accessibility-audit-script.md) B — 실기기보다 약하나 대부분 커버)

### Android 에뮬레이터 — 1회 셋업 후 동일 (현재 미설치)
```bash
# emulator 패키지 + 시스템이미지 설치(약 1GB 다운로드), AVD 생성
sdkmanager "emulator" "system-images;android-36;google_apis;arm64-v8a" "platforms;android-36"
avdmanager create avd -n devetym -k "system-images;android-36;google_apis;arm64-v8a"
emulator -avd devetym -no-snapshot -no-window &   # 헤드리스 부팅
adb wait-for-device
./gradlew :androidApp:installDebug
adb shell am start -n com.robin.devetym/.MainActivity
adb exec-out screencap -p > shot.png
adb logcat -d | grep -iE "devetym|AndroidRuntime|FATAL"   # 첫 기동 크래시/NoDefinitionFound 확인
```
> ⚠️ 에뮬 미설치 상태 — 셋업 원하면 지시. iOS 시뮬로 이미 크로스플랫폼 공통 로직(shared)은 대부분 커버되므로
> Android 에뮬은 Android **플랫폼 seam actual**(Intent 실 열림·PrefsStore) 실동작 확인이 주 가치(§3-1 Android는
> 이미 Robolectric JVM으로 그래프 닫음).

## Tier 2 · `[사람]` 실기기 전용 — 하드웨어 감각 (시뮬로 대체 불가)
- ☐ **메일앱 실제 전송**(시뮬엔 메일 계정 없음) · **앱간 클립보드 실붙여넣기 체감**
- ☐ iOS 공유시트 실동작 — ⚠️ 현재 `IosAppActions.share`는 no-op(백로그) → iOS는 "미동작"이 정상
- ☐ **실 DPI 아이콘 선명도**·홈스크린 실렌더(시뮬은 근사)
- ☐ 햅틱·실 제스처 나uance
- ☐ TalkBack/VoiceOver **실기기 제스처 주행**(시뮬 Inspector로 상당부분 선검증 후 잔여만)
- ☐ Dynamic Type 실반영(시뮬로도 상당부분 가능)

## Tier 3 · `[사람/외부]` — 환원 불가 (자율 금지, 지시 대기)
- ☐ **코드 서명**(실 keystore/인증서 — 시뮬 빌드는 서명 불요이나 배포는 필수) — [서명 가이드](m9-signing-upload-guide.md)
- ☐ iOS appiconset(Xcode 빌드 산물 — 상속 PNG 배치)
- ☐ **스토어 심사 제출·게시**(§7 Q5 — 사람 지시 대기)

---

## 판정
- **Tier 0 완료**(자동 + iOS 시뮬 첫 기동 실증).
- **Tier 1** = 사용자가 시뮬/에뮬서 탭 주행(iOS 즉시 / Android 셋업 후). 이게 **실기기 없이 M9 스모크의 실질 폐쇄**.
- **Tier 2·3**만 실기기/외부 잔여 — Tier 2는 하드웨어 확보 시, Tier 3는 게시 지시 시.

> **거짓 green 금지**: 시뮬은 실 하드웨어 감각(Tier 2)·외부 심사(Tier 3)를 보증하지 않는다. 그러나 앱 **런타임**
> (Tier 0·1)은 시뮬이 정직하게 닫는다 — 실제로 M8까지 놓친 링크 결함·저대비 버그를 시뮬이 잡았다.

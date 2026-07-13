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
xcrun simctl launch "$SIM" com.oddmuffin.devetym
xcrun simctl io "$SIM" screenshot shot.png     # 임의 시점 캡처
```
**✅ AI가 상태 주입으로 자율 확인(2026-07-05, 탭 불요)** — `simctl spawn defaults write`·컨테이너 DB `sqlite3` 주입:
- ✅ **온보딩 영속 게이트** — `onboarding_done=true` 주입 후 실행 → 온보딩 스킵, 메인 앱 진입(실 `UserDefaultsOnboardingStore` 읽음)
- ✅ **메인 앱 다크 렌더** — DevEtym 세리프 헤더·하단 4탭 네비·다크 Scaffold 배경(#0A0A0A)
- ✅ **외관 3모드 렌더** — `appearance_mode=1` 주입 → **라이트 테마 실렌더**(흰 배경·다크 텍스트), `=2` → 다크. §3-3(ii) `resolveDarkMode`가 실 iOS 런타임서 시각 렌더로 확인
- ✅ **실 디스크 SQLite + 반응형 쿼리→UI** — 컨테이너 DB에 searchHistory 3건 주입 → "최근 검색" FlowChip(mutex·daemon·kernel) **searchedAt DESC 렌더**. 실 `NativeSqliteDriver` 디스크 읽기 + `.asFlow()`→StateFlow→Compose 파이프라인 실동작(**B1 디스크 잔여 시뮬 폐쇄**)

**✅ 입력 주입분 완주(2026-07-13, CGEvent 자작 tap/swipe 도구 — cliclick은 mouse-up 유실로 폐기). 원장 = [시뮬 스모크 리포트](m9-ios-sim-smoke-report.md):**
- ✅ 검색 타이핑: 번들 히트(`mutex`)·alias(`Arne Andersson tree`→`aa-tree`)·미스→AI 실 프록시(`zzqxv`→NotDevTerm)
- ✅ 상세→북마크 토글→북마크 탭 즉시 반영→재기동 유지 + 어원 복사→`simctl pbpaste` 클립보드 seam actual
- ✅ 히스토리 탭·개별 삭제·실검색 타임스탬프 · 설정 외관 라이트/다크 실조작 · 라이선스 실스크롤
- ✅ 아이콘: 홈스크린 라이트/다크 렌더(이번에 AppIcon·LaunchLogo·brand 이관 배선 — 리포트 §1)
- ✅ VoiceOver: 시뮬 Inspector 감사 생략 — **실기기 VoiceOver 사용자 사인오프로 대체**(2026-07-13, Tier 2 참조)
- 🐛 발견 결함: iOS Found 상세 탈출 불가 — ✅ **수정 완료(2026-07-13**, 상시 back+재탭 pop → 이후 셸 재설계 NavContainer로 흡수), 리포트 §3

### Android 에뮬레이터 — ✅ Tier 1 완주 (2026-07-05, adb 자율 주행)
> **✅ 셋업·주행 완료** — `system-images;android-36;google_apis;arm64-v8a` AVD 부팅 후 adb `input tap/text`로 전 플로우 자율 주행.
> **🐛 첫 기동 크래시 포착·수정**(iOS `-lsqlite3`의 Android 판): `AndroidManifest`의 `.DevEtymApp`/`.MainActivity`가 실 클래스
> 패키지(`com.robin.devetym.android`)를 못 가리켜 `ClassNotFoundException` 즉사 → `.android.*`로 수정. **assembleDebug·Robolectric은
> 그래프 모듈만 검증해 셸 배선 미검출** — 실 에뮬 기동이 최종 오라클. 주행 green: 온보딩 2단계 다크·영속 · 검색 번들/alias/미스→AI
> 실 프록시 · 북마크·히스토리 영속(실 디스크 SQLite) · seam actual(메일 `ACTION_SENDTO`→Gmail·공유 chooser·평가 Play URL) ·
> 외관 3모드(시스템 OS 추종 실증) · 라이선스 OFL 스크롤 · adaptive 아이콘(`#2E5D3A`). **갭 2건**(백로그): 클립보드 dead-seam·스플래시 미배선.
> 정본 = [ROADMAP](../../ROADMAP.md)(M9). 아래 재현 절차는 이력·재실행용.

원 셋업(재현):
```bash
# emulator 패키지 + 시스템이미지 설치(약 1GB 다운로드), AVD 생성
sdkmanager "emulator" "system-images;android-36;google_apis;arm64-v8a" "platforms;android-36"
avdmanager create avd -n devetym -k "system-images;android-36;google_apis;arm64-v8a"
emulator -avd devetym -no-snapshot -no-window &   # 헤드리스 부팅
adb wait-for-device
./gradlew :androidApp:installDebug
adb shell am start -n com.oddmuffin.devetym/com.robin.devetym.android.MainActivity  # 패키지부=applicationId, 클래스부=namespace 기준 실 클래스
adb exec-out screencap -p > shot.png
adb logcat -d | grep -iE "devetym|AndroidRuntime|FATAL"   # 첫 기동 크래시/NoDefinitionFound 확인
```
> ⚠️ 에뮬 미설치 상태 — 셋업 원하면 지시. iOS 시뮬로 이미 크로스플랫폼 공통 로직(shared)은 대부분 커버되므로
> Android 에뮬은 Android **플랫폼 seam actual**(Intent 실 열림·PrefsStore) 실동작 확인이 주 가치(§3-1 Android는
> 이미 Robolectric JVM으로 그래프 닫음).

## Tier 2 · `[사람]` 실기기 전용 — 하드웨어 감각 (시뮬로 대체 불가)
- ✅ **메일 실동작**(2026-07-13 셸 재설계 — 메일 앱 열림·수신자/한글 제목 채움 확인, 실전송은 사용자 종결) · ✅ **앱간 클립보드 실붙여넣기 체감**(2026-07-13 아이폰 13 mini — 파생 피드백: 복사 범위 오인 → 셸 재설계 §2-E로 해소)
- ✅ iOS 공유시트 실동작 — **셸 재설계 스텝 1에서 `UIActivityViewController` 실구현**(구 no-op dead button 해소) + 라운드 2에서 프레젠테이션(iPhone 기본 시트)·전문 페이로드 보정 → 사용자 사인오프(2026-07-13). 정본 = [셸 재설계 체크리스트](m9-shell-redesign-device-checklist.md)
- ☐ **실 DPI 아이콘 선명도**·홈스크린 실렌더(시뮬은 근사 — 홈스크린 렌더 자체는 사용자 실사용 중 무지적)
- ☐ 햅틱·실 제스처 뉘앙스(앱에 커스텀 햅틱 없음 — 선택)
- ✅ VoiceOver **실기기 사용자 사인오프**(2026-07-13 아이폰 13 mini — "보이스오버는 됐다" 판정, Inspector 감사 생략 대체) · ☐ TalkBack(Android)은 실기기 확보 시 잔여
- ☐ Dynamic Type 실반영(시뮬로도 상당부분 가능)

## Tier 3 · `[사람/외부]` — 환원 불가 (자율 금지, 지시 대기)
- ☐ **코드 서명**(실 keystore/인증서 — 시뮬 빌드는 서명 불요이나 배포는 필수) — [서명 가이드](m9-signing-upload-guide.md)
- ☐ iOS appiconset(Xcode 빌드 산물 — 상속 PNG 배치)
- ☐ **스토어 심사 제출·게시**(§7 Q5 — 사람 지시 대기)

---

## 판정
- **Tier 0 완료**(자동 + iOS 시뮬 첫 기동 실증).
- **Tier 1 Android ✅ 완주**(adb 자율 주행, 크래시 1건 수정) · **Tier 1 iOS ✅ 완주**(2026-07-13 CGEvent 입력 주입 — [리포트](m9-ios-sim-smoke-report.md)). 시뮬/에뮬이 **실기기 없이 M9 스모크의 실질 폐쇄** + 4축이 못 잡은 **첫 기동 크래시 2건**(iOS `-lsqlite3`·Android manifest 클래스 경로) 포착.
- **Tier 2 ✅ 실질 완료**(2026-07-13 아이폰 13 mini — 셸 재설계 라운드 1·2 사인오프 + VoiceOver. [체크리스트](m9-shell-redesign-device-checklist.md)). 잔여(선택): TalkBack·Dynamic Type·실 DPI 재확인·햅틱.
- **Tier 2·3**만 실기기/외부 잔여 — Tier 2는 하드웨어 확보 시, Tier 3는 게시 지시 시.

> **거짓 green 금지**: 시뮬은 실 하드웨어 감각(Tier 2)·외부 심사(Tier 3)를 보증하지 않는다. 그러나 앱 **런타임**
> (Tier 0·1)은 시뮬이 정직하게 닫는다 — 실제로 M8까지 놓친 링크 결함·저대비 버그를 시뮬이 잡았다.

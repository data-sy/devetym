# M9 다음 세션 핸드오프 — Android 에뮬레이터 스모크 (Tier 1 Android) (2026-07-05)

> ## ✅ 완료 (2026-07-05) — 이 인계의 작업은 끝났다
> 이 핸드오프의 청크는 **주행 완료**. 결과 정본은 [ROADMAP](../../ROADMAP.md)(M9 "Tier 1 Android 에뮬 스모크 완주" 불릿).
> 요약: AVD 셋업→부팅→`installDebug`→adb 탭·타이핑으로 전 플로우 자율 주행. **첫 기동 크래시 1건 포착·수정**
> (`AndroidManifest`의 `.DevEtymApp`/`.MainActivity`가 `com.robin.devetym.android` 패키지를 못 가리켜 `ClassNotFoundException`
> 즉사 → `.android.*`로 수정, 커밋 diff). 검색 3경로·북마크/히스토리 영속·seam actual(메일/공유/평가)·외관 3모드·라이선스·
> 아이콘 전부 green. **갭 2건은 백로그로 이관**: 클립보드 dead-seam·Android 스플래시 미배선(ROADMAP 백로그 참조).
> **잔여는 실기기/외부만**(하드웨어 감각·서명·심사). 아래 원 계획은 이력 보존용.
>
> ---
>
> **성격: 다음 세션이 읽고 바로 착수하는 forward 실행 계획.** 진행 상태 정본 = [ROADMAP](../../ROADMAP.md)(M9),
> 게이트 지도 = [티어형 스모크 대본](../release/m9-device-smoke-script.md). 이 핸드오프는 **Android 에뮬 셋업+주행**
> 한 청크만 담는다(iOS 시뮬 첫 기동·Tier 1 iOS는 이전 세션서 진행).
>
> **착수법**: 새 세션에서 이 파일 + [스모크 대본](../release/m9-device-smoke-script.md) 주고 **"Android 에뮬 스모크 진행"**.
> 브랜치 `feat/m9-release-verification`. **푸시·게시 금지(지시 대기).**

## 0. 왜 새 세션인가 (이전 세션 맥락)
M9 `[AI]` 트랙(199 테스트·4축 green)·지그 5종·iOS 시뮬 첫 기동 실증은 완료(커밋 `22e3ecf`·`b8699ef`, 미푸시).
이전 세션이 iOS 시뮬로 **4축이 못 잡은 실 결함 2건**을 잡았다(참고 — Android도 유사 가능성):
1. iOS 앱 링크 불가(SQLiter sqlite3 미링크) — Android는 `assembleDebug` green이라 이 클래스는 없을 것.
2. 온보딩 저대비(Scaffold 이전 테마 배경 미도색) — **commonMain 수정이라 Android도 이미 고쳐짐**(`.background(colors.bg)`).
Android 에뮬은 독립 자동 청크라 컨텍스트 신선한 새 세션이 깔끔.

## 1. 환경 사실 (이전 세션 실측 — 재조사 불필요)
- Android SDK: `/opt/homebrew/share/android-commandlinetools` (`local.properties` `sdk.dir` 동일). `platforms/android-36`·`build-tools/35,36`·`platform-tools/adb` 존재.
- **미설치**: `emulator` 패키지·시스템 이미지·AVD 전무. `adb`는 있음(`platform-tools/adb`). `ANDROID_HOME` 미설정(gradle은 `local.properties`로 해결).
- 네트워크 OK(mavenCentral 200). Gradle 8.13, JDK 17(`/opt/homebrew/opt/openjdk@17`).
- `:androidApp:assembleDebug` green(APK 빌드됨). `applicationId=com.robin.devetym`, `versionName=0.1.0`, release 빌드타입 **signingConfig 미배선**(디버그 설치는 무관).
- ⚠️ Apple Silicon → **arm64-v8a 시스템 이미지** 필요(x86 아님).

## 2. 셋업 (자동 — sdkmanager/avdmanager, ~1GB 다운로드)
```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
SDKM="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"   # 경로 실확인(cmdline-tools 하위 구조)
yes | "$SDKM" --licenses
"$SDKM" "emulator" "system-images;android-36;google_apis;arm64-v8a" "platforms;android-36"
AVDM="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"
echo no | "$AVDM" create avd -n devetym -k "system-images;android-36;google_apis;arm64-v8a" --force
```
> ⚠️ `sdkmanager`/`avdmanager` 실제 경로 먼저 확인(`find $ANDROID_HOME -name sdkmanager`). `cmdline-tools/latest/bin` 아니면 `cmdline-tools/bin`.
> google_apis 이미지가 커서 느리면 `default` variant도 가능(Play 스토어 불필요 — 스모크엔 무관).

## 3. 부팅 + 설치 + 스모크 주행
```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
"$ANDROID_HOME/emulator/emulator" -avd devetym -no-snapshot -no-boot-anim &
"$ANDROID_HOME/platform-tools/adb" wait-for-device
# 부팅 완료 폴링
until [ "$("$ANDROID_HOME/platform-tools/adb" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 3; done
./gradlew :androidApp:installDebug
ADB="$ANDROID_HOME/platform-tools/adb"
$ADB shell am start -n com.robin.devetym/.MainActivity
sleep 4
$ADB exec-out screencap -p > /tmp/android_firstboot.png
$ADB logcat -d | grep -iE "devetym|AndroidRuntime|FATAL|NoDefinitionFound" | tail -30
```

## 4. 확인 항목 (Tier 1 Android — 스모크 대본 §Tier1 Android)
- ☐ **첫 기동 크래시 없음** — 실 `androidPlatformModule` 6 seam 해석(§3-1 Android는 Robolectric JVM으로 이미 닫았으나 실 에뮬 부팅이 최종 확인). logcat에 `NoDefinitionFound`/`FATAL` 없음.
- ☐ **온보딩 다크 렌더**(이전 세션 수정 반영 — 흰 배경 아님) → 완료 → 재기동 스킵(영속 게이트).
- ☐ 검색: 번들 히트(`mutex`)·alias(`Arne Andersson tree`→`aa-tree`)·미스→AI 경로(네트워크).
- ☐ 상세→북마크 토글→탭 즉시 반영→**재기동 후 유지(실 디스크 AndroidSqliteDriver 영속)**.
- ☐ 히스토리 누적·삭제.
- ☐ **Android seam actual 실동작**(§3-3 Robolectric은 Intent 구성만 — 실 열림은 여기): 메일 `ACTION_SENDTO` 실 열림·공유 chooser 표시·클립보드 붙여넣기·평가 스토어 URL.
- ☐ 외관 3모드 실전환(재구성이 색 바꾸는가) · 라이선스 OFL 실스크롤 · 스플래시.
- ☐ 아이콘: 에뮬 런처(홈스크린) 실렌더 — [아이콘 시트](../release/m9-icon-render-sheet.html) 대조(17엔트리 adaptive).
- ☐ (선택) TalkBack: 에뮬 설정서 켜고 [접근성 대본](../release/m9-accessibility-audit-script.md) B 훑기.

## 5. 입력 자동화 (탭 주행)
- `adb`로 **자동 주행 가능**: `adb shell input tap X Y`·`adb shell input text "mutex"`·`adb shell input keyevent`. iOS 시뮬(idb 부재로 수동)과 달리 **Android는 탭·타이핑까지 AI 자동화 가능** → 스크린샷 대조로 플로우 전체를 자율 주행할 수 있다(좌표는 `screencap` 스크린샷서 산출).
- 크래시/ANR은 `adb logcat`으로 확인(iOS보다 진단 쉬움).

## 6. 규율
- **푸시·머지·게시 금지**(지시 대기). 진행상태는 ROADMAP(디스크)에 기록.
- 에뮬로 못 닫는 것(실 메일 전송 체감·실 DPI·햅틱)은 Tier 2 실기기 잔여로 정직 이관 — **거짓 green 금지**.
- 에뮬 **teardown은 무조건 하지 말 것**(SessionEnd 훅 금지 — 측정 중 다른 세션 깨뜨림). 필요 시 조건부/사람 트리거.
- 끝나면 **운영문서 리프레시**(`/refresh-ops-docs`) — 로드맵·핸드오프·README 상태부 stale 제거.

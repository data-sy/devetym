# M9 검증 환경 — 나중에 한꺼번에 지울 것 (teardown 원장) (2026-07-05)

> **목적**: M9 실기기/시뮬/에뮬 스모크를 돌리며 **로컬 머신에 쌓인 대용량 산출물**을 한곳에 적어,
> 나중에 정리할 때 **고아 쓰레기 없이 한꺼번에** 지운다. 지금은 **전부 보존**(M9 진행 중 재실행 가능성) —
> 이 원장은 "M9 검증이 완전히 끝나 더 이상 스모크를 안 돌릴 때" 실행할 **삭제 체크리스트**다.
>
> ⚠️ 이 산출물들은 **git에 없다**(빌드/다운로드 캐시·에뮬 이미지). 코드가 아니라 **디스크 정리 대상**이다.
> ⚠️ 아래 명령은 **소급 검증**할 것 — 경로/패키지명은 머신 상태에 따라 다를 수 있다(`du -sh`로 먼저 실존 확인).

## 총량 (2026-07-05 실측)
| 분류 | 경로 | 크기 | 삭제 후 재확보 비용 |
|---|---|---|---|
| Android AVD | `~/.android/avd/devetym.avd` + `devetym.ini` | 1.7G | 초 단위 재생성(다운로드 없음) |
| Android emulator 패키지 | `$ANDROID_HOME/emulator` | 1.1G | ~1GB 재다운로드 |
| Android 시스템이미지 | `$ANDROID_HOME/system-images/android-36/google_apis/arm64-v8a` | 4.3G | 대용량 재다운로드 |
| iOS 시뮬 빌드 산출 | `iosApp/build/` (dd 포함) | 281M | `xcodebuild` 재빌드 |
| Gradle 빌드 산출 | `shared/build`·`androidApp/build`·`build` | ~835M | `./gradlew` 재빌드 |
| **합계** | | **≈ 8.2G** | |

`$ANDROID_HOME` = `/opt/homebrew/share/android-commandlinetools`

## 삭제 체크리스트 (M9 스모크 완전 종료 시 실행)

### 1) Android 에뮬 — 이번 세션에 신규 설치·생성 (최대 회수처)
```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
AVDM="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"
SDKM="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

# 에뮬 실행 중이면 먼저 종료
"$ANDROID_HOME/platform-tools/adb" emu kill 2>/dev/null

# (a) AVD 삭제 (1.7G) — ini + .avd 디렉토리 함께 정리됨
"$AVDM" delete avd -n devetym

# (b) 에뮬 패키지 삭제 (1.1G)
"$SDKM" --uninstall "emulator"

# (c) 시스템이미지 삭제 (4.3G)
"$SDKM" --uninstall "system-images;android-36;google_apis;arm64-v8a"
```
> ⛔ **`platforms;android-36`은 지우지 말 것** — gradle 빌드(`compileSdk 36`)가 사용한다. 고아 아님.
> ⛔ `platform-tools`(adb)·`build-tools`·`cmdline-tools`도 빌드 필수 — 보존.

### 2) iOS 시뮬 빌드 산출 (281M)
```bash
rm -rf /Users/owner/devetym/iosApp/build   # xcodebuild -derivedDataPath iosApp/build/dd 산출. 재빌드로 복구
```
> 시뮬레이터 자체(iPhone 16 등)는 **Xcode 기본 자산**이라 우리가 만든 게 아니다 — 지우지 않는다.
> `CoreSimulatorService` 등 XPC 데몬은 macOS launchd 관리 상시 프로세스 — 무시.

### 3) Gradle 빌드 캐시 (~835M, 선택)
```bash
cd /Users/owner/devetym && ./gradlew --stop && ./gradlew clean
# 또는 강하게: rm -rf build shared/build androidApp/build
```
> 정상 재생성 산출이라 "고아"는 아니나 디스크 압박 시 회수 가능. 다음 빌드가 다시 만든다.

## 실행 후
- 이 원장 상단에 `✅ 정리 완료 (날짜)` 배너를 달거나, ROADMAP M9에서 이 링크를 제거한다(정리 자체가 M9 종료 신호).
- 정본 상태는 [ROADMAP](../../ROADMAP.md)(M9).

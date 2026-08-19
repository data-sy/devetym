# 🤖 설정 「앱 평가하기」 무반응 수정 — App Store 리뷰 딥링크 이행

> ## ✅ 실행 완료 (2026-08-08) — **이 문서를 프롬프트로 다시 먹이지 말 것**
>
> 코드 수정은 끝났고 커밋됐다(`fix/settings-review-deeplink` `361e988`). **§0~§6은 이제 실행 지시가 아니라 경위 기록이다** — 특히 **§0을 다시 따르면 안 된다**(그 미커밋분은 이미 `aafa52d`로 닫혔고, fix 브랜치도 이미 있다).
>
> **✅ 트랙 완전 종결 (2026-08-19)** — 이 수정은 **v0.1.1로 2026-08-18 App Store 게시**됐고, 실기기 검증 PASS(iPhone 13 mini · iOS 26.5.2)·게시본 재확인·제보자 회신까지 끝났다. §9의 "남은 것"도 이제 없다.
>
> **지금 이 문서에서 볼 곳은 [§9 실행 결과](#9-실행-결과--무엇이-닫혔고-무엇이-남았나)** — 무엇이 닫혔고 무엇이 남았는지가 거기 있다. 진행 상태 정본은 [ROADMAP](../../ROADMAP.md) Now 항목.
>
> **남은 것은 사람 게이트 하나: 실기기 검증.** 그 뒤에야 버전 상향·제출을 판단한다.

**발단**: 2026-08-08 외부 유저 제보 — *"설정에서 앱 평가하기를 눌렀는데 작동을 안 해"*
**성격**: 코드 수정(iOS 라이브 앱). 웹 트랙 W와 **무관** — 별도 브랜치·별도 흐름.
**읽는 주체**: ~~콜드 세션 AI. 이 문서 하나로 §0 정리 → §5 검증까지 관통한다.~~ → **실행 종료. 아래는 경위 기록 + §9 결과.**

---

## 0. ⚠️ 먼저 — 작업대를 비운다 (코드 착수 전 필수)

착수 시점에 **웹 트랙 문서 작업이 미커밋 상태로 남아 있을 수 있다.** 빌드를 돌리려면 워킹트리가 깨끗해야 하므로 이것부터 닫는다.

```bash
git status --short
git branch --show-current   # 기대: docs/web-track-autonomy-prep
```

**미커밋분이 있으면** — 그건 **웹 설계 결정 감사**의 산출물이다(이 수정과 무관하니 섞지 않는다). 현재 브랜치에 **경로를 명시해** 커밋한다:

```bash
git add ROADMAP.md \
        "docs/handoff/🤖-26-08-05-web-design-decision-audit.md" \
        docs/handoff/26-08-05-web-design-confirmation-ledger.md
git commit -m "docs(web): 결정 감사 재개 규약(§9-0) + 확인 원장 신설 + 관측 구멍 백로그 3건"
```

> ⚠️ **`git add -A`/`git add .` 금지.** 이 핸드오프 파일 자체는 스테이징하지 않는다 — 아래 fix 브랜치에서 커밋한다(untracked 파일은 브랜치를 갈아타도 따라온다).

**워킹트리가 이미 깨끗하면** 위 커밋은 건너뛴다(사람이 먼저 닫았다는 뜻이다). 어느 경우든 다음 단계는 같다.

> 💡 커밋이 선택이 아닌 이유: 이 브랜치의 `ROADMAP.md`는 main과 13줄 다르다. 그 위에 미커밋 변경분이 있으면 **git이 `checkout main` 자체를 거부한다**(로컬 변경분 덮어쓰기 방지). 정리는 취향이 아니라 진행 조건이다.

**그다음 main에서 새 브랜치를 딴다** (웹 브랜치 위에 쌓지 않는다 — 웹 감사는 0/26 판정 대기라 언제 병합될지 모르고, 그 위에 얹으면 이 수정이 인질이 된다):

```bash
git checkout main
git checkout -b fix/settings-review-deeplink
```

**🔒 안전선 (프로젝트 규율 — 어기지 말 것)**
- **push는 사람 게이트다.** 커밋까지가 자율 범위. `git push`·PR 생성 전 반드시 사람에게 묻는다.
- **브랜치 삭제 금지** — 병합돼도 보존한다.
- 워킹트리가 지저분한 채로 빌드하지 않는다(어느 변경이 결과를 만들었는지 못 가린다).

---

## 1. 배경 — 왜 이건 "버그 재현"이 아니라 "API 오용 수정"인가

**증상은 재현 자체가 불가능할 수 있다.** 현재 경로:

```
SettingsScreen.kt:75  ActionRow("앱 평가하기") { actions.requestReview() }
  └─ iOS   IosSeams.kt:82  → iosReviewPresenter (셸 주입)
  │         └─ iOSApp.swift:17  AppStore.requestReview(in: scene)   ← StoreKit 2
  └─ Android AndroidSeams.kt:42 → Play 스토어 URL openUrl
```

iOS의 `AppStore.requestReview(in:)`는 **시스템이 조용히 삼키는 게 정상 동작**이다:

- 유저당 365일 **최대 3회** 스로틀 — 초과 시 아무 일도 안 일어나고 앱은 그걸 **알 방법이 없다**(반환값·콜백·에러 없음)
- `설정 > App Store > 앱 내 평가 및 리뷰`가 꺼져 있으면 **항상** 무반응
- 표시 여부·타이밍을 전적으로 시스템이 결정

Apple 가이드라인은 이 API를 **유저의 명시적 액션(버튼 탭)에 연결하지 말라**고 한다. "적절한 순간에 앱이 알아서 띄우는" 프롬프트용이지 메뉴 항목용이 아니다. 유저가 직접 "평가하기"를 눌렀다면 결과가 **결정적**이어야 하는데 현 구현은 구조적으로 그럴 수 없다.

**→ 따라서 과업은 "왜 안 떴는지 밝히기"가 아니라 "결정적으로 동작하는 API로 갈아타기"다.** 재현 시도에 시간 쓰지 말 것.

**이 자리는 이미 두 번 터졌다** (같은 실수의 3회차):
- 실기기 1차(2026-07-13) — `SKStoreReviewController`가 iOS 26에서 무프롬프트 no-op → StoreKit 2로 교체
- 실기기 3차 3-5 — 설정 액션 전멸(4중 결함)
- 두 번 다 **"더 새로운 프롬프트 API"로 갈아탔을 뿐 프롬프트 API라는 범주를 벗어나지 않았다.** 이번에 범주를 바꾼다.

**로그는 찾지 말 것** — 이 증상은 어디에도 안 남는다. Sentry는 uncaught crash만, `AnalyticsService`는 `PlaceholderAnalyticsService`(빈 구현), Cloudflare Worker는 AI 폴백 경로만 본다. 예외가 안 나므로 이벤트 자체가 없다.

---

## 2. 변경 설계 — 채택안과 대안

### 채택: seam은 유지하고 iOS actual만 딥링크로 교체

`AppActions.requestReview()`라는 seam은 "플랫폼에 맞는 방식으로 리뷰를 요청한다"는 뜻이므로 이름은 그대로 두고 **iOS 구현만** 바꾼다. 그러면 Android(이미 스토어 URL)와 **대칭**이 된다.

| 파일 | 변경 |
|---|---|
| `shared/src/commonMain/.../Constants.kt` | `appStoreReviewUrl` 상수 신설 — `https://apps.apple.com/app/id6790429958?action=write-review` |
| `shared/src/iosMain/.../IosSeams.kt` | `requestReview()` 본문을 `openUrl(Constants.appStoreReviewUrl)`로 교체. `iosReviewPresenter` 훅·`SKStoreReviewController` import 제거 |
| `iosApp/iosApp/iOSApp.swift` | `IosSeamsKt.iosReviewPresenter = { … }` 주입 블록 제거 (`StoreKit` import도 미사용이면 제거) |

**Apple ID `6790429958`은 실측 확정값**이다(2026-07-27 게시, ROADMAP §M9 📆 일정). `?action=write-review`가 리뷰 작성 시트를 직접 연다.

⚠️ **`foregroundWindowScene()`은 지우지 말 것** — `topPresentedViewController()`(공유·메일 폴백)가 계속 쓴다. `SKStoreReviewController` import만 제거 대상이다.

### 대안 (채택 안 함, 판단이 갈리면 사람에게 물을 것)

`requestReview()` seam을 통째로 걷어내고 `SettingsScreen`이 `openUrl(Constants.appStoreReviewUrl)`을 직접 부르는 안. 간접층이 하나 줄지만 **Android가 다른 URL을 써야 하는 사실**이 화면 코드로 새어 나온다. seam 유지가 낫다.

### Constants.kt 주석 관례

`privacyPolicyUrl`·`supportEmail`이 **왜 중앙화됐는지**(하드코딩 드리프트 방지) 주석으로 남기는 관례를 따른다. 새 상수에도 **Apple ID의 출처와 `write-review` 파라미터의 의미**를 적는다.

---

## 3. 곁가지 — 같이 처리할지 판단할 것

**`AndroidSeams.kt:50` `openUrl`에 `ActivityNotFoundException` 미처리.** 같은 파일 `sendMail`(29행)은 잡는데 `openUrl`은 안 잡아서, 브라우저·스토어 부재 기기에서 **크래시 경로**다. `requestReview`·`privacyPolicyUrl`이 전부 이 함수를 탄다.

**단 Android는 미출시다**(ROADMAP:94, F 트랙 2026-08-05 보류) — 실사용자가 0명이라 급하지 않고, `requestReview`가 여는 Play URL은 **지금 죽은 링크**다(미게시). 이 수정에 끼워 넣을지, F 재개 때 묶을지는 사람 판단. **기본 권고 = 이번엔 손대지 않는다**(스코프 오염 회피). 대신 ROADMAP 백로그에 한 줄 남긴다.

---

## 4. 테스트

기존 관례를 따른다 — **실행 동작이 아니라 순수 로직·구성값**을 잡는다(`MailtoUrlTest`가 `mailtoUrl()` 순수 함수를 잡은 것과 같은 결).

- **commonTest** — `Constants.appStoreReviewUrl` 형태 단언: `https://apps.apple.com/` 접두 · Apple ID `6790429958` 포함 · `action=write-review` 포함. 오타·ID 드리프트를 빌드 실패로 만든다.
- `UIApplication.openURL` 실호출은 테스트 범위 밖 — 실기기 오라클로 넘긴다(§5).

**축 확인**: 이 프로젝트의 green은 5축이다. 최소 아래 4축은 돌린다.

```bash
./gradlew :shared:testDebugUnitTest
./gradlew :shared:iosSimulatorArm64Test
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :androidApp:assembleDebug
```

`iOSApp.swift`를 건드리므로 **Xcode 빌드까지 돌려야 한다**(Swift 컴파일 에러는 위 4축이 못 잡는다).

---

## 5. 검증 — 오라클 규율

이 프로젝트는 마일스톤마다 *"빌드는 되는데 실기동은 깨진다"*를 겪었다(iOS `-lsqlite3` 미링크·Android manifest 클래스경로). **여기서도 green은 오라클이 아니다.**

| 층 | 무엇을 확인 | 한계 |
|---|---|---|
| 5축 green | 컴파일·링크·상수 형태 | 버튼이 실제로 뭘 여는지 모름 |
| **iOS 시뮬** | 설정 → 앱 평가하기 → **Safari로 App Store 웹페이지** | ⚠️ **시뮬레이터엔 App Store 앱이 없다** — 리뷰 시트가 아니라 Safari가 뜨는 게 정상. "URL이 살아서 열린다"까지만 증명 |
| **iOS 실기기** | **App Store 앱이 열리고 리뷰 작성 시트가 뜬다** | **← 진짜 오라클. 이것만이 제보 종결 근거다** |

실기기 검증은 사람 게이트다. 시뮬까지 자율로 밀고, 실기기 확인은 사람에게 요청한다.

---

## 6. 기록 — 코드 밖에 남길 것

- **GitHub Issue 선행** (ADR-0008 — 버그 정본은 Issues): `gh issue create`로 제보 원문·진단·수정 방향을 남기고 번호를 딴다. 커밋/PR 본문에 `Fixes #N`. *(현재 열린 이슈: #15 북마크 토글 미반영 · #16 '오픈ai' 분기 — 중복 아님, 새로 만든다.)*
- **피드백 원문 아카이브** — `docs/feedback/`에 2026-08-08 접수분으로 원문 한 줄을 남긴다. 이 창구를 만든 이유가 *"대응만 남고 원문이 증발하면 판단 근거를 못 되짚는다"*(`docs/feedback/README.md`)이므로 **수정만 하고 원문을 안 남기면 규율 위반**이다.
- **ROADMAP** — M9 이후 유지보수 항목으로 한 줄. §3을 보류했다면 백로그에도 한 줄.

---

## 7. ⚠️ 이 수정이 끝나도 제보자 문제는 안 풀린다

**main 병합 ≠ 유저 해결.** 앱이 App Store에 라이브(v0.1.0, Apple ID `6790429958`)이므로 유저에게 닿으려면 **새 버전 제출·심사·게시**가 필요하다.

### 7-1. 버전 상향 — 이건 AI가 한다 (심사 넣기로 확정된 경우)

파일 편집이라 자율 범위다. **단 사람이 "심사 넣는다"를 확정한 뒤에** 손댄다 — 확정 전이면 건너뛰고 §7-2만 보고한다.

| 파일 | 키 | 현재 → 변경 |
|---|---|---|
| `iosApp/iosApp/Info.plist` | `CFBundleShortVersionString` | `0.1.0` → `0.1.1` |
| `iosApp/iosApp/Info.plist` | `CFBundleVersion` | `2` → `3` |
| `androidApp/build.gradle.kts` | `versionName` | `0.1.0` → `0.1.1` (미출시지만 표기 동기화) |

⚠️ `CFBundleVersion`은 **ASC 업로드마다 증가해야 하는 빌드 번호**다(같은 값 재업로드는 거부). 현재 `2`인 것은 1차 업로드가 ITMS-90474로 거부돼 재업로드했기 때문 — 이력은 ROADMAP §M9 참조. `v0.1.1` 태그 부여 시점은 사람 지시를 따른다(v0.1.0은 게시가 아니라 **제출** 시점에 선행 부여했다).

### 7-2. 여기서부터는 사람·외부 게이트

아카이브 → ASC 업로드 → 심사 제출 → 심사(수일) → 수동 게시. 실행 정본은 [`docs/handoff/26-07-13-ios-submission-handoff.md`](26-07-13-ios-submission-handoff.md) §1.

세션 종료 시 사람에게 **명시적으로** 이 두 가지를 구분해 보고할 것:
1. 코드 수정 상태 (커밋됨·미푸시·버전 상향 여부)
2. 유저에게 닿기까지 남은 것 (아카이브 → 제출 → 심사 → 게시)

그리고 **제보자 회신은 별개 트랙**이다 — 다음 릴리스에 반영된다는 안내는 사람이 한다.

---

## 8. 정본 포인터

| 무엇 | 어디 |
|---|---|
| 버그·개선 트래커 | GitHub Issues (ADR-0008) — 이 건 = [#19](https://github.com/data-sy/devetym/issues/19) |
| 진행 상태 | `ROADMAP.md` |
| 피드백 원문 | `docs/feedback/` — ⚠️ **이 브랜치엔 없다**(창구가 `docs/web-track-autonomy-prep`에만 존재). 이 건 원문 = 거기 `26-08-08-외부-제보.md` |
| 플랫폼 seam 계약 | `shared/src/commonMain/.../ui/platform/AppDeps.kt` |
| 셸 재설계 경위 | `docs/specs/m9-ux-shell-redesign-draft.md` |

---

## 9. 실행 결과 — 무엇이 닫혔고 무엇이 남았나

**실행일 2026-08-08** · 커밋 `361e988` (`fix/settings-review-deeplink`, main 위) · **미푸시**

### 9-1. 닫힌 것

| 절 | 결과 |
|---|---|
| §0 작업대 | 웹 감사 미커밋분을 `aafa52d`로 닫고 main에서 fix 브랜치 분기 — 지시대로 |
| §6 Issue | **#19** 생성 (제보 원문·진단·수정 방향·오라클 기재) |
| §2 코드 | `Constants.appStoreReviewUrl` 신설 · `IosSeams.requestReview()` → `openUrl(딥링크)` · `iosReviewPresenter` 훅과 `SKStoreReviewController` import 제거 · `iOSApp.swift` 주입 블록·`StoreKit` import 제거. **채택안 그대로**(seam 유지, iOS actual만 교체) — 대안은 쓰지 않았다 |
| §4 테스트 | `shared/src/commonTest/.../AppStoreReviewUrlTest.kt` — 호스트·Apple ID·`write-review` 3단언 |
| §4 green | 4축 전부 통과 (`testDebugUnitTest` · `iosSimulatorArm64Test` · `linkDebugFrameworkIosSimulatorArm64` · `androidApp:assembleDebug`) |
| §6 피드백 원문 | 웹 브랜치에 `cdcfe52` (사유 = §8 표 각주) |
| §6 ROADMAP | Now에 유지보수 항목 + 백로그 2건 |

### 9-2. 계획과 달라진 것 3건

**① §4 Xcode 빌드 → `swiftc -typecheck`로 대체.** 환경 블로커다 — 설치된 시뮬 런타임이 iOS 18.5뿐인데 SDK는 26.5라 Xcode가 destination을 아예 못 찾는다(`Supported platforms for the buildables in the current scheme is empty`). 프레임워크 실물 대상 타입체크로 대체하고, **형식 통과가 아님을 음성대조로 확증**했다 — 제거된 `IosSeamsKt.iosReviewPresenter`를 참조하는 프로브는 실제로 컴파일 실패한다(`cannot find 'IosSeamsKt' in scope`).

**② §5 iOS 시뮬 실주행 → 포기(2026-08-12 사람 결정).** 위 블로커인데다 **시뮬엔 App Store 앱이 없어 어차피 Safari까지만** 증명한다. 런타임 다운로드(수 GB) 값이 실기기 1회보다 크지 않다.

**③ §6에 없던 stale 1건 발견·수정.** ROADMAP 백로그 「[Feature] 앱 내 평점 요청 배선」이 *"`iosReviewPresenter` 훅이 이미 배선돼 있어 호출 지점만 추가"* 라고 적고 있었다 — **이번 수정이 그 훅을 지웠으므로 좌표가 죽었다.** 갱신했다: 그 기능은 `iOSApp.swift` StoreKit 2 주입을 되살리고 **딥링크와 구분되는 새 seam**(예: `promptReview()`)으로 붙어야 한다. **기존 seam 재사용은 설정 버튼을 프롬프트로 되돌려 #19를 회귀시킨다.**

### 9-3. 남은 것

| # | 무엇 | 주체 |
|---|---|---|
| 1 | **실기기 검증 — App Store 앱이 열리고 리뷰 작성 시트가 뜨는가.** 유일·최종 오라클, 제보 종결 근거 | 사람 |
| 2 | §7-1 버전 상향 — **보류**(심사 투입 미확정, 2026-08-12). 현행 `0.1.0`/`CFBundleVersion 2` 유지 | 사람 판단 → AI 편집 |
| 3 | push·PR(`Fixes #19`) — **보류**(2026-08-12) | 사람 |
| 4 | 아카이브 → ASC 업로드 → 심사 → 게시 | 사람·외부 |
| 5 | 제보자 회신 | 사람 |
| 6 | §3 곁가지 `AndroidSeams.openUrl` `ActivityNotFoundException` — **이번에 손대지 않음**(권고대로). ROADMAP 백로그에 남김, F 트랙 재개 때 | 이월 |

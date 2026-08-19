# 🤖 #19 딥링크 수정 — 실기기 검증 → 심사 투입

> # ⛔ 역할 종료 — 이 문서로 세션을 시작하지 마시오 (2026-08-16)
>
> **§1~§5가 전부 실행됐다.** 실기기 PASS → 버전 상향(`0.1.1`/빌드 `3`) → 아카이브 → 업로드 → **심사 제출 완료.**
> 본문의 "미완"·"보류" 표기(§1 표의 ❌, §2 블로커, §4 버전 상향)는 전부 **stale**이다.
>
> **현재 상태 → [`ROADMAP.md`](ROADMAP.md) Now의 #19 항목**(정본).
> **이력 보존용으로 남긴다** — 왜 프롬프트 API를 떠났는지(§1 하단)와 이월 항목(§7)은 여전히 유효하다.

**앞 세션에서 이어받는다.** 코드는 끝났고 main에 들어갔다. 남은 건 **검증과 출하**다.

**발단**: 2026-08-08 외부 유저 제보 — *"설정에서 앱 평가하기를 눌렀는데 작동을 안 해"*
**읽는 주체**: 콜드 세션 AI. 이 문서 하나로 §1 확인 → §5 제출까지 관통한다.
**웹 트랙 W와 무관하다** — 섞지 말 것.

---

## 0. ⚠️ 가장 먼저 — 이 문서의 상태 주장을 믿지 말고 실측하라

아래 §1은 **작성 시점(2026-08-13) 스냅샷**이다. 그 사이 사람이 Xcode를 열어 플랫폼을 받았을 수도, 실기기 검증을 했을 수도 있다. **진행 상태 정본은 디스크(`ROADMAP.md` Now의 #19 항목)와 실제 명령 출력**이지 이 문단이 아니다.

```bash
git log --oneline -3                      # 기대: ea09348 Merge pull request #20
git status --short                        # 기대: 비어 있음
gh issue view 19 --json state -q .state   # 기대: CLOSED
xcodebuild -showdestinations -project iosApp/iosApp.xcodeproj -scheme iosApp 2>&1 | tail -5
```

**마지막 명령이 판단 분기점이다.**
- `Ineligible … iOS 26.5 is not installed` → **§2부터** (플랫폼 미설치, 작성 시점 상태)
- 실제 destination 목록이 뜬다 → **플랫폼이 이미 설치됐다. §2를 건너뛰고 §3으로.**

---

## 1. 지금까지 (앞 세션 결과)

| 무엇 | 상태 |
|---|---|
| 코드 수정 | ✅ `361e988` — seam 유지, iOS actual만 App Store 리뷰 딥링크로 교체 |
| 운영 문서 | ✅ `69dae7b` |
| main 병합 | ✅ [PR #20](https://github.com/data-sy/devetym/pull/20) merge-commit `ea09348` |
| 이슈 #19 | ✅ CLOSED (`Fixes #19` 자동) |
| gradle 4축 green | ✅ |
| Swift 검증 | ✅ `swiftc -typecheck` + 음성대조(제거된 심볼 참조가 실제로 컴파일 실패함) |
| **실기기 검증** | ❌ **미완 — 이번 세션의 본체** |
| 버전 상향 | ❌ 미실행 (심사 투입 확정 전이라 의도적 보류) |
| 아카이브·제출 | ❌ 미실행 |

**무엇을 고쳤나**: iOS `AppStore.requestReview(in:)`는 365일 3회 스로틀·유저 설정에 따라 **시스템이 조용히 삼키는 게 정상 동작**이라(반환값·콜백·에러 없음) 유저가 직접 누른 버튼의 구현으로는 원리상 결정적일 수 없었다. **프롬프트 API라는 범주를 벗어나** App Store 리뷰 딥링크(`Constants.appStoreReviewUrl`)로 갈아탔다. 경위·설계 정본 = [`docs/handoff/🤖-26-08-08-app-review-deeplink-fix.md`](docs/handoff/🤖-26-08-08-app-review-deeplink-fix.md) — **그 문서는 실행 완료다. 프롬프트로 다시 먹이지 말 것**(§9 실행 결과만 참조).

---

## 2. 블로커 — Xcode iOS 플랫폼 미설치

2026-08-13 실측:

```
xcodebuild … -destination 'generic/platform=iOS' archive
→ error: iOS 26.5 is not installed.
  Please download and install the platform from Xcode > Settings > Components.
```

**시뮬레이터만이 아니라 iOS 플랫폼 컴포넌트가 통째로 없다.** 그래서 **실기기 빌드도 아카이브도 둘 다 막혀 있다.** 앞 세션이 Xcode 빌드 축을 `swiftc -typecheck`로 대체한 것도 이 때문이다.

해소(둘 중 하나, **사람이 고르게 할 것**):

```bash
xcodebuild -downloadPlatform iOS      # 수 GB·수십 분. 백그라운드 실행 권장
```

또는 **Xcode > Settings > Components**에서 받는다 — 진행률이 보여 사람이 편하다.

> 💡 이걸 풀면 **실기기 검증과 아카이브가 동시에** 열린다. 하나의 블로커가 §3·§5를 같이 막고 있다.

**📍 2026-08-15 — ✅ 이 블로커는 해소됐다. §2는 이력이다.** `xcodebuild -downloadPlatform iOS`(8.52 GB) 완료 후 재실측:
- `-destination 'generic/platform=iOS' -showBuildSettings` **통과**, `SDKROOT=iPhoneOS26.5.sdk` → **아카이브 열림**(§5 진행 가능)
- 시뮬 런타임 iOS 26.5(23F77) Ready — 시뮬 기기 `kmp-test-26` 생성됨
- **시뮬 빌드·기동 성공**: `-sdk iphonesimulator`로 `BUILD SUCCEEDED` → install → launch → **온보딩 정상 표출·크래시 0**
- ⚠️ 위 기동 확인은 **셸이 산다**는 증명일 뿐 **#19의 오라클이 아니다**(시뮬에 App Store 앱 없음). §3은 그대로 유효하다.

⚠️ **시뮬 차단 원인 정정(2026-08-15 실측).** 이전 기록의 *"Xcode가 destination을 아예 못 찾음"* 은 오진단이었다. 실제로는 둘이다: ① destination 해석 실패 — 스킴 `SDKROOT=iphoneos` + 디바이스 플랫폼 미설치 → **`-sdk iphonesimulator` 명시로 우회됨**. ② 진짜 벽 — `actool`이 SDK와 맞는 시뮬 런타임을 요구(`No simulator runtime version from ["22F77"] … SDK version 23F81a`). **즉 시뮬도 같은 플랫폼 미설치가 뿌리다.**
그래도 **시뮬은 이 건의 오라클이 못 된다** — App Store 앱이 없어 Safari가 열리는 것까지만 증명한다(2026-08-12 사람 결정으로 포기). 런타임을 받는 값은 시뮬이 아니라 **§5 아카이브가 열리는 것**에 있다.

---

## 3. 실기기 검증 — 이번 세션의 본체

> **▶ 실행은 런북으로 옮겼다 (2026-08-15).** 손을 움직이는 순서·체크리스트·PASS 후 심사 투입까지 = [`🤖-26-08-15-device-verification-runbook.md`](🤖-26-08-15-device-verification-runbook.md). **새 세션은 그 문서에서 시작한다.** 아래 §3은 판정 기준의 근거로 남긴다.

**이것만이 제보 종결 근거다.** 빌드는 AI가, 탭과 판정은 사람이 한다.

**절차**: 실기기 연결 → Xcode에서 `iosApp` 스킴을 실기기 대상으로 Run → 앱에서 **설정 > 앱 평가하기** 탭.

**PASS 기준** — 아래가 **전부** 성립해야 한다:

1. **App Store 앱이 열린다** (Safari가 아니라 App Store 앱)
2. 열린 페이지가 **DevEtym(개발 어원 사전)** 이다 — 다른 앱이나 "항목을 찾을 수 없음"이 아니다
3. **리뷰 작성 시트**가 뜬다 (별점 입력 UI) — 앱 소개 페이지에서 멈추면 `?action=write-review`가 안 먹은 것이다

**FAIL이면 심사에 넣지 말 것.** 증상별 1차 의심:

| 증상 | 의심 |
|---|---|
| "항목을 찾을 수 없음" | Apple ID `6790429958` 오류 |
| 앱 소개 페이지만 열리고 리뷰 시트 없음 | `?action=write-review` 파라미터 미동작 |
| 아무 반응 없음 | `openUrl` 경로 자체 문제 — `IosSeams.kt` `openURL` 호출 확인 |

> ⚠️ **이 자리는 이미 3회차다**(실기기 1차 `SKStoreReviewController` no-op · 3차 3-5 설정 액션 전멸 · 이번 #19). 두 번 다 "고쳤다"고 판단한 뒤 실기기에서 뒤집혔다. **미검증 출하는 네 번째를 만든다** — 심사는 수일이라, 틀리면 제보자가 한 사이클을 더 기다린다.

검증 통과하면 그 사실을 **ROADMAP #19 항목에 기록**하고 §4로 간다.

---

## 4. 버전 상향 — 심사 투입이 확정된 뒤에만

파일 편집이라 AI 자율 범위지만 **사람이 "심사 넣는다"를 확정한 뒤에** 손댄다.

| 파일 | 키 | 현재 → 변경 |
|---|---|---|
| `iosApp/iosApp/Info.plist` | `CFBundleShortVersionString` | `0.1.0` → `0.1.1` |
| `iosApp/iosApp/Info.plist` | `CFBundleVersion` | `2` → `3` |
| `androidApp/build.gradle.kts` | `versionName` | `0.1.0` → `0.1.1` (미출시지만 표기 동기화) |

⚠️ **`CFBundleVersion`은 ASC 업로드마다 증가해야 하는 빌드 번호다** — 같은 값 재업로드는 거부된다. 현재 `2`인 것은 1차 업로드가 ITMS-90474로 거부돼 재업로드했기 때문(이력 = ROADMAP §M9).

`v0.1.1` 태그 부여 시점은 사람 지시를 따른다 — `v0.1.0`은 게시가 아니라 **제출** 시점에 선행 부여했다.

### 4b. 「이번 버전의 새로운 기능」 — 새 버전 레코드 필수 입력 (2026-08-15 신설)

v0.1.0(최초 출시)엔 없던 필드라 그동안 어느 문서에도 문안이 없었다. **초안 작성 완료** → 정본 = repo 밖 `~/Downloads/devetym-release/store-metadata.md` **§6**([이관 경위](docs/release/README.md)). 여기 중복해 적지 않는다. 최종 문구 확정·콘솔 입력은 **사람**이며, **실기기 PASS 전엔 확정하지 않는다**(뒤집히면 문안도 무효).

### 4c. v0.1.1 릴리스 범위 = #19 단독 (2026-08-15 사람 확정)

열린 이슈 **[#15](https://github.com/data-sy/devetym/issues/15)(북마크 토글 UI 미반영·bug)·[#16](https://github.com/data-sy/devetym/issues/16)(enhancement)은 이번 판에 태우지 않는다.** #15는 수용 기준이 self-healing 구조라 상태 전파를 건드려야 하고, 그러면 검증 표면이 #19 하나에서 둘로 늘어 제보자 대기가 더 밀린다. **번들 DB 추가(D1 캐시 승격)·웹 트랙 W도 이번 릴리스와 분리**한다(같은 이유 + 승격 잡 미구현 — §7 참조).

---

## 5. 아카이브 → 제출

**실행 정본 = [`docs/handoff/26-07-13-ios-submission-handoff.md`](docs/handoff/26-07-13-ios-submission-handoff.md) §1.** 여기 중복해 적지 않는다 — 그 문서가 정본이고 이 문단은 포인터다.

순서: 아카이브 → ASC 업로드 → 심사 제출 → 심사(수일) → **수동 게시**(자동 아님).

---

## 6. 🔒 안전선

- **브랜치 삭제 금지** — 병합돼도 보존한다(`fix/settings-review-deeplink`는 로컬·원격 둘 다 살아 있다).
- **push·PR·머지는 사람 게이트.** 앞 세션에서 사람이 명시 지시해 #20까지 나갔으나 **그건 그 건에 한정**이다. 새 커밋은 다시 물어라.
- **워킹트리가 지저분한 채로 빌드하지 않는다.**
- `docs/specs/`·ADR·architecture·거버넌스 문서는 **자동 수정 금지** — 고칠 게 보이면 제안하고 승인받는다.

---

## 7. 이월된 것 (이번 세션 범위 밖 — 주울지는 판단)

- **`docs/specs/spec.md:298` stale** — `iOS=StoreKit requestReview`라 적혀 있으나 이제 딥링크다. **데이터면이라 승인 필요**. 앞 세션에서 제안만 하고 안 고쳤다.
- **웹 브랜치 `docs/web-track-autonomy-prep`** — 〔2026-08-16 정정: **원격에 있다**(로컬·원격 tip `cdcfe52` 일치). 종전 "로컬 전용·미푸시" 서술은 stale이었다.〕 거기에 **제보 원문 아카이브**(`docs/feedback/26-08-08-외부-제보.md`)가 들어 있어, 병합 전까지 원문이 main에 없다(이건 여전히 유효). 병합 시점은 웹 트랙 감사(0/26 판정 대기)와 묶인 별개 판단.
- **`AndroidSeams.openUrl`의 `ActivityNotFoundException` 미처리**(크래시 경로) — Android 미출시라 보류, ROADMAP 백로그. F 트랙 재개 때.
- **번들 DB 추가 확장 = D1 캐시 승격 (2026-08-15 조사·백로그 확정)** — 디스크에 대기 중인 큐레이션 데이터는 **없다**(db-expand round-001~004 150건 **전부 이미 번들에 반영**, 650/650 종결). 실재하는 축적분은 **AI 폴백이 D1에 write-back한 항목**이고, 이를 `terms.json`으로 올리는 게 **INV-12 승격 잡**인데 **미구현**이다(S1 = 캐시 M0+M1까지만, 승격 잡 = 캐시 M5 후속 슬라이스). 지금 하려면 수동 대행: wrangler로 D1 export → **`normalizeKeyword` 키 공간 정합 확인**(스펙이 명시 경고하는 드리프트 지점 — 어긋나면 승격분이 영영 조회 안 됨) → validator·critic 게이트 → `Scripts/db-expand/merge.py`. ⚠️ 로컬에 **wrangler 미설치**(프록시 repo = `~/devetym-proxy`). **이번 릴리스와 분리**(§4c).
- **백로그 「[Feature] 앱 내 평점 요청 배선」** — 이 수정이 `iosReviewPresenter` 훅을 지웠으므로, 그 기능은 Swift 주입을 되살리고 **딥링크와 구분되는 새 seam**(예: `promptReview()`)으로 붙어야 한다. **기존 seam 재사용은 #19를 회귀시킨다.**

---

## 8. ⚠️ 심사 통과해도 제보자 문제는 그때 풀린다

**병합 ≠ 해결. 심사 승인 ≠ 해결.** 앱이 라이브(v0.1.0)라 유저에게 닿는 건 **게시(수동)** 시점이다. 그리고 **제보자 회신은 별개 트랙** — 다음 릴리스에 반영된다는 안내는 사람이 한다.

세션 종료 시 사람에게 **이 둘을 구분해** 보고할 것:
1. 코드·검증 상태 (실기기 PASS 여부·버전 상향·아카이브)
2. 유저에게 닿기까지 남은 것 (제출 → 심사 → 게시 → 회신)

---

## 9. 정본 포인터

| 무엇 | 어디 |
|---|---|
| 진행 상태 | `ROADMAP.md` Now의 #19 항목 |
| 이 수정의 경위·설계 | `docs/handoff/🤖-26-08-08-app-review-deeplink-fix.md` (**실행 완료 — 재실행 금지**, §9만) |
| 제출 실행 절차 | `docs/handoff/26-07-13-ios-submission-handoff.md` §1 |
| 버그·개선 트래커 | GitHub Issues (ADR-0008) — 이 건 = [#19](https://github.com/data-sy/devetym/issues/19) (CLOSED) |
| 피드백 원문 | `docs/feedback/` — ⚠️ main에 없다(§7) |
| 플랫폼 seam 계약 | `shared/src/commonMain/.../ui/platform/AppDeps.kt` |

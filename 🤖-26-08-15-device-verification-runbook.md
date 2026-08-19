# 🤖 실기기 검증 런북 — #19 딥링크 → v0.1.1 심사 투입

> # ⛔ 역할 종료 — 이 문서로 세션을 시작하지 마시오 (2026-08-16)
>
> **§1~§4를 끝까지 실행했다.** 실기기 PASS → 버전 상향 → 아카이브 → 업로드 → **심사 제출 완료.**
> 아래 미체크 박스는 **전부 소진된 이력**이다. 다시 실행하지 말 것.
>
> **현재 상태 → [`ROADMAP.md`](ROADMAP.md) Now의 #19 항목**(정본). 잔여 = 심사 결과 대기 → 수동 게시 → 제보자 회신, 전부 사람 몫.
> **리젝이 오면** 이 런북이 아니라 ROADMAP에서 현행 상태를 먼저 확인할 것.
>
> **실행 중 얻은 것(이력)**: 무선 설치가 된다(`devicectl` 터널 — USB 케이블 불요, 기기 잠금 해제 필수) · Xcode GUI 빌드는 JDK 함정이 있었다(`eaf7055`로 해소) · §8의 후속 작업(`devetym-release` 정리)은 **아직 미착수**.

**성격**: 실행 런북. 오늘 밤 실기기 앞에서 위에서 아래로 그대로 따라간다.
**진입**: 새 세션에서 *"`🤖-26-08-15-device-verification-runbook.md` 읽고 실기기 검증 진행하자"*.
**맥락·경위는 여기 없다** — [`🤖-26-08-13-ios-review-submission-handoff.md`](🤖-26-08-13-ios-review-submission-handoff.md)가 맥락 정본, `ROADMAP.md` Now가 상태 정본. 이 문서는 **손을 움직이는 순서**만 담는다.

**웹 트랙 W·번들 승격은 이번 범위 밖이다** — 섞지 말 것(릴리스 범위 = #19 단독).

---

## §0. 시작 전 실측 — 이 문서의 상태 주장을 믿지 마라

```bash
git -C ~/devetym log --oneline -3     # 기대: c36d196 · d6e97ff · ea09348
git -C ~/devetym status --short       # 기대: 비어 있음
```

**환경은 2026-08-15에 이미 해소됐다**(플랫폼 8.52 GB 설치 완료). 확인만 한다:

```bash
xcodebuild -project ~/devetym/iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'generic/platform=iOS' -showBuildSettings 2>&1 | grep SDKROOT | head -1
# 기대: SDKROOT = …/iPhoneOS26.5.sdk    ← 이게 나오면 아카이브까지 열려 있다
```

이게 실패하면 환경이 되돌아간 것이다 — 08-13 핸드오프 §2로 갈 것.

---

## §1. 기기 준비 `[사람]`

- [ ] 아이폰을 케이블로 연결 (**충전 상태로 두면 된다 — 완충 후 분리 불요**)
- [ ] 기기에서 «이 컴퓨터를 신뢰하시겠습니까?» → 신뢰
- [ ] 기기 잠금 해제 상태 유지
- [ ] **기기 iOS 버전을 적어둔다** (설정 > 일반 > 정보). 결과 기록에 남긴다 — 이 결함군은 iOS 26에서 처음 관측됐다

```bash
xcrun devicectl list devices      # 연결 확인
```

> ### ⚠️ 먼저 알고 시작할 것 — 라이브 앱이 대체된다
> 개발 빌드의 번들 ID가 App Store 판과 **같다**(`com.oddmuffin.devetym`). 설치하면 기기의 **라이브 앱이 개발 빌드로 덮인다.**
> - 검증이 끝나면 **삭제 후 App Store에서 다시 받으면** 원상 복구된다.
> - 북마크·히스토리가 기기에 있다면 덮이는 과정에서 날아갈 수 있다. 아깝다면 **검증 전에 확인**할 것.
> - 이게 부담이면 대안은 없다 — 이 검증은 실기기 단독 오라클이다(08-13 핸드오프 §3).

---

## §2. 빌드·설치 `[AI + 사람]`

**권장 = Xcode GUI.** 서명·프로비저닝을 Xcode가 알아서 처리한다.

- [ ] `~/devetym/iosApp/iosApp.xcodeproj` 열기
- [ ] 상단 destination을 **연결된 실기기**로 선택
- [ ] `Signing & Capabilities` → Team 설정돼 있는지 확인 (미설정이면 사람이 선택)
- [ ] **Run (⌘R)**
- [ ] 기기에서 «신뢰되지 않은 개발자» 뜨면 → 설정 > 일반 > VPN 및 기기 관리 > 개발자 앱 신뢰

CLI 대안(서명이 이미 잡혀 있을 때만):

```bash
cd ~/devetym
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination 'platform=iOS,name=<기기이름>' build
xcrun devicectl device install app --device <UDID> <빌드된 .app 경로>
```

- [ ] **앱이 기동한다** (온보딩 또는 검색 화면). 크래시 시 → §5

---

## §3. 판정 — 이번 검증의 본체 `[사람]`

**절차**: 앱에서 **설정 > 앱 평가하기** 탭.

### PASS 기준 — 아래가 전부 성립해야 한다

- [ ] ① **App Store 앱**이 열린다 (Safari가 아니다)
- [ ] ② 열린 페이지가 **「개발 어원 사전」**이다
- [ ] ③ **리뷰 작성 시트**가 뜬다 (별점 입력 UI)

> **②는 2026-08-15에 사전 확인됨** — `https://apps.apple.com/app/id6790429958?action=write-review`가 200 + 「개발 어원 사전」으로 해석된다. 즉 「항목을 찾을 수 없음」 실패 모드는 이미 제거됐다. **실기기에서 새로 판정되는 것은 ①과 ③이다.**

### FAIL 시 증상별 1차 의심

| 증상 | 의심 |
|---|---|
| Safari가 열린다 | 유니버설 링크 미동작 — `itms-apps://` 스킴 전환 검토 |
| 앱 소개 페이지만 뜨고 리뷰 시트 없음 | `?action=write-review` 미동작 |
| 아무 반응 없음 | `openURL` 경로 자체 — `IosSeams.kt:85 openUrl` 확인. **제보 원문과 같은 증상 = 4회차** |

- [ ] **결과를 `ROADMAP.md` #19 항목에 기록** (PASS/FAIL + 기기 iOS 버전 + 시각)

**FAIL이면 §4로 가지 않는다. 심사에 넣지 않는다.**

---

## §4. PASS일 때만 — 심사 투입 `[AI가 1~2, 사람이 3~5]`

### 4-1. 버전 상향 `[AI]`

| 파일 | 키 | 변경 |
|---|---|---|
| `iosApp/iosApp/Info.plist` | `CFBundleShortVersionString` | `0.1.0` → `0.1.1` |
| `iosApp/iosApp/Info.plist` | `CFBundleVersion` | `2` → `3` |
| `androidApp/build.gradle.kts` | `versionName` | `0.1.0` → `0.1.1` (미출시·표기 동기화) |

⚠️ `CFBundleVersion`을 안 올리면 **업로드가 거부된다**(현재 `2`는 1차 업로드 ITMS-90474 거부 후 재업로드 값).

### 4-2. What's New 확정 `[사람]`

초안 정본 = `~/Downloads/devetym-release/store-metadata.md` **§6**. 현재 초안:

> 설정의 「앱 평가하기」가 눌러도 아무 반응이 없던 문제를 고쳤습니다.
> 이제 App Store의 리뷰 작성 화면으로 바로 이동합니다.
>
> 불편을 알려주신 분께 감사드립니다.

- [ ] 문구 확정 (그대로 써도 되고 고쳐도 된다)

### 4-3. 아카이브 → 업로드 → 제출 `[사람]`

- [ ] 아카이브 (환경 열려 있음 — §0에서 확인한 그것)
- [ ] App Store Connect 업로드
- [ ] 새 버전 레코드 `0.1.1` 생성 → What's New 입력
- [ ] 심사 노트·스크린샷·메타는 **재입력 불요**(v0.1.0 값 유지)
- [ ] 심사 제출
- [ ] `v0.1.1` 태그 (사람 지시 시점에)

### 4-4. 제출 후

- [ ] ROADMAP #19 상태 갱신 (제출 완료·심사 대기)
- [ ] **게시는 수동이다** — 승인돼도 사람이 게시 버튼을 눌러야 유저에게 닿는다
- [ ] **제보자 회신은 별개 트랙(사람)**

---

## §5. 실패 처리

- 앱이 기동 못 함 → 서명·프로비저닝 우선 의심. 그다음 첫 기동 크래시(과거 iOS `-lsqlite3` 링크 갭 계열)
- 리뷰 딥링크 FAIL → §3 표로 1차 분류 후 **이슈로 남기고 멈춘다.** 그 자리에서 고쳐 바로 넣지 않는다 — 이 자리는 이미 3회차이고, 두 번 다 "고쳤다" 판단 직후 뒤집혔다

---

## §6. 🔒 안전선

- **push·PR·머지·심사 제출·게시·태그는 사람 지시로만.** 브랜치 삭제 금지(병합돼도 보존)
- 워킹트리가 지저분한 채로 빌드하지 않는다
- `docs/specs/`·ADR·architecture·거버넌스 문서는 자동 수정 금지 — 제안하고 승인받는다
- **릴리스 범위 = #19 단독.** 열린 이슈 #15·#16, 번들 승격, 웹 트랙 W는 태우지 않는다

---

## §7. 정본 포인터

| 무엇 | 어디 |
|---|---|
| 진행 상태 | `ROADMAP.md` Now의 #19 항목 |
| 맥락·경위·이월 항목 | [`🤖-26-08-13-ios-review-submission-handoff.md`](🤖-26-08-13-ios-review-submission-handoff.md) |
| 이 수정의 설계 | `docs/handoff/🤖-26-08-08-app-review-deeplink-fix.md` §9 (**실행 완료 — 재실행 금지**) |
| 스토어 메타·What's New | `~/Downloads/devetym-release/store-metadata.md` (repo 밖 — [경위](docs/release/README.md)) |
| 버그 트래커 | GitHub Issues — 이 건 [#19](https://github.com/data-sy/devetym/issues/19) (CLOSED) |

---

## §8. 다음 작업 (이 런북 종료 후)

**`~/Downloads/devetym-release` 정리** — 사람 승인 완료(2026-08-15), 순서 확정:

1. 폴더 전체 스냅샷 백업 (**git 밖이라 삭제가 되돌려지지 않는다** — 9개 파일이 git에 없음)
2. 하위 폴더 분류(`활성/` `종료/` `참고/`) + 종료 문서에 상태 배너
3. **삭제 후보는 목록으로 뽑아 승인받고** 실행

정돈 헌장 = `docs/handoff/26-07-14-doc-pruning-backlog.md` §0. ⚠️ 단 그 헌장의 *"git 이력이 보존하므로 삭제는 손실이 아니다"* 전제는 **이 폴더엔 절반만 성립한다**(2026-07-26 repo 밖 이관).

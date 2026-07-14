# M9 빌드(제출 아카이브) 착수 전 준비 체크리스트

> 노션 복붙용. 2026-07-14 repo 실측 기준 (git 상태·pbxproj·Info.plist 대조 완료).
> **정산(2026-07-14 저녁, 비용 트랙 세션)**: 실제 상태와 재대조 — 비용 트랙 착지 완료로 A-1의 미커밋 3건(ROADMAP·docs/cost) 해소, C-2·C-4 검증 통과 체크. 최대 blocker는 여전히 A-2(`fix/m9-iphone-only` 병합, main 대비 +8커밋).

**전제** — 여기서 "빌드" = **iOS 제출 아카이브**(출시 시퀀스 E: Xcode Archive → App Store Connect 업로드, main 기준·`v0.1.0`). Android AAB(F 트랙)는 서명 미배선(Blocker #5)·Play 계정 미확인(Blocker #6)이라 이 체크리스트 범위 밖.

- **목표**: main의 최종 커밋으로 iOS 아카이브를 빌드해 ASC에 재업로드 가능한 상태(ITMS-90474 해소분 포함)에 도달한다.
- **지금 상태**: 스크린샷 5컷×2사이즈·메타데이터는 ASC 업로드 완료. 직전 업로드가 **ITMS-90474로 거부** → 해소 커밋(`TARGETED_DEVICE_FAMILY=1`)은 `fix/m9-iphone-only`에 있고 **main 미머지** (main은 아직 `"1,2"`). main 작업트리에 미커밋 변경 + 미추적 파일 있음. 다른 세션들이 작업 중.
- **착수 경계**: Xcode Product → Archive 실행 (사람+AI 협업).
- **관련 파일**: `docs/release/m9-signing-upload-guide.md`(절차 정본) · `docs/release/LAUNCH-CHECKLIST.md` · `ROADMAP.md` M9 · `iosApp/iosApp/Info.plist` · `iosApp/iosApp.xcodeproj/project.pbxproj`
- **등장 주체**: 나(AI), 사용자(사람), 다른 세션들(작업 중), App Store Connect(외부)

태그: **[AI]** = 내가 실행 / **[사람]** = 사용자만 가능 / **[사람→AI]** = 사람이 열어주면 AI가 실행

### A. 다른 세션 착지 — 코드 정본 확정 (아카이브는 main 기준이므로 전부 main에 모여야 함)

- [ ] **[사람]** 다른 세션들 작업 완료 선언 — 어떤 세션이 어떤 파일/브랜치를 만지고 있는지 알려주기. *(정산: 비용 트랙 세션은 완료·전부 커밋(`21ee58f`) — ROADMAP·docs/cost 미커밋분 해소. fresh-eyes 문서·png도 작업트리에서 사라짐(m9 세션이 착지한 듯). 남은 미커밋 = pbxproj 노이즈 + 이 체크리스트 파일뿐. 사람 선언만 잔여)*
- [x] **[사람→AI]** `fix/m9-iphone-only` → main 병합 (PR) — *(완료 2026-07-14: PR #12 MERGED, 로컬 병합·충돌 2건 해소 — ROADMAP 양쪽 항목 보존·pbxproj는 fix 쪽 서명 TargetAttributes(DevelopmentTeam) 채택)*
- [x] **[AI]** 병합 후 main pbxproj에 `TARGETED_DEVICE_FAMILY = 1` 반영 확인 *(완료: Debug·Release 2군데 모두 `= 1`)*

### B. 작업트리 정리·동기화 (아카이브 기준 커밋을 깨끗하게)

- [x] **[AI]** 미커밋 변경 커밋 또는 정리 *(완료: pbxproj 노이즈 `4a78357`로 착지. 병합 시 TargetAttributes 충돌 1건 발생 — fix 쪽(DevelopmentTeam 서명 정보) 채택으로 해소)*
- [x] **[사람]** docs/ 스크린샷 png 3장 처리 결정 — ~~커밋할 자산인지 임시 파일인지~~ *(정산: 작업트리에 더 이상 없음 — m9 세션이 처리 완료, 항목 해소)*
- [x] **[AI]** `git status` clean + main == origin/main 동기화 확인 *(완료: 사람 승인 하 푸시 — 병합 커밋 `65dc11e`까지 origin 동기화)*
- [x] **[AI]** 브랜치 보존 규율 확인 — *(완료: `fix/m9-iphone-only` 로컬·원격 모두 보존, 삭제 안 함)*

### C. 빌드 입력 검증 (버전·서명·green)

- [x] **[AI]** `CFBundleVersion` 1 → **2** 증가 *(완료 2026-07-14: Info.plist 직접 편집 — INFOPLIST_FILE 직접 참조 정본 확인)*
- [x] **[AI]** `CFBundleShortVersionString` = 0.1.0 확인 *(정산 2026-07-14 저녁 재확인 OK)*
- [x] **[AI]** 최종 main에서 5축 green 재실행 *(완료 2026-07-14: 병합 커밋 위에서 unit·native·link·assemble·guard 일괄 BUILD SUCCESSFUL)*
- [x] **[AI]** 릴리즈 런타임 설정 확인 — Info.plist `SentryDsn` 존재 · 프록시 endpoint가 prod인지 (LAUNCH-CHECKLIST §4) *(정산: `SentryDsn` 키 존재 ✓, 값은 `$(SENTRY_DSN)`이고 빌드 설정 기본값 `""` → 크래시 리포팅 no-op. 실 DSN 주입(예: `xcodebuild SENTRY_DSN=…`)은 아카이브 시 사람 결정 — LAUNCH-CHECKLIST Blocker #7 잔여와 동일 항목. 프록시 endpoint ✓ `Constants.kt` = `devetym-proxy.data-sy-2.workers.dev` — 오늘 라이브 스모크 통과한 prod URL과 동일)*
- [ ] **[사람]** Xcode Signing & Capabilities — Team 선택·자동 서명 동작 확인 (Apple Developer 결제 완료 상태) *(참고: 병합으로 pbxproj에 DevelopmentTeam `4H79F9W5AQ`·자동 서명이 이미 박혀 있음 — Xcode에서 열어 확인만)*
- [ ] **[사람]** **Sentry DSN 발급(신규 결정 2026-07-14 — 주입 출시 확정)**: sentry.io 무료 플랜 계정·프로젝트 생성 → DSN을 루트 `.env`에 `SENTRY_DSN=…`으로 추가 → 아카이브 시 빌드 세팅 주입(절차는 AI가 안내)

### D. 승인 게이트 (← 사람 결정 지점)

- [ ] **[사람]** "이 main 커밋으로 아카이브" 비준 — 이후 업로드는 외부 대면(1회성, ASC에 올라가면 회수 불가는 아니나 빌드번호 소진)
- [ ] **[사람→AI]** 비준 후 함께 Archive 진행 (게시 시 `v0.1.0` 태그는 별도 — 게시는 사람 지시 대기)

### ▶ 경계: Xcode Product → Archive

여기부터가 본 작업(아카이브 → Distribute → ASC 업로드 → 처리 통과 확인) — 이 체크리스트 범위 밖.

---

**가장 먼저 막힌 의존성**: A-1 — 다른 세션들의 작업 완료 선언. 특히 `fix/m9-iphone-only` 병합 없이는 빌드 자체가 무의미(재거부 확정). 내가 지금 바로 도울 수 있는 것:

- [ ] **[AI]** 병합 상태를 주기적으로 모니터링하다가 착지되면 B~C의 [AI] 항목 자동 진행
- [x] **[AI]** 기다리는 동안 C 항목 중 병합과 무관한 것(SentryDsn·프록시 endpoint 확인) 선실행 *(정산: C-2·C-4 완료 — 위 참조)*

# Handoff — E · iOS 심사 제출 (M9 마지막 구간)

> **작성**: 2026-07-13 밤. **진입**: 새 세션에서 **"docs/handoff/26-07-13-ios-submission-handoff.md 읽고 E 제출 진행하자"**.
> **상태 정본**: [ROADMAP M9](../../ROADMAP.md) + [iOS 출시 대시보드](../release/ios-launch-dashboard.html)(`checked` 속성이 정본). 충돌 시 정본 우선.

## 0. 세션 운영 — 전문가는 하린 리드 (패널 재소집 불필요)

D1~D9 결정 세션의 4인 패널은 **판단**용이었고 전건 확정됐다([결정 로그](../release/ios-launch-decision-prompt.md)). E는 **실행** 구간이라 패널 전체는 과함 — 다음 축소 편성으로 운영한다:

- **🚢 하린(iOS 릴리즈 엔지니어) = 세션 리드.** 페르소나 정의는 [결정 프롬프트 §전문가 패널](../release/ios-launch-decision-prompt.md) 재사용. 아카이브·코드 서명·수출 규정 신고·버전 정책·콘솔 문항 해석이 영역. 콘솔에서 예상 밖 문항/옵션이 나오면 하린이 옵션+추천으로 즉석 브리핑(웹 최신 확인 후).
- **⚖️ 리안(App Review 대응) = 예비 소환.** 심사 노트 입력 검수, 그리고 **리젝 발생 시** 대응 리드.
- 확정된 결정(D1~D9)을 재논의하지 않는다 — 뒤집을 신규 사실이 나오면 결정 로그에 추기하고 사용자 승인 후 변경.

## 1. 현재 위치 (2026-07-13 밤)

- **A~D 완료**: public 전환 · Pages 방침 URL 라이브 · 실기기 사인오프 · **iOS 스크린샷 캡처+프레이밍 완료**.
- **D1~D9 결정 전건 확정** — 콘솔에 넣을 값이 전부 준비됨(아래 §2).
- **스토어 식별자 교체 완료(2026-07-13 밤)**: 공개 ID(iOS 번들 ID·Android applicationId) = **`com.oddmuffin.devetym`**, 코드 네임스페이스·Kotlin 패키지는 `com.robin.devetym` 유지(이원 설계 — AGP 공식 지원). 독립 블라인드 감사 2회 PASS(APK badging·showBuildSettings·xcodegen 재생성 실측). prd.md까지 사용자 승인으로 갱신 완료.
- **수출 규정(b5) 완료**: Info.plist `ITSAppUsesNonExemptEncryption=false` — HTTPS만 사용(면제), 업로드 후 암호화 문답 생략.
- 작업 브랜치 `feat/m9-store-submission`(main 위 스택). **⚠️ "제출 아카이브는 main 기준 빌드" 전제 갱신**: 식별자·수출규정 커밋(33520ea·c1b037f)이 feat에 얹혀 있어 **아카이브 전 feat→main 병합 필수**(push=사람 지시). 병합 전 main을 빌드하면 옛 식별자로 나간다.
- 시뮬 검증 환경 보존 중(teardown 금지 — [원장](../release/m9-verification-teardown-ledger.md)). 시뮬·실기기의 기존 설치본은 옛 ID(`com.robin.devetym`) — 새 ID는 별개 앱으로 설치되므로 다음 스모크 전 옛 앱 `simctl uninstall` 권장.

## 2. 콘솔 입력물 (전부 확정 — 복붙 가능)

| 입력 | 정본 위치 | 확정 결정 |
|---|---|---|
| 앱 이름·부제 | [메타 초안 부록 A](../release/m9-store-metadata-draft.md) | D3: 「개발 어원 사전」 + 「프로그래밍 용어의 유래와 작명 이유」 |
| 키워드 (95자) | 메타 초안 §2 | D4 |
| 전체 설명(후크형)·프로모션 텍스트 | 메타 초안 부록 A("후크형 전체 설명")·§2 | D5 |
| App Privacy 라벨 | 메타 초안 §3-3 (Data Not Linked: 검색어·기기ID·크래시 / Tracking 없음·ATT 불필요) | 방침 정합 |
| 심사 노트 (영문 전문) | **메타 초안 §5** | D9 |
| 스크린샷 | raw `~/devetym-shots/ios/` · **프레임드 6컷 `~/devetym-shots/ios/framed/`** (1320×2868). 제안 순서: f1 후크→f2 오프라인→f3 AI→f4 깊이→f5 북마크→f6 라이트 | D1. 순서는 업로드 시 사람 확정 |
| 지역 | **한국 단독** | D2 |
| 가격·게시 | **무료 + 수동 게시(Manually release)** | D6 |
| 연령 등급 questionnaire | **정직 응답**(무검수 AI 생성 있음 + 도메인 제한·일일 한도·제보 경로) → 산출 등급 수용(13+ 예상). 신 체계 4+/9+/13+/16+/18+ | D7 |
| 프리뷰 영상 | 없음(스킵) | D8 |
| 지원 이메일·방침 URL | oddmuffinstudio@gmail.com · https://data-sy.github.io/devetym/privacy-policy | 라이브 확인됨 |
| 번들 ID · SKU | 코드 정본(project.yml·pbxproj·build.gradle.kts) | **`com.oddmuffin.devetym`**(07-13 교체, 구 com.robin.devetym) · SKU 제안 `devetym` |

## 3. 실행 순서 (대시보드 섹션 2 → 4 → 5)

1. **콘솔 메타 입력 `[사람]`** — ⓪ 번들 ID가 신규라 developer portal Identifiers에 App ID `com.oddmuffin.devetym` 등록(Explicit, Capabilities 기본값 — 드롭다운에 없을 때) → App Store Connect 앱 레코드 생성(iOS·한국어·SKU `devetym`) → §2 표의 값 입력. AI는 화면별 입력값을 옆에서 대준다.
2. **빌드·서명 `[사람+AI]`** — [서명·업로드 가이드](../release/m9-signing-upload-guide.md) 절차. 체크: ⓪ **feat→main 병합**(식별자·수출규정 커밋 — push=사람) ① **main 기준** 빌드 ② iOS appiconset Xcode 확정(자산 이관됨 — 빌드에서 최종 확인) ③ **실 Sentry DSN 주입**(Info.plist `SentryDsn` — [메타 초안 §4](../release/m9-store-metadata-draft.md) 체크) ④ 버전 0.1.0 / 빌드 번호 ⑤ 아카이브 → 업로드(수출 규정 문답은 Info.plist 키로 생략됨).
3. **제출 전 최종 대조 `[AI]`** — 라벨↔방침↔설명 3자 정합 재확인(§2 표), 심사 노트 붙여넣기 확인(리안).
4. **심사 제출 `[사람 지시로만]`** → 승인 후 **수동 게시 버튼도 사람** → 게시 시점에 `v0.1.0` 태그(핫픽스 런북 전제).
5. **리젝 시** — 리안 리드로 사유 분석 → 대응은 결정 로그에 추기.

## 4. 안전선 (요약 — 정본은 CLAUDE.md·ROADMAP)

- **심사 제출·게시·push·태그는 사람 지시로만.** 브랜치 삭제 금지.
- 개인정보 문구는 방침 "현재 미수집(크래시 진단·AI 폴백 검색어 제외)" 정합 유지 — 콘솔 라벨 입력 시 §3-3에서 벗어나지 않는다.
- 스토어 정책·콘솔 문항 등 외부 사실은 브리핑 전 웹 최신 확인.
- 완료 항목은 대시보드 `checked` 갱신 + 커밋.

## 5. 파일 지도

| 파일 | 역할 |
|---|---|
| [ios-launch-dashboard.html](../release/ios-launch-dashboard.html) | 실행 체크리스트 정본 |
| [m9-store-metadata-draft.md](../release/m9-store-metadata-draft.md) | 콘솔 입력값 정본(§1·§2·§3·§5) |
| [m9-signing-upload-guide.md](../release/m9-signing-upload-guide.md) | 서명·아카이브·업로드 절차 |
| [ios-launch-decision-prompt.md](../release/ios-launch-decision-prompt.md) | D1~D9 결정 로그 + 페르소나 정의 |
| [m9-screenshot-caption-jig.html](../release/m9-screenshot-caption-jig.html) | 캡션 수정 시 프레임드 재생성 지그 |
| [LAUNCH-CHECKLIST.md](../release/LAUNCH-CHECKLIST.md) | 6카테고리 배경·근거 |

# WU-6 · DevEtym/ 네이티브 iOS 전수 스윕 — 파일별 이관/폐기 원장 (2026-07-10)

> **WU-6 실행 산출물.** 원본 `~/dev-etymology`(SwiftUI iOS 앱 + 문서·스크립트·CI·하네스)를 devetym(KMP)과 **파일별 대조**해, 아직 안 넘어온 로직/자산을 발굴하고 이관/폐기를 판정한다.
> 계획 원장 = [`26-07-10-selfcontained-migration-plan.md`](26-07-10-selfcontained-migration-plan.md) §2 WU-6. 상태 정본 = [ROADMAP](../../ROADMAP.md).
> 방법: 5개 슬라이스(Models·Services / Utils·Config / Features·UI / Assets / Config·거버넌스·Docs)를 병렬 read-only 대조 → 종합.

---

## 0. 결론 — 자기완결성 최종 점검 (WU-6 step 6의 크럭스)

**✅ devetym는 dev-etymology에 런타임·빌드·CI·자산 의존이 0이다.** `grep -rn "dev-etymology"`를 전 코드/CI/자산 확장자 + `site/` + `Scripts/`에 돌린 결과:

- 커플링(빌드/런타임에 읽힘) = **0건**. `site/`·`Scripts/` 히트 0.
- 남은 문자열 히트 2건은 **순수 출처 주석**(빌드·런타임 미참조):
  - `shared/…/Constants.kt:4` — "값은 iOS 검증본(`~/dev-etymology`) 계승" (프로버넌스)
  - `shared/…/data/remote/ClaudePrompt.kt:19` — "프롬프트·3도구 스키마는 iOS 검증본 계승" (프로버넌스)
- 프록시 base URL = `https://devetym-proxy.data-sy-2.workers.dev`(devetym 인프라, 정상). dev-etymology 인프라 참조 없음.
- 문서(ADR/specs/release/design)의 `~/dev-etymology` 언급은 전부 "iOS 검증본을 계승했다"는 역사·출처 기록 — 정당.

→ **WU-7(원본 repo 폐기)의 전제조건("잔여 이관 0·의존 0")은 코드/빌드/CI/자산 면에서 충족.** 남은 것은 아래 §2·§3의 UX/거버넌스 판단 항목뿐(폐기와 무관하거나 사람 결정).

---

## 1. 이번 세션 실행분 (WU-6 [AI] 이관·수정)

| # | 조치 | 파일 | 성격 |
|---|------|------|------|
| 1 | **🐛 지원 이메일 버그 수정** — 인앱 문의/오류제보가 `data.sy.2@gmail.com`(개인)로 감. 방침(site/)·이용약관·스토어 메타·런치 체크리스트는 **전부** `oddmuffinstudio@gmail.com`. 공개 법무·스토어 문서가 약속한 주소와 어긋남 → 사용자가 다른 주소로 문의. | `Constants.kt`(신규 `supportEmail` 중앙화), `ui/screens/SettingsScreen.kt`×2, `ui/AppRoot.kt`×1 | 정합성 버그 |
| 2 | **LICENSE 이관** — devetym에 LICENSE 파일 부재(공개 repo 대상). 원본 MIT(© 2026 Lee Soyeon) 그대로 복사. | `LICENSE`(신규) | 누락 이관 |

두 조치 모두 5축 green으로 검증(회귀 0). 이메일은 `Constants.supportEmail` 한 곳으로 중앙화해 재드리프트 차단(원본 `AppConfig` 패턴 계승).

---

## 2. 파일별 대조 판정 (원장)

판정: **MIGRATED**(로직/값 devetym에 존재) · **PARTIAL**(일부 미이관·명시) · **NEEDS-MIGRATION**(부재·이관 권고) · **DISCARD**(폐기·사유) · **PROPOSE-GATED**(거버넌스/데이터면 — 개별 승인 필요).

### 2.1 Models
| 원본 | 판정 | 근거 |
|------|------|------|
| `Models/AIErrorResponse.swift` | DISCARD | 원본서 dead code(참조 0). 실제 에러 분류는 `tool_use` 3분기 → KMP `ClaudeDto.toTermResult()`가 처리. |
| `Models/SearchHistory.swift` | MIGRATED | `searchHistory` 테이블(PK dedup + upsert). |
| `Models/Term.swift` | MIGRATED | `term` 테이블(+ `seenAt`/`schemaVersion` 상위집합), `TermMapper`. |
| `Models/TermEntry.swift` | MIGRATED | `model/TermEntry.kt` 6필드 + 옵셔널 버전. |
| `Models/TermResult.swift` | MIGRATED | `sealed interface TermResult`(+ 타입드 `Source`). |

### 2.2 Services
| 원본 | 판정 | 근거 |
|------|------|------|
| `Services/AnalyticsService.swift` | DISCARD(결정상 유보) | Firebase 구현. devetym는 미수집(instanceId=null). |
| `Services/AnalyticsServiceProtocol.swift` | DISCARD(결정상 유보) | KMP `AnalyticsService` 인터페이스 존재·`logError` 배선. 성공경로 `logSearchResult`는 미호출(수집 유보). |
| `Services/BundleDBService.swift` | MIGRATED | `BundleDbSource`(정규화 인덱스·first-wins). |
| `Services/ClaudeAPIError.swift` | MIGRATED | 4 에러케이스 → `ClaudeException` 1:1. |
| `Services/ClaudeAPIService.swift` | MIGRATED | 요청바디·시스템프롬프트·응답파싱 계승. keyword 대소문자는 **의도적 개선**(원본은 소문자화, KMP는 원형 유지·키만 폴딩). |
| `Services/TermService.swift` | MIGRATED | 3계층 오케스트레이션 → `TermRepositoryImpl`(+ refresh·Mutex·clamp·pinning). |
| `Services/TermServiceProtocol.swift` | MIGRATED | 전 메서드 `TermRepository` 대응(+ reactive Flow). |

### 2.3 Utils / Config
| 원본 | 판정 | 근거 |
|------|------|------|
| `Utils/AppConfig.swift` | PARTIAL → **§1-1로 수정** | `supportEmail` 드리프트 버그 수정·중앙화. `privacyPolicyURL`은 devetym 값 사용(구 repo 경로 미계승). |
| `Utils/CategoryDisplay.swift` | PARTIAL(§3 후보) | 정본 6집합은 `Category.kt` 계승. **이중언어 표시 라벨**(`동시성 · Concurrency`)은 미이관 — 배지가 한글 원문만 렌더. UX 판단. |
| `Utils/Constants.swift` | MIGRATED | `Constants.kt`에 모델·프록시·타임아웃·디바운스·한도 동일값. |
| `Utils/DeviceIdentifier.swift` | MIGRATED | `DeviceIdProvider` seam(iOS/Android). 저장키 `proxyDeviceId`→`device_id`(신규설치·무손실). |
| `Utils/EnvironmentKeys.swift` | DISCARD | SwiftUI DI 배관 → Koin 대체. |
| `Utils/PreviewSupport.swift` | DISCARD | `#if DEBUG` Preview 도구. |
| `Utils/Theme.swift` | MIGRATED | `theme/*.kt` 11토큰·하이브리드 폰트·21 타이포. |
| `Utils/TypographyModifiers.swift` | PARTIAL(시각 천장) | 역할·폰트·크기·굵기 계승. letterSpacing/lineHeight는 "green 미보증 시각 근사"(실기기 검증). |
| `Config.sample.xcconfig` | DISCARD | 빈 플레이스홀더(앱에 키 없음·프록시 모델). |
| `Info.plist` | PARTIAL(플랫폼 매니페스트) | `UIAppFonts` 7종 → `Res.font.*` 등록. 표시명·런치스크린은 플랫폼 매니페스트(§2.5·§3). |

### 2.4 Features / UI
| 원본 | 판정 | 근거 |
|------|------|------|
| `App/ContentView.swift` | MIGRATED | `AppRoot.kt` 4탭 셸·온보딩 게이트·외관모드. |
| `App/DevEtymApp.swift` | MIGRATED | 진입점 + DI → `AppRoot(deps)` + Koin. |
| `Features/Bookmark/*` | MIGRATED | `BookmarkScreen` + reactive VM. |
| `Features/Detail/DetailView·DetailViewModel` | PARTIAL(§3 후보) | 상태·배지·북마크·공유·복사 존재. **미이관: 3단계 타임드 로딩 메시지 + 안티플리커 최소표시 딜레이**(`LoadingPhase`/`minimumLoadingNanoseconds`) — KMP는 단일 정적 로딩 문구. |
| `Features/History/HistoryView·VM` | PARTIAL(§3 후보) | 목록·상대시간·개별삭제·전체삭제 존재. **미이관: 전체삭제 확인 다이얼로그** — KMP는 즉시 파괴적 삭제. |
| `Features/Onboarding/OnboardingView.swift` | PARTIAL(§3 후보·거버넌스) | 2단계 존재. **미이관: (a) 동의 페이지 방침 링크 (b) 동의 Boolean 미배선**(devetym 미수집이라 gate 대상 없음 — 방침 정합 문제). |
| `Features/Search/*` | MIGRATED | 디바운스·최근칩·빈상태·오타치환. 제안행 요약 생략(경미). |
| `Features/Settings/SettingsView.swift` | PARTIAL(§3 후보·거버넌스) | 외관·버전·문의·평가·오류제보·동의·방침·라이선스 존재. **미이관: (a) 빌드번호 행 (b) "내 식별자 보기"(App Instance ID + 클립보드) 화면** — instanceId seam이 dead(소비 UI 없음). PIPA 데이터삭제요청 경로 — WU-4/방침 연계. |
| `Features/Debug/TypographyDebugView.swift` | DISCARD | `#if DEBUG` 개발용 타이포 플레이그라운드. |

### 2.5 Assets
| 원본 | 판정 | 근거 |
|------|------|------|
| Theme/*.colorset (11 실토큰) | MIGRATED | `AppColors.kt` 11토큰 light+dark **hex 완전 일치**(드리프트 0). |
| AccentColor.colorset | DISCARD | Xcode 기본 빈 플레이스홀더(컬러 컴포넌트 없음). |
| 폰트 7 .ttf + OFL 3 .txt | MIGRATED | `composeResources/font` + `files/ofl_*`. |
| terms.json | MIGRATED | 원본과 byte-identical(7236줄). |
| Android 런처아이콘 + 스플래시 | MIGRATED | mipmap-* + adaptive + `Theme.DevEtym.Starting`(brand `#2E5D3A`). |
| **AppIcon.appiconset (iOS)** | NEEDS-MIGRATION(§3·M9) | KMP `iosApp`에 Assets.xcassets/AppIcon **전무** → iOS 기본 빈 아이콘. ROADMAP M9 "iOS appiconset·Xcode 축 밖" 게이트와 동일. |
| **AppIcon icon-dark.png (다크변형)** | NEEDS-MIGRATION(§3·M9) | 다크 앱아이콘 변형 어디에도 없음. |
| **LaunchLogo + iOS UILaunchScreen** | NEEDS-MIGRATION(§3·M9) | iOS `Info.plist` `UILaunchScreen=<dict/>`(빈값). 원본 brand-bg+LaunchLogo 미재현. |

### 2.6 Config / 거버넌스 / Docs
| 원본 | 판정 | 근거 |
|------|------|------|
| `.claude/agents/ios-debug-senior.md` | DISCARD(재작성) | 전면 iOS/Xcode/simctl 특화. KMP엔 재작성 필요(복사 무의미). |
| `.claude/agents/ios-ux-design.md` | DISCARD(재작성) | iOS 18 SwiftUI·iPhone 특정. |
| `.claude/commands/{commit,commit-staged,pr}.md` | PROPOSE-GATED | 재사용가능하나 "NEVER Co-Authored-By"가 devetym 규약(Co-Authored-By 필수)과 **정면충돌** → 맹목 복사 금지·조정 후 승인. |
| `.claude/settings.local.json` | DISCARD | xcodebuild·구 repo 절대경로 허용목록. 재생성. |
| `CLAUDE.md`(프로젝트) | PROPOSE-GATED(거버넌스) | devetym에 프로젝트 CLAUDE.md 없음. 원본은 100% iOS → **신규 저술**(이관 아님)·승인 필요. |
| `LICENSE` | **→ §1-2 이관 완료** | MIT 그대로 복사. |
| `README.md` | MIGRATED(그린필드) | devetym 자체 CMP README. |
| `docs/README.md`(인덱스) | NEEDS-MIGRATION(경미) | devetym `docs/` 최상위 인덱스 부재. 저우선. |
| `GoogleService-Info.plist` | DISCARD | **라이브 시크릿 포함**(API_KEY 등). 미수집 정합 → 정당 부재. 절대 이관 금지. |
| `.github/workflows/pages.yml` | MIGRATED | byte-identical. 원본에 다른 워크플로 없음. |
| `.gitignore` | MIGRATED(적응) | KMP/Gradle용 재작성. Config.xcconfig/Firebase 룰 불요(해당 자산 없음). `local.properties` ignore·미추적 확인·시크릿 노출 0. |
| `docs/adr/0001` | MIGRATED(재구성) | ADR-0004(프록시)+ADR-0006(캐시)로 계승. 추가 편집 승인게이트. |
| `docs/product/prd.md` | MIGRATED(자체) | devetym 자체 prd. 라인diff 백필은 승인게이트(product). |
| `docs/specs/spec.md` | MIGRATED(분산) | spec.md + m1~m9 슬라이스로 분산. 승인게이트. |
| `docs/design/*`(non-icon) | 대부분 DISCARD | wireframe·mockup html = iOS 일회성 탐색. `typography-review.md`·`design-followup.md`는 근거 재사용 여지(선택 제안). |
| `docs/ai-quality/`·`docs/db-expand/`·`docs/launch-prep*`·`e2e-checklist.md`·`docs/design/icon` | MIGRATED | WU-3·WU-5 및 선행 이관 완료. |

---

## 3. 잔여 — 개별 승인·판단 대기 (이번 세션 미실행)

WU-6는 "이관 필요분 실이관 + specs/ADR/거버넌스는 개별 승인"이 규율. 아래는 자동 실행하지 않고 사용자 결정에 올린다.

### 3.1 UX 패리티 포트 (순수 [AI]·저위험·선택 스코프)
원본에 있고 devetym에 없는 **행위**. 각기 5축 green으로 포트 가능하나 "자기완결성"이 아닌 폴리시 개선이라 스코프 확인 후 배치.
- **Detail 3단계 타임드 로딩 + 안티플리커 딜레이** — 체감 지연 완화.
- **History 전체삭제 확인 다이얼로그** — 파괴적 동작 안전장치.
- **CategoryDisplay 이중언어 배지 라벨**(`동시성 · Concurrency`) — 디자인 판단.
- **Settings 빌드번호 행**(경미·빌드번호 seam 필요) / **Search 제안행 요약**(순코스메틱).

### 3.2 프라이버시·거버넌스 얽힘 (자동 이관 금지 — 승인/연계)
- **온보딩 동의 배선 + Settings "내 식별자 보기"(App Instance ID) 화면** — PIPA/방침 약속면. devetym는 현재 **미수집**(instanceId=null·Firebase 없음)이라 "표시할 식별자"도 "gate할 동의 대상"도 없음. → **WU-4(Sentry 크래시 도입·방침 갱신) 결정과 함께** 판단해야 정합. 지금 단독 포트 시 방침과 어긋남.
- **인앱 방침/라이선스 URL 정합** — 인앱은 `devetym.app/privacy`·`/licenses`인데 `site/` 퍼머링크는 `/privacy-policy/`, `_config.yml`에 url/baseurl/CNAME 없음·도메인 미설정 → 인앱 링크 404 위험. **WU-1(Pages 실배포)에서 실제 배포 URL 확정 시 인앱 값과 동기화**해야 함(WU-1 DoD "방침 URL을 site 링크에 반영"과 정합).
- **`.claude/commands/{commit,pr}` · 프로젝트 `CLAUDE.md`** — 거버넌스면. 신규 저술/조정 후 승인. commit 규약 충돌(Co-Authored-By) 반드시 반영.
- **ADR/product/specs 라인 백필** — 데이터/거버넌스면. 필요 시 개별 제안.

### 3.3 iOS Xcode 자산 (M9 게이트와 중첩)
- **iOS AppIcon.appiconset(전무)·다크 아이콘 변형·UILaunchScreen(빈값)** — 파일 복사+`project.yml` 배선은 [AI] 가능하나 검증이 Xcode 빌드(4축 밖)라 **M9 실기기/Xcode 게이트**로 귀속. ROADMAP M9 "iOS appiconset — Xcode 축 밖" 항목과 동일. WU-6 발굴로 원장에 명시, 실배선은 M9 트랙에서.

### 3.4 경미
- `docs/README.md` 인덱스 부재(저우선) · DeviceId 저장키 rename(신규설치 무손실) · analytics consent 키 문자열 미중앙화(no-op이라 저위험).

---

## 4. WU-7(원본 폐기) 준비 상태

- **코드/빌드/CI/자산 의존 = 0** (§0). 이 축에서 폐기 가능.
- 단 §3의 판단 항목 중 **원본에만 있는 자산/근거**(iOS appiconset PNG·icon-dark·LaunchLogo·typography-review·design-followup·`.claude` 원본)는 폐기 전에 **devetym로 뽑아둘지 결정**해야 유실 없음. → WU-7은 §3.2·§3.3 처리 후 사람 최종 확인(계획 §2 WU-7 의존과 정합).

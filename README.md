# 개발 어원 사전 (DevEtym) — Compose Multiplatform

개발 용어의 **어원과 작명 이유**를 한국어로 설명하는 사전 앱.
단순히 뜻을 알려주는 게 아니라 *왜 그 이름이 붙었는지*를 설명해 개념 이해와 기억을 돕는다.

**Android · iOS 단일 코드베이스** — Kotlin Multiplatform 위에서 UI까지 Compose Multiplatform으로 공유한다.

> 🎉 **iOS 출시 — [App Store에서 보기](https://apps.apple.com/kr/app/id6790429958)** (2026-07-27 최초 게시 · **현재 라이브 v0.1.1**, 2026-08-18). Android는 후행 트랙.

- 앱 표시 이름: **개발 어원 사전**
- 애플리케이션 ID / 번들 ID: `com.oddmuffin.devetym` (코드 네임스페이스·Kotlin 패키지는 `com.robin.devetym` 유지)
- 타깃: Android 8.0+ (API 26), iOS 16+
- 리브랜딩 후보: `Rootly` (추후 검토)

---

## 왜 Compose Multiplatform인가

한 사람이 만드는 작은 사전 앱에서 **화면과 로직을 두 번 쓰지 않기 위해서**다. 검색·북마크·히스토리·AI 폴백 같은 로직뿐 아니라 화면(Composable)까지 `commonMain` 한 곳에 두고, 플랫폼별로 갈리는 건 네트워크 엔진과 DB 드라이버 같은 얇은 조각뿐이다.

- **로직 공유(KMP)** — 네트워크·로컬 저장·ViewModel을 공유
- **UI 공유(CMP)** — Compose로 Android/iOS 화면을 함께 그림
- **플랫폼 조각만 분리** — 엔진(OkHttp / Darwin), DB 드라이버 등은 `expect`/`actual`

결정 근거는 [`docs/adr/`](docs/adr/)에 남긴다.

---

## 기술 스택

| 영역 | 선택 |
|---|---|
| 언어 | Kotlin (Multiplatform) |
| UI | Compose Multiplatform |
| 상태 | ViewModel + `StateFlow` (단방향 데이터 흐름) |
| 네트워킹 | Ktor Client + `kotlinx.serialization` (엔진: Android=OkHttp, iOS=Darwin) |
| 로컬 저장 | **SQLDelight** 2.3.2 (히스토리·북마크·AI 캐시) — [ADR-0003 확정](docs/adr/0003-local-storage.md) |
| DI | Koin (`module`/`single`/`viewModel`) |
| 큐레이션 DB | 앱 번들 내 JSON (`terms.json`, 650개) |
| AI 폴백 | Claude (Cloudflare Worker 프록시 경유, 기기당 일 10회) |

> **백엔드 계약은 앱과 분리돼 있다.** 클라이언트는 `devetym-proxy`(Cloudflare Worker)를 거쳐 Claude에 닿는다. 서버 계약은 플랫폼과 무관하게 그대로다.

---

## 아키텍처 한눈에

의존은 **한 방향으로만** 흐르고, 거의 전부가 `commonMain`에 있다.

```
Compose UI            # @Composable · 상태를 그림
│  관찰 (collectAsState)
▼
ViewModel             # StateFlow<UiState> 노출
│  호출
▼
Repository            # 소스 조율 · 캐시 정책
│            ╲
▼             ▼
Ktor(원격)        DB(로컬)     # 엔진·드라이버만 플랫폼별 (expect/actual)
```

핵심 데이터 흐름: **번들 DB(즉답) → 로컬 캐시 → 서버 D1 캐시 → AI 폴백(온라인)**.
서버 캐시 층은 별도 repo [`devetym-proxy`](https://github.com/data-sy/devetym-proxy)가 담당하며
**클라에는 투명**하다(앱 코드 무변경 — INV-1). 자세한 건 [아키텍처 설계서](docs/architecture.md) 참고.

---

## 문서

이 repo는 **문서 → 구현** 순서로 채워 나간다. **M0~M8 구현 완료**(코드 레벨) → **M9(검증·출시)** — iOS 게시로 DoD 폐쇄, 잔여는 Android 트랙.

| 위치 | 내용 | 상태 |
|---|---|---|
| [`docs/product/prd.md`](docs/product/prd.md) | 제품 기획 — 문제·타겟·유저 스토리·콘텐츠 (*왜*의 정본) | ✅ |
| [`docs/architecture.md`](docs/architecture.md) | 아키텍처 설계 — 레이어링·Ktor·로컬 저장·Koin (기술 *어떻게*) | ✅ |
| [`docs/adr/`](docs/adr/) | 돌이킬 수 없는 결정 기록 (0001~0008: CMP·관용구 원칙·로컬 DB·프록시 경계·SKIE interop·서버 캐시 경계·AI 프롬프트 품질·이슈 트래킹 / **0009~0011: 웹 프레임워크·웹 남용 방지·프롬프트 소유권 — ✅ 비준 2026-08-25** / **0012~0013: 콘텐츠 정본 D1 승격·웹 라우트 계약 — ✅ 비준 2026-08-25**) | ✅ |
| [`docs/design/web-transition-design.md`](docs/design/web-transition-design.md) | **웹 이행 설계 정본** — 렌더링·이식 판정·남용 위협 모델·범위·실패 모드 (진행 상태는 ROADMAP W 트랙) | 🚧 구현 중 |
| [`web/`](web/README.md) | **웹 표면 구현** — Astro + Cloudflare Workers. 기반(W0a·W0b) 완료, 용어 페이지·검색·AI는 미착수 | 🚧 |
| [`docs/cache-delivery-milestones.md`](docs/cache-delivery-milestones.md) | 캐시·딜리버리 불변식(INV-1~13)·마일스톤 정본 — 서버 트랙의 제약 | ✅ |
| [`docs/specs/spec.md`](docs/specs/spec.md) | 화면·동작 구현 명세 (Phase 1~4, Claude Code 전용) | ✅ |
| [`ROADMAP.md`](ROADMAP.md) | 이행 순서(코어 먼저, UI 나중) + **진행 상태 정본** | ✅ |
| [`docs/cost/`](docs/cost/) | 비용 — **[운영 비용 원장](docs/cost/running-costs.md)**(실제 나가는 돈·갱신일) · API 비용 결정 문서 · Console 설정 스냅샷 로그 (`Scripts/cost/report.py`가 리포트 도구) | ✅ |

---

## 빌드 · green 루프

검증 오라클(**네 축 모두 통과해야 green**). 버전은 [`gradle/libs.versions.toml`](gradle/libs.versions.toml) 한 곳에서 관리
(Kotlin 2.3.21 · CMP 1.11.1 · AGP 8.13.0 · Gradle 8.13 · SKIE 0.10.12 — [ADR-0005](docs/adr/0005-ios-interop.md)).

```bash
./gradlew :shared:testDebugUnitTest                      # 공유 로직 + Robolectric(실 Android 그래프·seam) — androidUnitTest
./gradlew :androidApp:assembleDebug                      # Android APK
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64    # iOS 프레임워크(Kotlin/Native + SKIE)
./gradlew :shared:iosSimulatorArm64Test                  # commonTest + iosTest(네이티브 DB·NSUserDefaults) 네이티브 실행
```

iOS 앱 **시뮬레이터 실 구동**(Apple Silicon)은 M9서 실증 — `xcodebuild -scheme iosApp -sdk iphonesimulator … build` + `simctl boot/install/launch`
(⚠️ 앱 링크에 `-lsqlite3` 필요 — SQLiter cinterop). 상세는 [ROADMAP](ROADMAP.md) Done의 M9 항목.

> ✅ **이 경로는 열려 있다 (2026-08-16 실측).** 2026-08-12~15에 막혀 있었으나 `xcodebuild -downloadPlatform iOS`(8.52 GB)로 해소됐다.
> 현재 시뮬 런타임 **iOS 18.5 + 26.5** 둘 다 설치, 디바이스 플랫폼도 함께 설치돼 **아카이브까지 가능**하다.
>
> 당시 원인 서술(`destination 해석 실패`)은 **오진단이었다.** 실제로는 둘이다 — ① 스킴 `SDKROOT=iphoneos`인데 디바이스 플랫폼이 없어 시뮬 destination까지 무너지던 것(**`-sdk iphonesimulator` 명시로 우회 가능**) ② 진짜 벽은 애셋 카탈로그: `actool`이 SDK와 맞는 시뮬 런타임을 요구(`No simulator runtime version from ["22F77"] … SDK version 23F81a`). 둘 다 뿌리는 **플랫폼 미설치** 하나였다.
>
> ⚠️ **Xcode GUI 빌드는 별개 함정이 있다** — 스크립트 페이즈가 로그인 셸 환경을 상속하지 않아 `JAVA_HOME`이 비고 시스템 기본 JDK(OpenJDK 25)가 잡혀 Gradle 8.13이 죽는다(`* What went wrong: 25.0.1`). CLI는 통과하는데 Xcode만 실패하는 갈래라 헷갈린다. **`eaf7055`에서 preBuildScript가 JDK 17/21을 명시 해석하도록 해소됨.**

- iOS interop은 **SKIE**로 `Shared.framework`의 Swift API를 개선한다(suspend→async/await, Flow→AsyncSequence 등).
- ⚠️ **SKIE 0.10.12는 Kotlin 최대 2.3.21까지만** 지원 — Kotlin을 앞질러 올리지 말 것.

---

## 현재 상태

**M0~M8 구현 완료 — 앱 코드 레벨 완성.** 모델·직렬화(M1)·로컬 DB(M2)·네트워킹(M3)·Repository(M4)·ViewModel(M5)·
Compose UI 6화면(M6)·Koin 배선(M7)·통합·자산·seam actual(M8)까지 **4축 green**으로 닫혔다. 현재 **M9(검증·출시)** —
`[AI]` 트랙 완료(199 테스트: 실 Android 그래프 완전성·네이티브 DB 왕복·seam 로직·접근성 리포트) + **iOS 시뮬 첫 기동·
메인 앱·양 테마·반응형 DB 실증** + **Android 에뮬 Tier 1 스모크 완주**(adb 탭·타이핑 자율 주행 — 검색 3경로·북마크/히스토리 영속·
seam actual·외관 3모드·라이선스·아이콘). **시뮬/에뮬이 4축 green이 못 잡은 실 첫 기동 크래시 2건 포착·수정**: iOS 앱 링크
`-lsqlite3` 누락 + **Android manifest 클래스 경로 오류**(`.DevEtymApp`→`.android.DevEtymApp`, `ClassNotFoundException` 즉사).
이후 완주(2026-07-13): **iOS 시뮬 입력 주입 스모크 완주**(CGEvent 탭·타이핑) · **실기기 사인오프**(아이폰 13 mini — 셸 재설계 라운드 1·2 + VoiceOver) ·
**출시 시퀀스 A~D 완료**(A public 전환·B Pages 방침 URL 라이브·C 실기기 스모크·D iOS 스토어 스크린샷 캡처+캡션 프레이밍) ·
**출시 결정 D1~D9 전건 확정**(이름·키워드·카피·지역·등급·심사 노트). 결정 로그는 2026-08-25 서류 정돈에서 삭제 — 확정값은 App Store Connect와 `~/Downloads/devetym-release/store-metadata.md`에 산다.
**🎉 E iOS 배포 완료 — 2026-07-27 App Store 게시·라이브**([개발 어원 사전](https://apps.apple.com/kr/app/id6790429958), Apple ID `6790429958`). M9 DoD "스토어 게시" 폐쇄·iOS 트랙 종결.

**✅ v0.1.1 게시 완료 — 2026-08-18 라이브** (App Store 실측 2026-08-19: 현재 버전 `0.1.1`). 출시 후 첫 유지보수 릴리스 — 외부 제보([#19](https://github.com/data-sy/devetym/issues/19)) 수정 1건 단독.
설정 「앱 평가하기」가 무반응이던 것을 App Store 리뷰 딥링크로 교체했고, **실기기 검증 PASS**(iPhone 13 mini · iOS 26.5.2)로 종결 근거를 확보했다.
빌드 `0.1.1(3)` 제출 → 심사 통과 → 수동 게시 → **게시본에서 재확인 완료**(업데이트 받은 App Store 판으로 「앱 평가하기」 동작 확인). **수정이 실사용자에게 도달했고 제보자 회신까지 완료**(2026-08-19). 제보→회신 전 구간이 닫힌 첫 사이클 — 잔여 없음.

남은 것 = **[외부]** F Android 배포(후행 — 폐쇄테스트 20명×14일 게이트 + 스크린샷 캡처 잔여) · **웹 트랙 W(기반 완료·본체 미착수 — 아래)** · **씨딩·리뷰 확보 — W에 종속**(2026-08-19 결정: App Store 착지는 다운로드 마찰로 커뮤니티 참여도가 낮아, 씨딩은 웹 완성 후 웹과 함께 나간다).
진행 상태 정본은 [`ROADMAP.md`](ROADMAP.md), 출시 실무 자료의 위치는 [`docs/release/README.md`](docs/release/README.md).

**🌐 웹 트랙 W — 기반 라이브 (2026-08-25).** <https://devetym.com> 이 200 응답한다(Astro + Cloudflare Workers,
[ADR-0009](docs/adr/0009-web-framework-rendering.md)). 앱을 대체하는 게 아니라 **채널 확장**이다 — 웹 = 검색·씨딩 착지면, 앱 = 무제한 표면.
지금 있는 것은 **기반뿐**이다: 디자인 토큰을 앱 Kotlin 정본에서 **빌드마다 자동 추출**(손으로 안 베낀다 — 개수를 단언해 드리프트 시 빌드가 깨진다) ·
도메인 참조 `SITE_URL` 단일 지점 · 폰트 woff2 · 사이트맵 · 클라이언트 JS 0바이트.
**아직 없는 것**: 용어 페이지 650장·검색·AI 폴백. **[ADR-0012](docs/adr/0012-content-canon-d1.md)·[ADR-0013](docs/adr/0013-web-route-contract.md) 비준 완료(2026-08-25)로 게이트는 해소됐고, W0c(650 D1 시딩)는 2026-08-26 착수해 격리 환경까지 끝났다**([`w0c-sandbox-roadmap.md`](w0c-sandbox-roadmap.md) — 브랜치 `sandbox/w0c-d1-seeding`)
— 정본은 D1로 올라가고, 웹은 SSG 650 + 조회 전용 SSR 폴백(색인 자격은 품질 게이트가 연다)으로 간다. 상세 = [`web/README.md`](web/README.md), 상태 = [ROADMAP](ROADMAP.md) W 트랙.

**서버 캐시 트랙 S1 — 가동 중 (2026-07-28).** 앱 배포와 **독립**으로 완결되는 트랙이라 심사와 무관하게
먼저 나갔다. `devetym-proxy`에 D1 read-through 캐시를 붙여 **한 사용자가 생성시킨 항목을 다른 사용자가
재사용**한다 — 비용이 *사용자 수*가 아니라 *새 용어 수*에 비례한다. 라이브 실측으로 확인:
같은 용어 10회 접근에 Anthropic 호출 1회($0.0230), 한글 요청이 영문 정본 키로 접히고, 한도 소진
상태에서도 캐시 히트는 200을 준다. 클라 측 변경은 `normalizeKeyword` 동치 테스트 1건뿐(출하 동작 무변경).
상세는 [ROADMAP 백로그 항목 I](ROADMAP.md).

**병행 트랙 (2026-07-10 착수).** 원격 `data-sy/devetym`(2026-07-13 **public 전환**) → `m1`~`m8` 스택 PR(#1~8) 병합 + **PR #9 병합(2026-07-13, main=M9 검증 구간)** + **PR #11 병합(2026-07-14, main=제출 준비분)** + **PR #12 병합(제출 수정분: 아이폰 전용·VoiceOver)** + **PR #14 병합(Sentry 실 DSN 배선·실증)** + 원본 repo `~/dev-etymology` **이관·자기완결화** + 코드 갭 정리. **완료**: 이관 WU-1(**Pages 배포·방침 URL 라이브 2026-07-13**, [PR #10](https://github.com/data-sy/devetym/pull/10) 병합·<https://data-sy.github.io/devetym/>)·WU-2(Scripts·db-expand 검증)·WU-3(ai-quality→ADR-0007)·WU-4(크래시 리포팅 Sentry — 방침 사인오프 + **WU-4B 단일 KMP 통합**까지 완료, iOS도 실배선)·WU-5(launch-prep 대조)·WU-6(네이티브 iOS 전수 스윕·자기완결성 확증) + 코드 갭 WU-8(클립보드)·WU-9(스플래시)·WU-10(셸 회귀가드). **잔여**: WU-7(원본 repo 폐기·사람). **WU 계획·결정 원장은 2026-08-25 서류 정돈에서 삭제**(전건 소진). 잔여 WU-7의 절차·선행 게이트는 [ROADMAP](ROADMAP.md) Now에 자기완결적으로 옮겨 적었다.

**열린 PR: 없음 (2026-08-12 확인).** 종전 3건은 **전부 병합됨(2026-07-27)** — [#17](https://github.com/data-sy/devetym/pull/17) 서버 캐시 S1 스펙 · [#18](https://github.com/data-sy/devetym/pull/18) 클라 동치 테스트 · [devetym-proxy#3](https://github.com/data-sy/devetym-proxy/pull/3) 캐시 구현.

**열린 이슈 2건 (2026-08-16)** — 버그·개선 정본은 GitHub Issues([ADR-0008](docs/adr/0008-issue-tracking.md)):
[#16](https://github.com/data-sy/devetym/issues/16) '오픈ai' 검색 분기(enhancement) · [#15](https://github.com/data-sy/devetym/issues/15) 북마크 토글 미반영(bug).
둘 다 **v0.1.1에 태우지 않기로 확정**(2026-08-16) — #15는 수용 기준이 self-healing 구조라 검증 표면이 늘고 제보자 대기가 밀린다. 다음 판 대상.
〔[#19](https://github.com/data-sy/devetym/issues/19)는 CLOSED — 실기기 PASS 후 v0.1.1로 제출 완료. 검증 결과는 이슈 코멘트에 기록.〕

**브랜치 — 전부 원격에 있다 (2026-08-16 `git ls-remote` 실측).** 종전 "로컬 미푸시 2건" 서술은 stale이었다.
`fix/settings-review-deeplink`(#19, 병합됨·보존) · `docs/web-track-autonomy-prep`(웹 트랙 W 감사 준비 + 피드백 원문) · `chore/doc-pruning` — 로컬·원격 tip 일치.
⚠️ 다만 **제보 원문은 여전히 main에 없다** — `docs/feedback/`가 `docs/web-track-autonomy-prep`에만 있고, 그 브랜치 병합 시점은 웹 트랙 감사(0/26)와 묶인 별개 판단이다.

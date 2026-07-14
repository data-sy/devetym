# 개발 어원 사전 (DevEtym) — Compose Multiplatform

개발 용어의 **어원과 작명 이유**를 한국어로 설명하는 사전 앱.
단순히 뜻을 알려주는 게 아니라 *왜 그 이름이 붙었는지*를 설명해 개념 이해와 기억을 돕는다.

**Android · iOS 단일 코드베이스** — Kotlin Multiplatform 위에서 UI까지 Compose Multiplatform으로 공유한다.

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

핵심 데이터 흐름: **번들 DB(즉답) → 로컬 캐시 → AI 폴백(온라인)**. 자세한 건 [아키텍처 설계서](docs/architecture.md) 참고.

---

## 문서

이 repo는 **문서 → 구현** 순서로 채워 나간다. **M0~M8 구현 완료**(코드 레벨), 현재 **M9(검증·출시)** 단계다.

| 위치 | 내용 | 상태 |
|---|---|---|
| [`docs/product/prd.md`](docs/product/prd.md) | 제품 기획 — 문제·타겟·유저 스토리·콘텐츠 (*왜*의 정본) | ✅ |
| [`docs/architecture.md`](docs/architecture.md) | 아키텍처 설계 — 레이어링·Ktor·로컬 저장·Koin (기술 *어떻게*) | ✅ |
| [`docs/adr/`](docs/adr/) | 돌이킬 수 없는 결정 기록 (0001~0006: CMP·관용구 원칙·로컬 DB·프록시 경계·SKIE interop·서버 캐시 경계) | ✅ |
| [`docs/specs/spec.md`](docs/specs/spec.md) | 화면·동작 구현 명세 (Phase 1~4, Claude Code 전용) | ✅ |
| [`ROADMAP.md`](ROADMAP.md) | 이행 순서(코어 먼저, UI 나중) + **진행 상태 정본** | ✅ |
| [`docs/cost/`](docs/cost/) | API 비용 관리 — 결정 문서·Console 설정 스냅샷 로그 (`Scripts/cost/report.py`가 리포트 도구) | ✅ |

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
(⚠️ 앱 링크에 `-lsqlite3` 필요 — SQLiter cinterop). 상세는 [실기기/시뮬 스모크 대본](docs/release/m9-device-smoke-script.md).

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
**출시 결정 D1~D9 전건 확정**(이름·키워드·카피·지역·등급·심사 노트 — [결정 로그](docs/release/ios-launch-decision-prompt.md)).
남은 것 = **[외부]** E iOS 제출 **진행 중**(2026-07-14 — ASC 앱 레코드·스크린샷·메타 입력 + 제출 전 최종 대조 통과, 잔여 = 심사 노트 입력·재아카이브·제출·게시) · F Android 배포(후행·폐쇄테스트 20명×14일 게이트 + 스크린샷 캡처 잔여).
진행 상태 정본은 [`ROADMAP.md`](ROADMAP.md)(M9), 출시 지그·게이트는 [`docs/release/`](docs/release/).

**병행 트랙 (2026-07-10 착수).** 원격 `data-sy/devetym`(2026-07-13 **public 전환**) → `m1`~`m8` 스택 PR(#1~8) 병합 + **PR #9 병합(2026-07-13, main=M9 검증 구간)** + **PR #11 병합(2026-07-14, main=제출 준비분)** + **PR #12 병합(제출 수정분: 아이폰 전용·VoiceOver)** + **PR #14 병합(Sentry 실 DSN 배선·실증)** + 원본 repo `~/dev-etymology` **이관·자기완결화** + 코드 갭 정리. **완료**: 이관 WU-1(**Pages 배포·방침 URL 라이브 2026-07-13**, [PR #10](https://github.com/data-sy/devetym/pull/10) 병합·<https://data-sy.github.io/devetym/>)·WU-2(Scripts·db-expand 검증)·WU-3(ai-quality→ADR-0007)·WU-4(크래시 리포팅 Sentry — 방침 사인오프 + **WU-4B 단일 KMP 통합**까지 완료, iOS도 실배선)·WU-5(launch-prep 대조)·WU-6(네이티브 iOS 전수 스윕·자기완결성 확증) + 코드 갭 WU-8(클립보드)·WU-9(스플래시)·WU-10(셸 회귀가드). **잔여**: WU-7(원본 repo 폐기·사람). **독립 작업단위 WU-1~12 + 확정 결정(크래시 SDK=Sentry KMP 등)의 정본 = [`docs/handoff/26-07-10-selfcontained-migration-plan.md`](docs/handoff/26-07-10-selfcontained-migration-plan.md)** — 미래 세션이 WU 단위로 실행.

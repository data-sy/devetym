# 개발 어원 사전 (DevEtym) — Compose Multiplatform

개발 용어의 **어원과 작명 이유**를 한국어로 설명하는 사전 앱.
단순히 뜻을 알려주는 게 아니라 *왜 그 이름이 붙었는지*를 설명해 개념 이해와 기억을 돕는다.

**Android · iOS 단일 코드베이스** — Kotlin Multiplatform 위에서 UI까지 Compose Multiplatform으로 공유한다.

- 앱 표시 이름: **개발 어원 사전**
- 애플리케이션 ID / 번들 ID: `com.robin.devetym`
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
| 로컬 저장 | SQLDelight *또는* Room KMP (히스토리·북마크·AI 캐시) — [ADR에서 확정 예정](docs/adr/) |
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

이 repo는 **문서 → 구현** 순서로 채워 나간다. 현재는 설계 단계다.

| 위치 | 내용 | 상태 |
|---|---|---|
| [`docs/product/prd.md`](docs/product/prd.md) | 제품 기획 — 문제·타겟·유저 스토리·콘텐츠 (*왜*의 정본) | ✅ |
| [`docs/architecture.md`](docs/architecture.md) | 아키텍처 설계 — 레이어링·Ktor·로컬 저장·Koin (기술 *어떻게*) | ✅ |
| [`docs/adr/`](docs/adr/) | 돌이킬 수 없는 결정 기록 (CMP 선택·관용구 원칙·로컬 DB·프록시 경계) | ✅ |
| [`docs/specs/spec.md`](docs/specs/spec.md) | 화면·동작 구현 명세 (Phase 1~4, Claude Code 전용) | ✅ |
| [`ROADMAP.md`](ROADMAP.md) | 이행 순서(코어 먼저, UI 나중) + **진행 상태 정본** | ✅ |

---

## 현재 상태

**설계 문서 세트 완료 → 구현 착수 대기.** README·PRD·아키텍처·ADR·Spec·로드맵을 세웠고, 아직 앱 코드는 없다. 다음 착수는 [`ROADMAP.md`](ROADMAP.md)의 **M0(KMP 골격)** — 진행 상태 정본은 로드맵.

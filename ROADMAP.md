# DevEtym Roadmap (Compose Multiplatform)

DevEtym(개발 어원 사전) CMP 앱의 중장기 작업 계획이자 **진행 상태 정본**. 세부 실행 지시는 [`docs/specs/spec.md`](docs/specs/spec.md)·[`docs/architecture.md`](docs/architecture.md)·각 [ADR](docs/adr/)를 참조.

구축 원칙: **위험이 낮은 코어부터, UI는 마지막.** 거의 전부 `commonMain`, 플랫폼 조각(엔진·드라이버·셸)만 각자.

---

## Now — 진행 중

- **M0(KMP 골격) 완료 — 양 플랫폼 실제 실행 확인.** `shared + androidApp + iosApp` 골격이 서고,
  공유 `Greeting`을 Koin으로 배선해 **Android APK + iOS 시뮬레이터** 둘 다에서 공유 Compose 화면이 뜬다.
  - green 루프 3축: `:shared:testDebugUnitTest` · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64`(SKIE 포함).
  - iOS: `iosApp.xcodeproj`(XcodeGen 스펙 `project.yml`) — 시뮬레이터에서 렌더 검증 완료([`iosApp/README.md`](iosApp/README.md)). 실기기 서명·배포는 이후.
- **다음 착수 = Next M1(모델·직렬화) — 별도 세션.**

---

## Next — 구현 (코어 먼저, UI 마지막)

각 마일스톤은 앞 단계 완료를 전제로 순차 진행. 완료 시 Done으로 이관.

- **M0 · KMP 골격** — ✅ **완료**(Android APK + iOS 시뮬레이터 실제 실행 확인). `shared + androidApp + iosApp`, Koin `startKoin` 배선, 공유 `Greeting`을 양 플랫폼 Compose 화면에 표시.
  - 참조: architecture §3·§5, spec 1-6.
  - ✅ iOS interop 결정: **SKIE**([ADR-0005](docs/adr/0005-ios-interop.md)). 골격 버전(**빌드 실측 확정**): **Kotlin 2.3.21 · CMP 1.11.1 · AGP 8.13.0 · Gradle 8.13 · SKIE 0.10.12**. ⚠️ **SKIE 0.10.12는 Kotlin 최대 2.3.21**(2.4.0 거부, 실측) — SKIE가 새 Kotlin 지원 전엔 앞질러 올리지 말 것.
- **M1 · 모델·직렬화** — `TermEntry`(@Serializable)·`Source`·`TermResult`(sealed)·매퍼. 참조: spec 1-1.
- **M2 · 로컬 DB** — 스키마(`term`·`searchHistory`)·반응형 쿼리·드라이버 `expect`/`actual`.
  - ⚠️ 착수 전: **[ADR-0003](docs/adr/0003-local-storage.md) 확정**(SQLDelight vs Room, `klibs.io`로 iOS 성숙도 재확인). 참조: spec 1-2.
- **M3 · 네트워킹** — Ktor 클라이언트·Claude 요청/응답(tool_use 3분기)·`X-Device-Id`·429. 프록시 계약 그대로([ADR-0004](docs/adr/0004-backend-proxy-boundary.md)). 참조: spec 2-1·2-2.
- **M4 · Repository 오케스트레이터** — `fetch` 3단 흐름·upsert·북마크·히스토리·Analytics. Fake 협력자 테스트. 참조: spec 2-3·2-4.
- **M5 · ViewModel + StateFlow** — 화면 상태를 sealed로 노출. 참조: architecture §4.5.
- **M6 · Compose UI** — 검색/상세/북마크/히스토리/온보딩/설정. **반응형 `Flow`로 갱신(수동 재조회 없음, [ADR-0002](docs/adr/0002-code-idiom-principle.md))**. 참조: spec 3-x.
  - 선행: **디자인 토큰 확정**(`docs/design/`, 작성 예정) — 색·타이포 값. iOS dark-first·DM 서체를 출발점으로.
- **M7 · 배선·셸** — Koin 조립 마무리, 셸별 권한·진입점. 참조: architecture §3·§4.7.
- **M8 · 통합·마무리** — 오류 처리 통합·접근성·번들 DB 650 확장(iOS 자산 재사용)·앱 아이콘(Android adaptive + iOS)·스플래시. 참조: spec 4-x.

---

## Later — 백로그 (미착수 / 출시 후 / 검토)

- **[Ops] Android 첫 배포** — Play Console·AAB·keystore. 새로 배우는 영역(iOS 배포는 기존 경험 자산). CI(GitHub Actions)로 양쪽 빌드 자동화 검토.
- **[Docs] AI 품질 문서 이관** — 시스템 프롬프트 원문·도구 스키마를 `docs/ai-quality/` 정본으로(현재는 iOS 검증본을 `commonMain`에 계승). ADR로 흡수.
- **[Docs] db-expand 파이프라인 이관** — 번들 DB 생성·검증 파이프라인 문서(`docs/db-expand/`). claude.ai 정액 수동 경로 유지(API 종량 회피).
- **[Data] 번들 DB 추가 확장** — 출시 후 검색 빈도 데이터를 우선순위 입력으로.
- **[Arch] AI 스트리밍 도입 검토** — 현재 단발 응답. 토큰 스트리밍(`Flow<String>`)은 이후 선택지(architecture §4.3).
- **[Arch] 프롬프트 서버 이전 검토** — 현재 클라이언트(`commonMain`) 소유. 프롬프트 핫픽스 필요성 커지면 재검토([ADR-0004](docs/adr/0004-backend-proxy-boundary.md) 유보 항목).
- **[UI] 디자인 후속** — 다크/라이트 폴리시·대비·플랫폼별 미세 조정.
- (아이디어 추가 시 여기로)

---

## Done — 완료

- **프로젝트 문서 세트 수립** — 2026-07-04
  - **그린필드 CMP 설계로** README·[PRD](docs/product/prd.md)·[아키텍처 설계서](docs/architecture.md)·[ADR 0001~0004](docs/adr/)·[Spec](docs/specs/spec.md) 작성.
  - 동일 제품의 iOS(`dev-etymology`, SwiftUI) 구현에서 **검증된 데이터 흐름·설계 불변식을 계승**(fetch 3단·lazy 저장·upsert·aliases 보존·tool_use 3분기·프록시 계약), 관용구는 **코틀린으로**([ADR-0002](docs/adr/0002-code-idiom-principle.md): 리터럴 포팅 금지, 우회 패턴은 삭제).
  - 결정: CMP(UI까지 공유, [ADR-0001](docs/adr/0001-cross-platform-framework.md)) / 로컬 DB SQLDelight 우선·미확정([ADR-0003](docs/adr/0003-local-storage.md)) / 프록시 계약 계승([ADR-0004](docs/adr/0004-backend-proxy-boundary.md)).
- **repo 개설** — 2026-07-04. `devetym`(git init, 계정 `data-sy` 예약).

---

## 작업 단위 분할 원칙

작은 단일 앱이라 가벼운 구조를 쓴다.
- **Roadmap** — 모든 작업의 단일 인덱스이자 진행 상태 정본 (이 문서).
- **Architecture** — [`docs/architecture.md`](docs/architecture.md), 기술 설계 정본.
- **Spec** — [`docs/specs/spec.md`](docs/specs/spec.md), 구현 명세(Phase 1~4).
- **ADR** — 돌이킬 수 없는 결정 ([`docs/adr/`](docs/adr/)).

## 갱신 규칙

- 마일스톤 착수 시 Now로 이동, 브랜치명 함께 기록.
- 완료 시 Done으로 이동, 완료일·PR 번호 기록. 의사결정이 있었다면 ADR 번호도 함께.
- 새 아이디어는 Later에 먼저 추가하고, 우선순위가 오르면 Next로 승격.
- 보류 작업은 Next에 두고 "보류 사유" 명시.

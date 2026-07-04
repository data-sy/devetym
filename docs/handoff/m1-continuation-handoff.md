# 핸드오프 — M1 이어서 (새 세션용, 일회용)

> **성격: 일회용 인수인계.** 진행 상태 정본은 [`../../ROADMAP.md`](../../ROADMAP.md)다. 이 문서는 새 세션이 M1을 이어받는 시작점이며, **M1 구현 착수로 소비되면 삭제**한다(지속 결정은 ROADMAP·ADR로 흡수).
>
> 작성: 2026-07-05 종료 시점. 다음 세션(2026-07-06~)에서 이 링크를 열고 "이어서 시작하자"로 바로 착수 가능하게 씀.

---

## STEP 0 — 가장 먼저: M1 비준 verdict 확인

어제(2026-07-05) M1 스펙 슬라이스를 **적대적 비준 하네스**에 태웠고, 종료 시점엔 아직 수렴 중(round 5, cap 6)이었다. **다음 세션의 첫 동작은 최종 verdict 확인이다.**

1. **verdict 산출물**: `~/dev/agent-harnesses/runs/` 에 `<specName>-*.md`/`.json` verdict 파일이 있는지 본다(어제는 미생성 = 미완). 있으면 그게 정본 판정.
2. **비준 진행 흔적**: `git -C ~/devetym log --oneline | grep ratify` — `ratify(m1-model-serialization-draft): round N — close blockers` 커밋들(어제 round 2~5까지 로컬 커밋됨, 미push).
3. **비준 대상(수렴된 슬라이스)**: [`../specs/m1-model-serialization-draft.md`](../specs/m1-model-serialization-draft.md) — 라운드마다 blocker를 닫으며 갱신된 현재본. §7 열린 질문의 결착이 여기 반영돼 있다.

**판정 해석:**
- **RATIFIED** → 아래 §3 M1 구현으로 진행.
- **ESCALATE**(cap 6 소진 / 닫음 미코로보레이트 / 리뷰어 인프라 실패) → verdict의 잔여 Blocker·갭 목록을 사람이 검토 → 슬라이스 보강 후 **재비준**(§6 커맨드). 자율로 구현 넘어가지 말 것.
- 만약 어제 워크플로가 안 끝난 채였다면: 완료 통지가 왔는지 보고, 없으면 재실행(§6)하거나 마지막 라운드 슬라이스를 사람이 판단.

---

## 1. 현재 상태 (2026-07-05 종료)

- **repo/브랜치**: `~/devetym`, `feat/m1-model-serialization` (main 위, 원격 없음).
- **M1 스펙 슬라이스**: 작성 완료 + 적대 비준 다회 라운드 수렴(브랜치 로컬 커밋). 최종 verdict는 STEP 0.
- **ADR-0003 확정**: **SQLDelight 2.3.2** (커밋 `76f94db`, Status: Accepted). M2 착수 게이트 닫힘.
- **번들 발견**: `~/dev-etymology/DevEtym/DevEtym/Resources/terms.json` — **650개**, M1 `TermEntry`와 스키마 정합(6필드, 버전 필드 없음). 배치는 **M1 구현 시**로 defer(ROADMAP M8 항목에 포인터).
- **하네스 정돈**: 엔진/프로파일 분리 + devetym 프로파일 실채움 + 출력경로 버그 수정. `~/dev/agent-harnesses` 브랜치 **`harness/engine-profile-split`** (커밋 `bcda984`), **master 미머지**(사람 리뷰·머지 대기).
- **워킹트리**: devetym clean(어제 변경 전부 커밋). 원격 push 없음.

## 2. 잠긴 결정 — 건드리지 말 것 (근거는 ADR/슬라이스)

- **로컬 저장 = SQLDelight 2.3.2** ([ADR-0003](../adr/0003-local-storage.md)). 좌표는 ADR "Implementation Notes". *(M2 소관이지만 M1에서 라이브러리 도입 시 정렬 확인.)*
- **버전 상한 = Kotlin 2.3.21** (SKIE 0.10.12 캡, ROADMAP M0 Done). 어떤 라이브러리(kotlinx.serialization 포함)도 2.3.21에서 klib 소비 가능해야 함. **stale 버전 하드코딩 금지 — 착수 시 사실 확인.**
- **M1 슬라이스 §7 결착** (비준 수렴본 참조): category는 M1에서 `String` pass-through(강제 안 함, 강제는 M3·M4) / `Json` 인스턴스 설정은 M3로 미룸 / DTO↔엔티티 매퍼 경계는 비준 판정본을 따른다.

## 3. 다음 단계 — M1 구현 (verdict RATIFIED 시)

**범위**(슬라이스 §2·§3, spec [1-1](../specs/spec.md)):
- `commonMain/model/`: `TermEntry`(@Serializable, 버전 필드 옵셔널), `Source` enum, `TermResult` sealed interface, 카테고리 정본어휘(6개).
- **kotlinx.serialization JSON 왕복**(encode/decode), 버전 필드 없는 JSON 하위호환(INV-B, null default).
- **번들 배치**: `terms.json`(650) → `commonMain/composeResources`(또는 `data/bundle/`, spec 1-5). 버전 필드 없음 → INV-B 경로가 실물로 검증됨.
- `commonTest`: 슬라이스 §6 테스트 목록(왕복 보존·버전 하위호환·sealed 전수·category pass-through).

**green 오라클(수렴 기준)**: `:shared:testDebugUnitTest` · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64`.

**⚠️ 착수 전 결정 필요 — 구현을 어떻게 돌릴지:**
- **구현 하네스(`agent-harnesses/implementation/`)는 아직 미구현**(설계 방향 노트만). M1 구현을 다음 중 택:
  - (a) **이번 한 번 수동 구현**(모델·직렬화라 작음) → 구현 하네스는 나중에 더 큰 마일스톤에서 구축.
  - (b) **구현 하네스 먼저 구축**(harness-engineer 격리 세션, Workflow형: generate→green 게이트→적대 리뷰→cap) 후 M1을 태움.
- 순차 마일스톤(M1)이라 **worktree 격리 불필요**(비파괴·무상태). 병렬은 M6/M8만.

## 4. 후속·미해결 (백로그)

- **하네스 메타 문서 stale**: `~/dev/agent-harnesses/spec-ratification/README.md`·`design-prompt.md`가 구 경로(`spec-ratification/ratify-spec.workflow.js` → 이제 `engine/`)·구 호출예시(`args:{specPath}` → 이제 `profile`·`harnessRoot` 필수)를 가리킴. **harness-engineer 격리 세션**에서 갱신(운영 세션이 하네스 메타 안 고침).
- **하네스 self-ratify 권고**: 엔진/프로파일 분리는 큰 개조 → 하네스를 기술한 `harness-...-draft.md`로 한 번 self-ratify하거나 다른 격리 세션 적대 검수 권장(페르소나 §7.3).
- **하네스 브랜치 머지**: `harness/engine-profile-split` → master, 사람 리뷰 후.

## 5. 안전 규율 (불가침)

- **어떤 브랜치도 push하지 않는다. GitHub 원격 생성 금지.** 로컬 커밋만, push는 사람이 명시 지시할 때.
- **마일스톤 경계 사람 비준** 없이 다음으로 안 넘어간다. **하네스는 push·머지·`-draft` 제거를 하지 않는다.**
- **워크플로(멀티에이전트) 실행은 명시적 옵트인** — 그 세션에서 "돌려줘"라 할 때만.
- **하네스 수정 = 격리 세션 + `harness-engineer` 페르소나**(제어면/데이터면 분리). 운영 세션은 하네스 안 고친다.
- 네이밍은 젠더중립/여성형 기본.

## 6. 재개 커맨드·포인터

- **정본 읽기 순서**: [ROADMAP](../../ROADMAP.md)(상태·순서) → 이 핸드오프 → [M1 슬라이스](../specs/m1-model-serialization-draft.md) → [ADR-0003](../adr/0003-local-storage.md).
- **M1 재비준(ESCALATE거나 슬라이스 보강 후)**:
  ```
  Workflow({ scriptPath: "/Users/owner/dev/agent-harnesses/engine/ratify-spec.workflow.js",
             args: { profile: "devetym", harnessRoot: "/Users/owner/dev/agent-harnesses",
                     specPath: "docs/specs/m1-model-serialization-draft.md" } })
  ```
  (verdict는 `~/dev/agent-harnesses/runs/`에 떨어짐. Rule 1: specPath는 반드시 `-draft.md`.)
- **완료 시**: ROADMAP에서 M1 → Done 이관(완료일·근거), 이 핸드오프 삭제, M2(로컬 DB·SQLDelight) 착수 준비.

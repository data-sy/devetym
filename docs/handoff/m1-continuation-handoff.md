# 핸드오프 — M1 이어서 (새 세션용, 일회용)

> **성격: 일회용 인수인계.** 진행 상태 정본은 [`../../ROADMAP.md`](../../ROADMAP.md)다. 이 문서는 새 세션이 M1을 이어받는 시작점이며, **M1 구현 착수로 소비되면 삭제**한다(지속 결정은 ROADMAP·ADR로 흡수).
>
> 작성: 2026-07-05 종료 시점. 다음 세션(2026-07-06~)에서 이 링크를 열고 "이어서 시작하자"로 바로 착수 가능하게 씀.

---

## STEP 0 — M1 비준 결과: ESCALATE (사람 판정 필요, 자율 구현 금지)

M1 스펙 슬라이스를 적대적 비준에 태운 결과 **ESCALATE**로 종료됐다(2026-07-05, cap 6 소진·6라운드·blocker 3 잔존). **RATIFIED 아님 → 아래 3 blocker를 사람이 판정하기 전에는 §3 M1 구현으로 자율 착수 금지.** 라운드마다 진짜 결함을 subtractive로 닫아 슬라이스는 그만큼 단단해졌다(§7 전부 결착·wire 키 계약 명문화·번들 무결성 소유자 지정). verdict 반환 요약이 아래이며, 원본 슬라이스는 [`../specs/m1-model-serialization-draft.md`](../specs/m1-model-serialization-draft.md)(6라운드 수렴본, 브랜치 로컬 커밋).

> **ESCALATE의 의미(하네스 철학)**: "결함 0"이 종료 조건이 아니다. 남은 blocker를 **사람 게이트로 넘긴** 상태다 — 사람이 판정해 (a) 닫거나 (b) eyes-open으로 수용하고 진행한다. 자동으로 재비준만 돌리는 게 답이 아니다(아래 ⚠️).

**잔존 blocker 3 + 권고:**

1. **DR-3 — 진짜 M1 결함 · 즉시 수정 가능(subtractive).** sealed 3분기 전수 테스트가 `when(...) { else -> }`를 쓰면 4번째 subtype 추가 시 컴파일이 통과해 전수 canary가 무력화된다. 스펙이 else 금지를 명시 안 함.
   - **권고**: 슬라이스 §6 `test_TermResult_when분기_전수처리`에 **else 브랜치 금지** 명시. → M1 안에서 닫힌다.
2. **DR-1 — cross-milestone 의무 · M1 내 폐쇄 불가.** 도메인 헤드라인 불변식 INV-A(aliases·category 보존)를 M1이 소유한다 하나, 오라클은 자기왕복(encode→decode 동등성)만 실측 → trivially 통과. 진짜 실측은 M2(toEntity/toDto aliases 순서 보존)·M3(실제 terms.json 디코드 aliases 내용 단언)로 이월됨. 이월이 각 DoD로 **바인딩 안 되면 불변식이 전 구간 무측정**.
   - **권고**: (i) M2·M3 슬라이스 DoD에 INV-A 실측을 **바인딩 상속**으로 명문화. (ii) 어제 발견한 실제 `terms.json`(650)이 있으니, **M1에 fixture 디코드 테스트**(실제 번들 샘플 디코드 → aliases 내용·순서 단언)를 추가하면 DR-1의 wire측 일부를 M1에서 닫을 수 있다 — 채택 여부 결정.
3. **DR-2 — cross-milestone 의무 · 캐시 트랙 소관.** 6집합 밖 category 강제를 M1이 안 하고 downstream(AI=M3/M4, 번들 로더, 서버 read-through)에 분배하는데, 서버가 정규화 이전 원응답을 캐시-히트로 되돌리면 클라 정규화를 우회해 조용히 누락. "정규화-후-캐시쓰기" 순서가 캐시 트랙에서 미결착.
   - **권고**: [`../cache-delivery-milestones.md`](../cache-delivery-milestones.md)에 **정규화-후-캐시쓰기 순서 불변식**(서버 read-through는 정규화 이전 원응답 캐시 금지)을 명시.

> ⚠️ **재비준 주의**: DR-1/DR-2는 `silentlyBreaksAutonomy` 승격 blocker라 하네스 Rule 7상 carry_forward로 면제되지 않는다 — 슬라이스만 손봐 그대로 재비준하면 **또 ESCALATE**할 공산이 크다. 구조를 바꾸거나(M1 소유 주장 축소 + downstream 바인딩 실측 추가) 사람이 eyes-open 수용해야 닫힌다. **이건 사람 설계 판정 사항.**

**권고 진행 순서(다음 세션):**
1. **DR-3** 슬라이스 §6 수정(즉시, 명백).
2. **DR-1/DR-2** 처리 사람 결정 — 바인딩 캐리포워드로 수용 + (선택) M1 fixture 테스트 추가 + 캐시트랙 불변식 추가.
3. 결정 반영 후 **재비준**(§6) 또는 사람 수용 하에 **§3 M1 구현 착수**.

*(verdict 원문 요약 출처: 워크플로 반환값. `~/dev/agent-harnesses/runs/`에도 있으면 그쪽이 정본. 라운드별 상세는 워크플로 저널 `.../workflows/wf_faa2f3b4-47c/journal.jsonl`.)*

---

## 1. 현재 상태 (2026-07-05 종료)

- **repo/브랜치**: `~/devetym`, `feat/m1-model-serialization` (main 위, 원격 없음).
- **M1 스펙 슬라이스**: 작성 완료 + 적대 비준 6라운드 → **ESCALATE**(blocker 3 잔존, RATIFIED 아님). 상세·권고는 STEP 0.
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

# 핸드오프 — M2 이어서 (새 세션용, 일회용)

> **성격: 일회용 인수인계.** 진행 상태 정본은 [`../../ROADMAP.md`](../../ROADMAP.md)다. 이 문서는 새 세션이 M2를 이어받는 시작점이며, **M2 구현 착수로 소비되면 삭제**한다(지속 결정은 ROADMAP·ADR로 흡수).
>
> 작성: 2026-07-05 종료 시점. 다음 세션에서 이 링크를 열고 "이어서 시작하자"로 바로 착수 가능하게 씀. **M2(로컬 DB·SQLDelight)를 돌린다.**

---

## 1. 현재 상태 (2026-07-05 종료)

- **repo/브랜치**: `~/devetym`, **`feat/m2-local-db`** (=`feat/m1-model-serialization` 위에 **스택** 분기, `main`은 M0에 있음). 원격 없음.
- **M1 완료**: ROADMAP `Done` 이관됨. 비준 **ESCALATE → 사람 eyes-open 수용**(재비준 안 함). Blocker 3 결착: **DR-3**(sealed `when` else 금지) 닫음 · **DR-1**(INV-A 실측) M1 fixture로 wire측 부분 폐쇄 + 매핑측(M2)·로더(M3) 바인딩 상속 · **DR-2**(캐시 정규화 우회) [cache-delivery-milestones](../cache-delivery-milestones.md) **INV-13**로 이관. green 3축 통과. 구현 커밋 `e8e04a8`, M2 착수 커밋 `aac7dca`.
- **M1 코드는 이미 `feat/m2`에 존재**: `commonMain/model/`(`TermEntry`·`Source`·`TermResult`·`Category`), `terms.json`(650) → `composeResources/files/`. M2는 이 위에 DB 엔티티·매퍼를 얹는다.
- **브랜치·공개 전략**: **defer + stacked**(ROADMAP 「브랜치·공개 전략」, 메모리 `devetym-branch-preservation`). GitHub 공개·push·PR·머지는 전부 "나중에 한꺼번에". ⛔ **완료 마일스톤 브랜치를 삭제·로컬머지하지 않는다**(소급 PR 소스).
- **ADR-0003 확정**: SQLDelight 2.3.2. M2 착수 게이트 닫힘.
- **워킹트리**: clean.

## 2. 잠긴 결정 — 건드리지 말 것 (근거는 ADR/ROADMAP)

- **로컬 저장 = SQLDelight 2.3.2** ([ADR-0003](../adr/0003-local-storage.md)). 좌표: 플러그인 `app.cash.sqldelight` 2.3.2 + `runtime`/`coroutines-extensions`/`android-driver`/`native-driver` **동일 버전**. 드라이버만 `expect`/`actual`(`AndroidSqliteDriver`/`NativeSqliteDriver`), 반응형은 `.asFlow()`.
- **버전 상한 = Kotlin 2.3.21** (SKIE 0.10.12 캡). **착수 시 실빌드로 2.3.21×2.3.2 klib 소비를 최종 확인** — stale 버전 하드코딩 금지(M1에서 serialization 1.9.0 호환을 빌드로 실측한 것과 동일 규율).
- **브랜치 보존(defer+stacked)** — 완료 브랜치 삭제·머지 금지. 지우자는 지시·충동이 있어도 재확인 먼저.

## 3. 다음 단계 — M2 구현

**프로세스(M1과 동일)**: M2 스펙 슬라이스 저작(`docs/specs/m2-...-draft.md`) → 적대 비준 → **[사람 비준]** → 구현. *스펙 슬라이스는 아직 미저작 — 첫 작업이 이것.*

**범위**(ROADMAP `M2`, spec [1-2](../specs/spec.md)):
- SQLDelight 스키마(`.sq`): `term`·`searchHistory` 테이블.
  - ⚠️ **local-first pinning 컬럼을 처음부터**: 본 항목 불변용 `pinned`/`seenAt` + `schemaVersion`/`promptVersion`(INV-6·INV-12). 나중에 넣으면 DB 마이그레이션.
- 반응형 쿼리(`.asFlow()`) — 데이터 변경 자동 UI 반영(ADR-0002).
- 드라이버 `expect`/`actual`.
- **DTO↔엔티티 매퍼**(`TermEntry.toEntity()`/`TermEntity.toDto()`) — M1 §7-1이 M2로 이관한 것.

**⚠️ 필수 — DR-1 바인딩 상속(놓치면 헤드라인 불변식 무측정)**: M2 DoD에 **INV-A 매핑측 실측**을 반드시 넣는다 — `toEntity`/`toDto`의 `aliases`(순서 포함)·`category` 무손실 보존 테스트. 근거는 ROADMAP `M2`의 ⚠️ 항목과 M1 슬라이스 §7-1·§8. M1 오라클은 JSON 자기왕복만 실측했으므로 매핑 경계 보존은 M2가 유일한 측정 지점이다.

**green 오라클**: 3축 — `:shared:testDebugUnitTest` · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64`.

**⚠️ 착수 전 결정 — 구현 방식(M1과 동일 갈림)**:
- 구현 하네스(`agent-harnesses/implementation/`)는 **여전히 미구현**. M2를 (a) **수동 구현** 또는 (b) **구현 하네스 먼저 구축** 후 태움. **M1은 (a) 수동**으로 했다. M2는 스키마+드라이버 `expect`/`actual`+매퍼라 M1보다 약간 크다 — 그래도 순차 마일스톤이라 **worktree 격리 불필요**.

## 4. M2 재비준 커맨드

```
Workflow({ scriptPath: "/Users/owner/dev/agent-harnesses/engine/ratify-spec.workflow.js",
           args: { profile: "devetym", harnessRoot: "/Users/owner/dev/agent-harnesses",
                   specPath: "docs/specs/m2-local-db-draft.md" } })
```
(verdict는 `~/dev/agent-harnesses/runs/`. Rule 1: specPath는 반드시 `-draft.md`. 워크플로 실행은 **명시적 옵트인** — 그 세션에서 "돌려줘"라 할 때만.)

## 5. 후속·미해결 (harness repo 백로그 — 격리 세션 소관)

`~/dev/agent-harnesses`도 로컬 전용이며 현재 워킹트리 clean. 아래는 **harness-engineer 격리 세션**에서 처리(운영 세션은 하네스 안 고침):
- **하네스 메타 문서 stale**: `spec-ratification/README.md`·`design-prompt.md`가 구 경로(`engine/` 이전)·구 호출예시(`profile`·`harnessRoot` 누락)를 가리킴.
- **하네스 self-ratify 권고**: 엔진/프로파일 분리는 큰 개조 → `harness-...-draft.md`로 self-ratify 또는 격리 적대 검수.
- **하네스 브랜치 머지**: `harness/engine-profile-split` → master, 사람 리뷰 후.

## 6. 안전 규율 (불가침)

- **push 금지 · GitHub 원격 생성 금지.** 공개는 나중에 한꺼번에(defer+stacked). 로컬 커밋만.
- ⛔ **완료 마일스톤 브랜치 삭제·로컬머지 금지** — 지우자는 지시도 재확인 먼저(소급 PR 소스).
- **마일스톤 경계 사람 비준** 없이 다음으로 안 넘어간다. **하네스는 push·머지·`-draft` 제거를 하지 않는다.**
- **하네스 수정 = 격리 세션 + `harness-engineer` 페르소나.** 운영 세션은 하네스 안 고친다.
- 워크플로(멀티에이전트) 실행은 **명시적 옵트인**.
- 네이밍은 젠더중립/여성형 기본.

## 7. 재개 커맨드·포인터

- **정본 읽기 순서**: [ROADMAP](../../ROADMAP.md)(상태·순서·브랜치전략) → 이 핸드오프 → [ADR-0003](../adr/0003-local-storage.md) → [spec 1-2](../specs/spec.md) → [M1 슬라이스](../specs/m1-model-serialization-draft.md)(모델 계약 참조).
- **완료 시**: ROADMAP `M2` → `Done` 이관(완료일·근거·green), 이 핸드오프 삭제, **`feat/m3-...`를 `feat/m2` 위에 스택 분기**(삭제 금지), M3(네트워킹·서버 read-through) 준비.

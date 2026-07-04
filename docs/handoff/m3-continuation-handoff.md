# 핸드오프 — M3 이어서 (새 세션용, 일회용)

> **성격: 일회용 인수인계.** 진행 상태 정본은 [`../../ROADMAP.md`](../../ROADMAP.md)다. 이 문서는 새 세션이 M3을 이어받는 시작점이며, **M3 구현 착수로 소비되면 삭제**한다(지속 결정은 ROADMAP·ADR로 흡수).
>
> 작성: 2026-07-05(M2 완료 시점). 다음 세션에서 이 링크를 열고 "이어서 시작하자"로 바로 착수 가능하게 씀. **M3(네트워킹 + 서버 read-through)를 돌린다.**

---

## 1. 현재 상태 (2026-07-05, M2 완료)

- **repo/브랜치**: `~/devetym`, **`feat/m3-networking`** (=`feat/m2-local-db` 위에 **스택** 분기, `main`은 M0에 있음). 원격 없음.
- **M2 완료**: ROADMAP `Done` 이관됨. 비준 **ESCALATE → 사람 eyes-open + B1**(네이티브 실행 축 추가로 직렬화 절반 폐쇄, 재비준 안 함). **green 4축** 통과: `:shared:testDebugUnitTest`(17) · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64` · **`:shared:iosSimulatorArm64Test`(11, B1 신규)**. 구현 커밋 `46a7688`.
- **M2 코드는 이미 `feat/m3`에 존재**: `commonMain/data/local/`(`DevEtym.sq`·`DriverFactory` expect/actual·`TermMapper`), `commonMain/model/`(M1). M3는 이 위에 **번들 로더·네트워킹**을 얹는다.
- **브랜치·공개 전략**: **defer + stacked**(ROADMAP 「브랜치·공개 전략」, 메모리 `devetym-branch-preservation`). GitHub 공개·push·PR·머지는 전부 "나중에 한꺼번에". ⛔ **완료 마일스톤 브랜치를 삭제·로컬머지하지 않는다**(소급 PR 소스). 현 스택: `m0(→main) · feat/m1 · feat/m2 · feat/m3` 전부 보존.
- **ADR-0006 확정**: 백엔드 = read-through 캐시 프록시(ADR-0004 대체). M3 계약 게이트 닫힘.
- **워킹트리**: clean.

## 2. 잠긴 결정 — 건드리지 말 것 (근거는 ADR/ROADMAP)

- **백엔드 계약 = read-through 프록시** ([ADR-0006](../adr/0006-server-cache-boundary.md)). 앱에 API 키 없음 — 프록시가 키 주입 + 기기당 일일 한도(`X-Device-Id`, 429). 클라엔 **투명**: `Source`는 여전히 `BUNDLE` vs 네트워크(→`AI`), D1 히트 여부는 서버 내부 사정이라 호출 형태는 그대로. 응답의 `schemaVersion`/`promptVersion`(INV-9)만 `TermEntry`로 왕복.
- **버전 상한 = Kotlin 2.3.21** (SKIE 0.10.12 캡). **Ktor 좌표는 착수 시 실빌드로 2.3.21 호환을 최종 확인** — stale 버전 하드코딩 금지(M1 serialization·M2 SQLDelight를 빌드로 실측한 것과 동일 규율).
- **브랜치 보존(defer+stacked)** — 완료 브랜치 삭제·머지 금지. 지우자는 지시·충동이 있어도 재확인 먼저.
- **⚠️ green 오라클 네이티브 실행 갭 — M3 슬라이스 DoD에 처음부터 `:shared:iosSimulatorArm64Test` 축을 넣는다.** 3축 green은 JVM만 실행·네이티브는 링크만 확인이라, 네이티브 런타임 정확성(Ktor 엔진·직렬화 왕복)이 무측정으로 남으면 **M1·M2처럼 비준 blocker로 재ESCALATE**된다(메모리 [green-oracle-native-execution-gap], M2 §5 B1 선례). 순수 commonTest(엔진 무관 파서·매퍼)를 네이티브 실행 축에 태워 선제 폐쇄.

## 3. 다음 단계 — M3 구현

**프로세스(M1·M2와 동일)**: M3 스펙 슬라이스 저작(`docs/specs/m3-networking-draft.md`) → 적대 비준 → **[사람 비준]** → 구현. *스펙 슬라이스는 아직 미저작 — 첫 작업이 이것.*

**범위**(ROADMAP `M3`, spec [2-1·2-2](../specs/spec.md)):
- **BundleDbSource**(`data/bundle/`) — `search`(정규화 후 keyword/aliases 완전 매칭)·`autocomplete`(prefix). `terms.json`(650) 앱 시작 1회 로드·메모리 캐시.
- **ClaudeApi**(`data/remote/`, Ktor) — read-through 프록시 호출(Claude 직접 아님), `tool_use` 3분기(`return_term_entry`→`Found(AI)` / `return_not_dev_term`→`NotDevTerm` / `return_possible_typo`→`PossibleTypo`), `X-Device-Id` 헤더, 429→`DailyLimitExceeded`. 시스템 프롬프트·도구 스키마는 `commonMain`(iOS 검증본 계승).
- **서버 `devetym-proxy` 신규 구축**(별도 repo, Worker/D1) — D1 스키마·Worker read-through(D1→API·write-back·first-write-wins)·single-flight(DO)·validator write-게이트·rate-limit/남용/무효화.

**⚠️ 필수 — M1 DR-1/DR-2 바인딩 상속(놓치면 헤드라인 불변식 무측정)**:
- **INV-A wire측 로더 실측(M1 DR-1)**: 번들 로더(`BundleDbSource`)의 **실제 로드 경로**가 aliases 내용을 보존하는지 **M3 DoD 회귀 가드**로 테스트한다 — 실제 배포 `terms.json`을 로더로 로드해 알려진 term의 aliases *내용*을 단언(**성공 디코드는 무효 오라클** — wrong-key·키 생략도 예외 없이 성공 디코드돼 aliases가 silent 소실). 근거: M1 슬라이스 §7-4·§8, ROADMAP M3 ⚠️.
- **서버 read-through category 소유(M1 DR-2)**: 서버가 정규화 이전 원응답을 캐시-히트로 되돌려 클라 정규화를 우회하지 않도록 **정규화-후-캐시쓰기 순서 고정**(집합 밖 category clamp 후 write-back). 정본 불변식: [cache-delivery-milestones](../cache-delivery-milestones.md) **INV-13**. write 게이트(validator)와 같은 지점에서 category 정규화 적용.

**green 오라클**: **4축** — `:shared:testDebugUnitTest` · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64` · **`:shared:iosSimulatorArm64Test`**(M2에서 추가, §2 참조). 서버는 별도 repo·언어라 자체 테스트 오라클을 슬라이스가 정한다.

**⚠️ 착수 전 결정**:
- **구현 방식**(M1·M2와 동일 갈림): 구현 하네스는 여전히 미구현. M3을 (a) **수동 구현** 또는 (b) 하네스 먼저. M1·M2는 (a) 수동. 순차 마일스톤이라 worktree 격리 불필요(클라측).
- **클라 vs 서버 스코핑**: M3은 **서버(devetym-proxy, 별도 repo·TS/Worker)를 포함**해 M1·M2보다 크다. 슬라이스가 (i) 클라 네트워킹+번들 로더와 서버를 한 슬라이스로 볼지, (ii) 서버를 별도 슬라이스/트랙으로 뗄지 결정한다 — 서버 repo가 아직 없으므로 이 스코핑을 슬라이스 첫머리에서 판정한다.

## 4. M3 재비준 커맨드

```
Workflow({ scriptPath: "/Users/owner/dev/agent-harnesses/engine/ratify-spec.workflow.js",
           args: { profile: "devetym", harnessRoot: "/Users/owner/dev/agent-harnesses",
                   specPath: "docs/specs/m3-networking-draft.md" } })
```
(verdict는 `~/dev/agent-harnesses/runs/`. Rule 1: specPath는 반드시 `-draft.md`. 워크플로 실행은 **명시적 옵트인** — 그 세션에서 "돌려줘"라 할 때만.)

## 5. 후속·미해결 (harness repo 백로그 — 격리 세션 소관)

`~/dev/agent-harnesses`도 로컬 전용. 아래는 **harness-engineer 격리 세션**에서 처리(운영 세션은 하네스 안 고침):
- **하네스 메타 문서 stale**: `spec-ratification/README.md`·`design-prompt.md`가 구 경로·구 호출예시를 가리킴(M2 핸드오프에서 이월).
- **하네스 self-ratify 권고**: 엔진/프로파일 분리는 큰 개조 → self-ratify 또는 격리 적대 검수.
- **하네스 브랜치 머지**: `harness/engine-profile-split` → master, 사람 리뷰 후.

## 6. 안전 규율 (불가침)

- **push 금지 · GitHub 원격 생성 금지.** 공개는 나중에 한꺼번에(defer+stacked). 로컬 커밋만.
- ⛔ **완료 마일스톤 브랜치 삭제·로컬머지 금지** — 지우자는 지시도 재확인 먼저(소급 PR 소스).
- **마일스톤 경계 사람 비준** 없이 다음으로 안 넘어간다. **하네스는 push·머지·`-draft` 제거를 하지 않는다.**
- **하네스 수정 = 격리 세션 + `harness-engineer` 페르소나.** 운영 세션은 하네스 안 고친다.
- 워크플로(멀티에이전트) 실행은 **명시적 옵트인**.
- 네이밍은 젠더중립/여성형 기본.

## 7. 재개 커맨드·포인터

- **정본 읽기 순서**: [ROADMAP](../../ROADMAP.md)(상태·순서·브랜치전략) → 이 핸드오프 → [ADR-0006](../adr/0006-server-cache-boundary.md)(서버 계약) → [spec 2-1·2-2](../specs/spec.md) → [M1 슬라이스](../specs/m1-model-serialization-draft.md) §7-4(로더 실측 계약)·[M2 슬라이스](../specs/m2-local-db-draft.md)(모델·DB 계약 참조) → [cache-delivery-milestones](../cache-delivery-milestones.md) INV-13.
- **완료 시**: ROADMAP `M3` → `Done` 이관(완료일·근거·green 4축), 이 핸드오프 삭제, **`feat/m4-...`를 `feat/m3` 위에 스택 분기**(삭제 금지), M4(Repository 오케스트레이터) 준비. M4는 ⚠️ upsert 보존(createdAt·M2 DR-M2-2)·schemaVersion Int범위(M2 DR-M2-3)를 DoD로 상속(ROADMAP M4 ⚠️ 항목).

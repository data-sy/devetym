# M1 슬라이스 (draft) — 모델·직렬화

> **성격: 비준 하네스 입력용 spec 슬라이스(`-draft.md`).** 단일 [`spec.md`](spec.md) Phase 1의 모델·직렬화 부분을 마일스톤 경계로 떼어낸 것. 진행 상태 정본은 [`../../ROADMAP.md`](../../ROADMAP.md), 상위 설계는 [`../architecture.md`](../architecture.md), 결정 근거는 [`../adr/`](../adr/).
>
> 이 문서는 **자율 구현 전 적대적 비준(완결성·모호성·테스트 가능성·정합성)의 대상**이다. 아래 §7 열린 질문은 비준이 판정할 항목이다.

- **마일스톤**: M1 · 모델·직렬화
- **브랜치**: `feat/m1-model-serialization` (main에서 분기, M0 병합 완료 위)
- **참조**: spec [1-1](spec.md), [ADR-0002](../adr/0002-code-idiom-principle.md)(코틀린 관용), 캐시 트랙 INV-9(버전 태깅), [cache-delivery-milestones](../cache-delivery-milestones.md)

---

## 1. 목표 (이 슬라이스가 내는 것)

`commonMain`에 도메인 모델과 그 **직렬화 가능한 shape**(`@Serializable` 필드 구조 + 왕복 보존 불변식)을 세운다. 이 모델은 이후 세 소비처의 공통 shape다: (a) 번들 DB `terms.json` 역직렬화, (b) 서버 read-through 응답(프록시가 `TermEntry`를 왕복), (c) 로컬 DB 저장(엔티티 변환 — 경계는 §7 참조). **소비처가 공유할 `Json` 인스턴스 설정(encodeDefaults·ignoreUnknownKeys 등)은 이 슬라이스가 확정하지 않는다 — 네트워크 슬라이스(M3, §7-3) 소관이다.** M1이 고정하는 것은 모델 필드 구조와 그 위의 왕복 보존(§4·§6)뿐이다. UI·네트워크·DB 드라이버는 이 슬라이스 밖이다.

## 2. 스코프

**IN (M1):**
- `TermEntry` — `@Serializable` DTO (`commonMain/model/`).
- `Source` enum, `TermResult` sealed interface.
- 카테고리 고정 집합(6개) 정의와 그 제약.
- **kotlinx.serialization JSON 왕복**(encode/decode), 버전 필드 옵셔널·default 동작 포함.
- 위를 검증하는 `commonTest`.

**OUT (다른 마일스톤):**
- 로컬 DB 스키마·`TermEntity`·반응형 쿼리·드라이버 → **M2**(spec 1-2).
- `BundleDbSource`·`ClaudeApi`·`TermRepository` → **M3·M4**(spec 2-x).
- 상수(`Constants`)·Koin 배선·번들 로더·셸 진입점 → 이미 M0 골격 또는 후속 슬라이스.
- **DTO↔엔티티 매퍼**(`TermEntry.toEntity()`/`TermEntity.toDto()`)의 경계는 **§7 열린 질문**(엔티티가 M2 소관이라 결착 필요).

## 3. 산출 명세

### 3-1. `TermEntry` (`commonMain/model/`)
```kotlin
@Serializable
data class TermEntry(
    val keyword: String,
    val aliases: List<String> = emptyList(),
    val category: String,
    val summary: String,
    val etymology: String,
    val namingReason: String,
    // 버전 태깅 (INV-9, 서버 캐시·딜리버리 트랙 선반영) — 옵셔널·default로 기존 번들 DB/AI 응답과 호환.
    val schemaVersion: Int? = null,
    val promptVersion: String? = null,
)
```
- **버전 필드는 옵셔널**: 현재 번들 DB·AI 응답 어디에도 없으므로 역직렬화 시 default(`null`). 서버 캐시 트랙 착수 전까지 채우지 않되, 지금 필드만 확보해 이후 `@Serializable` DTO 마이그레이션을 회피(INV-9).
- **wire 키 계약**: JSON 키 = Kotlin 프로퍼티명(camelCase)이 M1의 명시 계약이다(`@SerialName` 별도 지정 없음 = 프로퍼티명이 곧 wire 키). 외부 생산자(§1 소비처 a 번들 `terms.json`·b 서버)는 이 키 이름으로 내보내야 하며, 다른 naming(예: `alias` 단수·`aliases` 생략)으로 내보내면 `aliases`가 default `emptyList()`로 예외 없이 조용히 떨어져 별칭이 소실된다(INV-A 위반 경로). M1 §6 오라클은 **자기 왕복**(encode→decode 동등성)만 실측하므로 이 계약을 외부 픽스처로 실측하지 않는다 — aliases를 담은 실제 `terms.json`/서버 응답을 디코드해 계약 준수를 실측하는 것은 **번들 로더(M3)** 소관으로 이월(§7-4).

### 3-2. `Source` / `TermResult` (`commonMain/model/`)
```kotlin
enum class Source { BUNDLE, AI }

sealed interface TermResult {
    data class Found(val entry: TermEntry, val source: Source) : TermResult
    data object NotDevTerm : TermResult
    data class PossibleTypo(val suggestion: String) : TermResult
}
```
- 결과 출처는 문자열이 아니라 타입으로 — 컴파일 타임 분기 강제. `sealed`라 `when`이 전수 분기.

### 3-3. 카테고리 정본 어휘 (번들 DB·AI 응답 공통, 6개)
`동시성` · `자료구조` · `네트워크` · `DB` · `패턴` · `기타` — downstream(카테고리 필터·버킷팅)이 기대하는 정본 집합.
- **M1 계약은 `category: String` pass-through다.** M1 직렬화/역직렬화는 집합 밖 값(오타 `네트웤`·영문값 `Database` 등)도 **검증·거부·정규화 없이** 그대로 왕복 보존한다(INV-A). 6개 집합에 대한 **강제/정규화는 M1 밖** — 집합 밖 값이 유입되는 각 경로의 소관이다(§7-2 결착: AI 응답=M3·M4 강제/정규화, 사람이 큐레이션하는 번들 `terms.json`=번들 저작/린트가 in-set 보장). M1은 이 어휘를 *문서화*하되 *강제하지 않는다*(§7-2 결착 참조).

## 4. 설계 불변식 (이 슬라이스가 반드시 지킬 것)
- **INV-A `aliases`·`category` 보존**: M1이 실측하는 것은 **직렬화/역직렬화 왕복**에서 `aliases`(순서 포함)와 `category`의 무손실 보존이다(§6 오라클). 이 불변식은 DTO↔엔티티 변환(`toEntity()`/`toDto()`)에서도 성립해야 하나, 그 매핑 경계는 `TermEntity`(M2 소관)에 의존하므로 **M1에서는 테스트 불가** — 매핑측 보존은 M2 DoD로 상속된다(§7-1). M1 계층에서는 이 **보존이 거부에 우선한다**: 6개 집합 밖 `category`라도 예외 없이 보존하며 M1에서 거부·정규화하지 않는다(§3-3, 강제는 downstream).
- **INV-B 버전 필드 하위호환**: 버전 필드 없는 JSON(기존 번들 DB shape)을 역직렬화하면 `schemaVersion`/`promptVersion`이 `null`로 채워지고 예외가 나지 않는다.
- **ADR-0002 코틀린 관용**: iOS(SwiftData) 우회 패턴을 옮기지 않는다. 순수 `@Serializable` data class + kotlinx.serialization 관용.

## 5. 완료 조건 (DoD) — 하네스 수렴 오라클
- `commonMain`의 모델·직렬화가 **Android·iOS 양쪽에서 컴파일**된다: `:shared:testDebugUnitTest` + `:androidApp:assembleDebug` + `:shared:linkDebugFrameworkIosSimulatorArm64` green(M0에서 세운 3축 green 루프).
- 아래 §6 `commonTest`가 전부 통과.
- 새 라이브러리 좌표(kotlinx.serialization)를 도입하면 **버전 정렬을 사실로 확인**(Kotlin↔serialization 플러그인 호환) — stale 버전 하드코딩 금지. 이 확인은 load-bearing이다: 버전 카탈로그 헤더의 '빌드 확인' 표기를 이 확인의 대체물로 삼지 말 것(헤더는 serialization 런타임의 `commonMain` 배선을 보증하지 않는다).

## 6. 테스트 (`commonTest/`) — 함수명 `test_[대상]_[조건]_[기대]`
- `test_TermEntry_직렬화왕복_모든필드보존` — encode→decode 후 **디코드된 객체가 원본과 동등**(`==`, data class 구조 동등; 특히 `aliases` 순서·`category`)함으로 검증한다. JSON 키 존재 여부가 아니라 **객체 동등성**을 오라클로 삼아 `encodeDefaults` 설정(default 필드 생략 여부)과 무관하게 결정적이게 한다.
- `test_TermEntry_버전필드없는JSON_역직렬화시null` — 기존 번들 shape 하위호환(INV-B).
- `test_TermEntry_버전필드있는JSON_왕복보존` — 서버 배달 shape(INV-9).
- `test_TermResult_when분기_전수처리` — sealed 3분기(`Found`/`NotDevTerm`/`PossibleTypo`) 컴파일 타임 전수. **`else` 브랜치 금지(DR-3 결착)**: 이 테스트의 `when(result)`은 반드시 세 subtype을 명시 분기하고 `else ->`를 두지 않는다. `else`를 두면 4번째 subtype 추가 시에도 컴파일이 통과해 전수 canary가 무력화되므로, 미래에 subtype이 늘면 이 `when`이 컴파일 에러로 실패해야 한다(그 컴파일 에러가 곧 canary다).
- `test_카테고리_집합밖값_처리` — 집합 밖 `category`(예: 오타 `네트웤`, 영문 `Database`)가 M1 왕복에서 **예외 없이 그대로 보존**됨(pass-through, INV-A). M1은 거부·정규화하지 않는다(강제는 M3·M4).
- `test_실제번들_terms디코드_aliases내용보존` — **DR-1 wire측 M1 부분 폐쇄(사람 eyes-open 수용 하 채택)**. 실제 배포 `terms.json`(650, composeResources 배치본)을 로드·디코드해 (a) 항목 수가 650, (b) aliases를 가진 **알려진 term의 aliases *내용*을 단언**한다(예: `aa-tree` → `["AA 트리", "Arne Andersson tree"]`, `category` = `자료구조`). **성공 디코드는 오라클로 삼지 않는다** — wrong-key·키 생략 JSON도 예외 없이 성공 디코드되어 `aliases = emptyList()`를 산출하므로(별칭 소실이 silent), 반드시 aliases *내용*을 단언한다(§7-4 무효 오라클 규정 준수). 이 테스트는 §3-1 wire 키 계약(JSON 키=camelCase 프로퍼티명)이 실제 번들 문서에서 지켜짐을 M1에서 실측한다. **잔여 이월**: 번들 *로더*(M3 `BundleDbSource`)의 실제 로드 경로 회귀 가드와 DTO↔엔티티 매핑측(M2) 보존은 여전히 각 DoD로 상속된다(§7-1·§7-4, 아래 §8 및 ROADMAP M2/M3 바인딩 참조).

## 7. 열린 질문 (비준이 판정할 항목)

1. **매퍼 소속 경계 (ROADMAP↔spec 정합)** — ROADMAP은 M1에 "매퍼"를 명시하나, spec 1-1의 매퍼(`TermEntry.toEntity()`/`TermEntity.toDto()`)는 `TermEntity`(spec 1-2, **M2** 로컬 DB 로우)에 의존한다. 제안: **DB-엔티티 매퍼는 M2로 이관**하고, M1은 *직렬화* 계약(JSON↔`TermEntry`)과 보존 불변식(INV-A)만 소유. 대안: M1에 엔티티 타입을 선정의하고 매퍼까지 포함(단 M2 스키마와 결합). — **비준 판정 필요.** 매퍼를 M2로 이관하면 **INV-A의 매핑측(`toEntity`/`toDto`) `aliases`(순서)·`category` 보존 테스트를 M2 DoD로 반드시 상속**한다(carry-forward): M1 오라클(§6)은 JSON 왕복만 실측하므로 매핑 경계 보존은 M1에서 무측정이며, INV-A가 M1 오라클보다 넓게 걸려 있음을 M2 비준자가 인지해야 이 도메인의 헤드라인 불변식(DTO↔엔티티 `aliases` 누락)이 어느 마일스톤에서도 실측되지 않은 채 조용히 소실되는 것을 막는다.
2. **카테고리 강제 지점 — 결착(round 2): M1은 강제하지 않는다.** M1 계약은 `category: String` pass-through로, 집합 밖 값도 손실 없이 보존한다(§3-3·INV-A). 6개 집합에 대한 검증/정규화는 집합 밖 값이 유입되는 각 지점이 소유한다: (a) AI 응답 경로(M3·M4)는 유입되는 집합 밖 값을 강제/정규화한다; (b) 사람이 큐레이션하는 번들 `terms.json`은 번들 저작 계약과 번들 로더/린트(M3 `BundleDbSource` 로드 경로)가 6개 집합 in-set를 보장한다 — 저작 오타로 집합 밖 값이 들어가면 downstream 버킷팅에서 조용히 누락되므로, 번들 경로의 category 무결성은 무주공산이 아니라 번들 로더/린트 소관이다. (c) 서버 read-through 배달 경로(§1 소비처 b)는 M1이 소유하지 않는다 — 서버가 정규화 이전 원응답을 캐시해 캐시-히트로 되돌려주면 집합 밖 category가 클라이언트(M3·M4) 정규화를 우회해 downstream 버킷팅에서 조용히 누락될 수 있다. 이 경로의 category 소유자(정규화-후-캐시쓰기 순서 고정)는 **캐시·딜리버리 트랙**으로 이월한다(M1 범위 밖 — M1은 순서를 고정하지 않는다). M1 자신은 어느 경로에도 강제를 걸지 않는다(pass-through 유지). 자율 구간에서 enum 강제 여부를 임의로 굳히지 않는다 — M1 오라클(§6 `test_카테고리_집합밖값_처리`)은 pass-through 보존으로 확정.
3. **직렬화 설정 위치 — 결착(round 2): M3로 미룬다.** 소비처가 공유할 `Json` 인스턴스 설정(`encodeDefaults`·`ignoreUnknownKeys` 등, 서버 향후 필드 추가 시 하위호환)은 M1이 확정하지 않는다(§1). M1 왕복 테스트는 테스트-로컬 `Json`으로 **객체 동등성**을 검증하므로(§6) 이 설정과 무관하게 결정적이다. 인스턴스 정책은 네트워크 슬라이스(M3)에서 확정한다.
4. **wire 키 계약 실측 이월 (번들 로더 M3) — 결착(round 5·6 보강).** M1 §6은 자기 왕복(encode→decode 동등성)만 검증하고 aliases를 담은 실제 외부형 JSON(번들 `terms.json` shape·서버 응답)을 디코드하지 않는다. JSON 키=프로퍼티명(camelCase) 계약(§3-1)이 실제 외부 문서에서 지켜지는지 — 특히 `aliases`가 다른 키·생략으로 default `emptyList()`에 조용히 떨어져 별칭이 소실(INV-A 위반)되지 않는지 — 의 실측은 실제 픽스처를 로드하는 **번들 로더(M3 `BundleDbSource`)** DoD로 이월한다. **이 이월은 '성공 디코드'로 만족되지 않는다**: wrong-key·키 생략 JSON도 예외 없이 성공 디코드되어 `aliases = emptyList()`를 산출하므로(별칭 소실이 loud한 에러가 아니라 silent), 성공 디코드를 오라클로 삼으면 divergence를 통과시킨다. 따라서 M3 실측은 반드시 **실제 배포 `terms.json`을 로드해 aliases를 가진 알려진 term의 aliases *내용*을 단언**하는 형태여야 한다(성공 디코드 여부는 무효 오라클). 이는 §7-1이 매핑측(`toEntity`/`toDto`) 보존을 M2로 이월한 것과 대칭인, 디코드 wire측 이월이다: M1은 계약을 *명시*(§3-1)하되 외부 픽스처로 *실측*하지는 않으므로, M3 비준자가 이 실측 상속을 인지해야 헤드라인 불변식(aliases 누락 금지)이 어느 마일스톤에서도 실측되지 않은 채 소실되는 것을 막는다.

## 8. 안전·규율
- 마일스톤 경계 **사람 비준** 없이 다음으로 넘어가지 않는다. **하네스는 push·머지·`-draft` 제거를 하지 않는다.**
- **사람 비준 체크리스트 — INV-A 실측 범위 오독 금지**: §6 green은 M1 **자기왕복**(encode→decode 동등성)만 실측한 것이다. 헤드라인 불변식 INV-A(실제 생산자 wire 키 계약에서 aliases 보존)는 M1 오라클보다 **넓게** 걸려 있고 **M1에서는 무측정**이다 — 디코드 wire측 실측은 M3(§7-4, 실제 배포 `terms.json`의 aliases *내용* 단언), 매핑측 실측은 M2(§7-1)로 이월된다. 비준자는 §6 green을 'INV-A ✓ 실측됨'으로 오독하지 말고, 이 두 이월이 각 마일스톤 DoD로 상속됐는지 확인한다.
- 네이밍은 젠더중립/여성형 기본.
- 진행 상태 정본은 ROADMAP(디스크). 이 슬라이스는 시간 안 타는 명세만.

## Open Questions

> 비준 종료(ESCALATE — cap 6 도달, Blocker 3 잔존) 시점의 **명시 이월**. 미탐색이지만 알려진 클래스를 암묵적으로 넘기지 않고, 여기에 적어서 넘긴다("본다는 걸 적어서 넘긴다").

- [x] 이번 비준 라운드의 carry-forward(미탐색이지만 알려진 클래스): **없음(빈 목록)**. 별도의 신규 이월 클래스는 기록되지 않았다.
- [x] **사람 게이트 판정 완료 (2026-07-05, eyes-open 수용)**. ESCALATE로 상신된 Blocker 3을 사람이 다음과 같이 결착했다:
  - **DR-3 (`when` else 금지 미명시)** — **닫음**: §6 `test_TermResult_when분기_전수처리`에 else 금지 명시(subtractive 수정).
  - **DR-1 (INV-A 실측 범위)** — **부분 폐쇄 + 바인딩 상속**: §6에 실제 `terms.json` 디코드 fixture 테스트(`test_실제번들_terms디코드_aliases내용보존`)를 추가해 wire 키 계약을 M1에서 실측(내용 단언, 성공 디코드 무효). 잔여 매핑측(M2)·로더 회귀(M3)는 ROADMAP M2/M3 DoD에 바인딩 상속으로 명문화.
  - **DR-2 (서버 캐시-히트 정규화 우회)** — **캐시 트랙 불변식으로 이관**: `cache-delivery-milestones.md` INV-13(정규화-후-캐시쓰기 순서) 신설, 캐시 M1(write-back)·M3(write-게이트) 불변식 목록에 바인딩.
  - **판정 성격**: 재비준을 다시 돌리지 않고(하네스 Rule 7상 DR-1/DR-2는 carry_forward 면제 안 됨 → 재ESCALATE 공산) 사람이 eyes-open으로 잔여 리스크를 수용하고 §3 M1 구현 착수를 승인했다. 자율 재비준 아님.

# M2 슬라이스 (draft) — 로컬 DB (SQLDelight)

> **성격: 비준 하네스 입력용 spec 슬라이스(`-draft.md`).** 단일 [`spec.md`](spec.md) Phase 1의 로컬 저장(1-2) 부분을 마일스톤 경계로 떼어낸 것. 진행 상태 정본은 [`../../ROADMAP.md`](../../ROADMAP.md), 상위 설계는 [`../architecture.md`](../architecture.md), 결정 근거는 [`../adr/`](../adr/).
>
> 이 문서는 **자율 구현 전 적대적 비준(완결성·모호성·테스트 가능성·정합성)의 대상**이다. 아래 §7 열린 질문은 비준이 판정할 항목이다.

- **마일스톤**: M2 · 로컬 DB
- **브랜치**: `feat/m2-local-db` (=`feat/m1-model-serialization` 위에 **스택** 분기 — main엔 아직 M1이 없으므로 M2가 M1 코드를 상속해야 빌드됨. ROADMAP 「브랜치·공개 전략」)
- **참조**: spec [1-2](spec.md), [ADR-0003](../adr/0003-local-storage.md)(SQLDelight 2.3.2 확정), [ADR-0002](../adr/0002-code-idiom-principle.md)(코틀린 관용·반응형으로 수동 재조회 제거), [M1 슬라이스](m1-model-serialization-draft.md)(§7-1 매퍼 이관·§4 INV-A), 캐시 트랙 [INV-6·INV-9·INV-12·INV-13](../cache-delivery-milestones.md)

---

## 1. 목표 (이 슬라이스가 내는 것)

`commonMain`에 로컬 영속 계층의 **스키마·타입세이프 쿼리·DTO↔엔티티 매퍼**를 세운다. M1이 세운 도메인 모델(`TermEntry`)을 SQLDelight가 생성한 DB 로우 타입과 무손실로 오간다. 이 계층은 이후 M4 `TermRepository`가 오케스트레이션할 저장/조회 **메커니즘**을 제공한다 — 저장 *정책*(fetch 3단 순서·pinning 스킵·upsert 필드 보존)은 M4 소관이고, M2는 그 정책이 얹힐 **스키마·쿼리·매퍼**만 확정한다.

SQLDelight 도입은 **버전 정렬을 빌드로 실측**하는 지점이다(ADR-0003 §Consequences): Kotlin 2.3.21 × SQLDelight 2.3.2 klib 소비가 Native에서 성립하는지를 `linkDebugFrameworkIosSimulatorArm64` green으로 확인한다(§5). UI·네트워크·오케스트레이터·Koin 전체 조립은 이 슬라이스 밖이다.

## 2. 스코프

**IN (M2):**
- **SQLDelight 배선**: 버전 카탈로그 좌표(플러그인 `app.cash.sqldelight` 2.3.2 + `runtime`/`coroutines-extensions`/`android-driver`/`native-driver` 동일 버전), `sqldelight { databases { create(...) } }` Gradle 설정.
- **스키마(`.sq`)**: `term`·`searchHistory` 테이블. ⚠️ **local-first pinning 컬럼을 처음부터** — `seenAt`(INV-6) + `schemaVersion`/`promptVersion`(INV-9), 그리고 `source`/`isBookmarked`/`createdAt`(spec 1-2). 나중에 넣으면 DB 마이그레이션.
- **타입세이프 쿼리**: 선택/삽입 + **반응형 라벨 쿼리** `bookmarked`·`recent`(`.asFlow()` 대상, ADR-0002·ADR-0003).
- **드라이버 `expect`/`actual`**: `AndroidSqliteDriver`(androidMain)/`NativeSqliteDriver`(iosMain) 주입용 팩토리.
- **DTO↔엔티티 매퍼**: `TermEntry.toEntity(...)` / `Term.toDto()` — M1 §7-1이 M2로 이관한 것. **⚠️ INV-A 매핑측 실측(DR-1 바인딩 상속)이 이 슬라이스 DoD의 필수 항목**(§4·§6).

**OUT (다른 마일스톤):**
- `BundleDbSource`(번들 로더)·`ClaudeApi`·`TermRepository`(오케스트레이터·fetch 3단·upsert 정책·pinning 스킵 로직) → **M3·M4**(spec 2-x).
- **Koin 전체 조립**(드라이버 바인딩·`termDatabase` 주입) → **M7**(spec 1-4는 최종 배선 소관). M2는 드라이버 팩토리 `expect`/`actual`와 DB 팩토리 함수만 제공하고, 그 컴파일을 green 3축으로 검증한다(런타임 DI 주입은 M7).
- **공유 `Json` 인스턴스 정책**(`encodeDefaults`·`ignoreUnknownKeys` 등 wire 정책) → **M3**(M1 §7-3 결착). M2 매퍼의 `aliases` 컬럼 인코딩은 이 wire 정책과 **독립**한 저장-내부 인코딩이다(§3-4 참조).
- **`.asFlow()` 재방출 행위 검증**(DB 변경 시 자동 재방출) → **M4/M5**(Fake 협력자·ViewModel 테스트). M2는 라벨 쿼리를 *정의*하고 결과·정렬을 직접 실행(`.executeAsList()`)으로 실측하며, 반응형 재방출은 소비 계층에서 실측한다(§6·§7-5).

## 3. 산출 명세

### 3-1. SQLDelight 스키마 (`commonMain/sqldelight/com/robin/devetym/db/DevEtym.sq`)

DB 이름 `DevEtymDatabase`, 패키지 `com.robin.devetym.db`(SQLDelight 기본 소스 디렉터리 `src/commonMain/sqldelight`).

```sql
CREATE TABLE term (
  keyword       TEXT NOT NULL PRIMARY KEY,   -- 정규화된 용어(정규화 책임은 M4)
  aliases       TEXT NOT NULL,               -- JSON 인코딩 List<String> (INV-A 보존)
  category      TEXT NOT NULL,               -- 6집합 pass-through(M1) — M2도 강제·정규화 안 함
  summary       TEXT NOT NULL,
  etymology     TEXT NOT NULL,
  namingReason  TEXT NOT NULL,
  source        TEXT NOT NULL,               -- 'BUNDLE' | 'AI'
  isBookmarked  INTEGER NOT NULL DEFAULT 0,  -- 0/1 (Boolean ↔ Long)
  createdAt     INTEGER NOT NULL,            -- 호출자 주입 시각(매퍼에 Clock 없음, §7-3)
  -- local-first pinning + 버전 태깅 (ADR-0006 빌트인, 처음부터 — 나중 마이그레이션 회피)
  seenAt        INTEGER,                     -- NOT NULL이면 pinned(INV-6). fetch가 덮지 않음(refresh만 예외 — 로직은 M4)
  schemaVersion INTEGER,                     -- INV-9 (DTO Int? ↔ INTEGER Long?)
  promptVersion TEXT                         -- INV-9. null = pre-versioning
);

CREATE TABLE searchHistory (
  keyword    TEXT NOT NULL PRIMARY KEY,
  searchedAt INTEGER NOT NULL
);
```

- 컬럼 존재 자체가 M2 락 지점이다: pinning/버전 컬럼을 **처음부터** 둬 서버 캐시 트랙(M4 소비·캐시 M4 저장측) 착수 시 DB 마이그레이션을 회피한다. **이 컬럼들은 M2에서 채우지 않되(값 쓰기는 M4) 스키마엔 존재**한다.
- `category`는 M1과 동일 **pass-through**다 — M2 스키마/매퍼는 6집합 밖 값도 거부·정규화 없이 저장·복원한다(강제는 downstream, INV-13은 서버 소관).

### 3-2. 쿼리 (같은 `.sq`)

M2는 스키마가 지지해야 할 **메커니즘 쿼리**를 정의한다. upsert *정책*(필드 보존·pinned 스킵)의 판단은 M4지만, 스키마·쿼리는 그것을 지지해야 한다.

```sql
selectAllTerms:
SELECT * FROM term;

selectTermByKeyword:
SELECT * FROM term WHERE keyword = ?;

insertOrReplaceTerm:
INSERT OR REPLACE INTO term(keyword, aliases, category, summary, etymology, namingReason,
  source, isBookmarked, createdAt, seenAt, schemaVersion, promptVersion)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

-- 반응형(Flow) — 북마크 목록 UI 자동 갱신 (ADR-0002·ADR-0003)
bookmarked:
SELECT * FROM term WHERE isBookmarked = 1 ORDER BY createdAt DESC;

insertOrReplaceSearch:
INSERT OR REPLACE INTO searchHistory(keyword, searchedAt) VALUES (?, ?);

-- 반응형(Flow) — 최근 검색 자동 갱신
recent:
SELECT * FROM searchHistory ORDER BY searchedAt DESC LIMIT :limit;

deleteSearch:
DELETE FROM searchHistory WHERE keyword = ?;

clearAllSearch:
DELETE FROM searchHistory;
```

- `bookmarked`/`recent`가 반응형 진입점이다: M4/M5가 `.asFlow().mapToList(...)`로 관찰해 **수동 재조회 없이** 목록을 갱신한다(ADR-0002). M2는 쿼리를 정의하고 `.executeAsList()`로 결과·정렬을 실측한다(재방출은 M4/M5, §2·§7-5).
- `insertOrReplaceTerm`은 M2가 제공하는 **기본 저장 메커니즘**이다. `isBookmarked`/`source`/`createdAt` 보존, pinned(`seenAt`) 로우 미덮어쓰기 같은 **정책은 M4**가 이 위에서 조건 분기로 구현한다(M2는 REPLACE 자체만 제공, 정책 판단 없음). ⚠️ `INSERT OR REPLACE`는 DELETE+INSERT라 `createdAt`을 포함한 모든 컬럼을 새 값으로 덮으므로, bookmarked(및 pinned) 로우 refresh 시 M4가 `createdAt`을 **`isBookmarked`/`source`와 함께 보존**해야 `bookmarked`(§3-1 `ORDER BY createdAt DESC`) 정렬이 새로고침마다 조용히 재정렬되지 않는다 — 이 보존 목록·정렬 안정성은 M4 DoD/ROADMAP로 상속(§7·DR-M2-2).

### 3-3. 드라이버 `expect`/`actual` (`commonMain` + `androidMain`/`iosMain`)

교체 비용을 낮게: **드라이버만 플랫폼별**, 위층은 영향 없음(ADR-0003 §Consequences Neutral).

```kotlin
// commonMain/data/local/DriverFactory.kt
expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(factory: DriverFactory): DevEtymDatabase =
    DevEtymDatabase(factory.createDriver())
```
```kotlin
// androidMain — AndroidSqliteDriver는 Context 필요(플랫폼 모듈/Koin이 M7에서 주입)
actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(DevEtymDatabase.Schema, context, "devetym.db")
}
```
```kotlin
// iosMain
actual class DriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(DevEtymDatabase.Schema, "devetym.db")
}
```
- **actual의 검증은 컴파일이다**(M1과 동일 규율): iOS actual은 `:shared:linkDebugFrameworkIosSimulatorArm64`, Android actual은 `:shared:testDebugUnitTest`/`:androidApp:assembleDebug` green으로 확인한다. 단위테스트는 이 actual을 실행하지 않고 in-memory JDBC 드라이버를 직접 쓴다(§6, 네이티브 드라이버는 JVM 단위테스트에서 미실행).
- 드라이버 아티팩트명(`android-driver`/`native-driver`)은 SQLDelight 2.x 관례 기반 — **착수 시 `app.cash.sqldelight` Maven 그룹에서 최종 확인**(ADR-0003 Implementation Notes).

### 3-4. DTO↔엔티티 매퍼 (`commonMain/data/local/TermMapper.kt`)

M1 §7-1이 M2로 이관한 매퍼. 엔티티 타입 = **SQLDelight가 `term` 테이블에서 생성한 `Term`**(별도 손수 `TermEntity` 클래스를 두지 않음 — §7-1). 이 `Term`이 spec 2-3의 `Flow<List<Term>>`가 반환하는 타입과 동일하다.

```kotlin
import com.robin.devetym.db.Term
import kotlinx.serialization.json.Json

// aliases 컬럼 전용 저장-내부 인코딩. wire 정책(M3, §7-3)과 독립 —
// List<String> 인코딩엔 encodeDefaults 모호성이 없어 결정적이다.
private val aliasesJson = Json

fun TermEntry.toEntity(
    source: Source,
    createdAt: Long,
    isBookmarked: Boolean,   // 기본값 없음 — 보존-임계(북마크). M4가 명시 전달, 누락=컴파일 에러
    seenAt: Long?,           // 기본값 없음 — 보존-임계(pinned, INV-6). M4가 명시 전달, 누락=컴파일 에러
): Term = Term(
    keyword = keyword,
    aliases = aliasesJson.encodeToString(aliases),   // List<String> → JSON TEXT (순서 보존)
    category = category,                             // pass-through (정규화 없음)
    summary = summary,
    etymology = etymology,
    namingReason = namingReason,
    source = source.name,                            // Source → 'BUNDLE'|'AI'
    isBookmarked = if (isBookmarked) 1L else 0L,
    createdAt = createdAt,
    seenAt = seenAt,
    schemaVersion = schemaVersion?.toLong(),         // Int? → INTEGER Long?
    promptVersion = promptVersion,
)

fun Term.toDto(): TermEntry = TermEntry(
    keyword = keyword,
    aliases = aliasesJson.decodeFromString(aliases), // JSON TEXT → List<String> (순서 복원)
    category = category,
    summary = summary,
    etymology = etymology,
    namingReason = namingReason,
    schemaVersion = schemaVersion?.toInt(),          // INTEGER Long? → Int?
    promptVersion = promptVersion,
)
```

- **매퍼는 비대칭이다**: `TermEntry`(DTO)엔 DB 전용 필드(`source`/`isBookmarked`/`createdAt`/`seenAt`)가 없으므로 `toEntity`가 호출자(M4 repository)로부터 주입받는다. `createdAt`/`seenAt` 시계는 **호출자 주입**(매퍼에 `Clock` 없음 → 테스트 결정성). `toDto`는 DB 전용 필드를 버리고 DTO shape만 복원한다.
- **네 DB 전용 필드에 기본값을 두지 않는다(DR-1 폐쇄)**: `source`/`createdAt`/`isBookmarked`/`seenAt` 전부 **필수 인자**다. `toDto`가 이 넷을 버리므로 DTO 왕복으로는 보존 불가 → M4 refresh는 옛 `Term`을 직접 읽어 네 값을 `toEntity(...)`로 재주입해야 하는데(read-modify-write; `INSERT OR REPLACE`=DELETE+INSERT라 부분 갱신 불가), 만약 `isBookmarked`/`seenAt`에 기본값이 있으면 M4의 재주입 누락이 컴파일 에러 없이 `isBookmarked→0`(북마크 소실)·`seenAt→null`(unpin, INV-6 위반)으로 **조용히** 덮인다. 기본값을 없애 그 누락을 컴파일 에러로 만든다(도메인 anti-silent-corruption 기조). 신규 저장은 호출부에서 `isBookmarked = false, seenAt = null`을 **명시**한다.
- **aliases·source 변환은 매퍼에 둔다**(SQLDelight 컬럼 어댑터 아님, §7-2). 그래야 INV-A 매핑측 실측이 **드라이버 없는 순수 commonTest**로 성립한다(`toEntity(...).toDto()`가 실제 JSON 인코드/디코드를 태움).
- **타입 폭 변환**: `schemaVersion` Int?(DTO)↔Long?(INTEGER), `isBookmarked` Boolean↔Long(0/1). null 보존(INV-B·INV-9). `toDto`의 `Long?→Int?`는 **Int 범위 값에서만 무손실** — Int 범위 밖 `Long?`(서버 배달 경로가 채울 수 있음)의 무손실 보장·범위 가드는 M4/캐시 트랙에 상속(INV-9).

## 4. 설계 불변식 (이 슬라이스가 반드시 지킬 것)

- **INV-A 매핑측 보존 (DR-1 바인딩 상속 — M2 필수 실측)**: `toEntity(...).toDto()` 왕복에서 `aliases`(**순서 포함**)·`category`가 무손실 보존된다. **이것이 M1 §7-1 carry-forward의 폐쇄 지점**이다 — M1 오라클은 JSON 자기왕복만 실측했으므로 DTO↔엔티티 매핑 경계 보존은 M2가 유일한 측정 지점이다. 6집합 밖 `category`도 매퍼가 거부·정규화하지 않고 그대로 보존(pass-through, M1과 동일). §6 오라클이 이를 실측한다.
- **INV-6 pinning 스키마 준비**: `seenAt` 컬럼이 스키마에 **처음부터** 존재해 "본 항목 불변"을 담을 자리를 확보한다. M2는 컬럼을 만들 뿐이며, "본 항목을 fetch가 덮지 않는" *행위*는 M4 오케스트레이션 소관이다(그때 마이그레이션 없이 값을 채운다).
- **INV-9 버전 태깅 하위호환**: `schemaVersion`/`promptVersion` 컬럼이 nullable이며 매퍼가 `null`을 왕복 보존한다(버전 이전 항목 = null). Int↔Long 변환은 **M2 소유 경로(DTO `Int?` 출처의 `toEntity`)에서** null과 값 모두 무손실이다 — 이 경계 안에서만 왕복이 성립한다. **이월(무처방)**: `toDto`는 임의 `Long?`(64비트 INTEGER)을 받으므로, 서버 캐시 트랙(M4 저장측)이 `Int.MAX_VALUE`를 넘는 `schemaVersion`을 컬럼에 직접 기록하면 `Long.toInt()`가 상위비트를 조용히 잘라 무손실이 깨진다. 현 서버 계약상 `schemaVersion`은 작은 단조 정수라 실현 가능성은 낮으므로 M2는 처방을 강제하지 않고, **`schemaVersion`을 Int 범위로 보장할(또는 `toDto`에 범위 가드를 둘) 책임을 M4/캐시 트랙에 상속**한다(§3-4).
- **ADR-0002 코틀린 관용**: iOS(SwiftData) 수동 재조회·우회 패턴을 옮기지 않는다. 반응형은 `.asFlow()`(라벨 쿼리 `bookmarked`/`recent`)로만 노출한다.

## 5. 완료 조건 (DoD) — 하네스 수렴 오라클

- 스키마·쿼리·드라이버 `expect`/`actual`·매퍼가 **Android·iOS 양쪽에서 컴파일**된다: `:shared:testDebugUnitTest` + `:androidApp:assembleDebug` + `:shared:linkDebugFrameworkIosSimulatorArm64` green(M0/M1의 3축 green 루프).
- 아래 §6 테스트가 전부 통과. **§6-A(매퍼 INV-A 실측)는 DoD의 필수 항목** — 이것이 없으면 DR-1 매핑측이 무측정으로 남는다.
- **버전 정렬을 사실로 확인(load-bearing, ADR-0003)**: SQLDelight 2.3.2 플러그인·런타임·`native-driver`가 **Kotlin 2.3.21에서 klib 소비된다는 것을 실빌드로 확인**한다 — 특히 `linkDebugFrameworkIosSimulatorArm64`가 native-driver klib를 소비해 green이어야 한다. stale 버전 하드코딩 금지(M1이 serialization 1.9.0×2.3.21을 빌드로 실측한 것과 동일 규율). 버전 카탈로그 헤더의 '빌드 확인' 표기를 이 확인의 대체물로 삼지 말 것.

## 6. 테스트 — 함수명 `test_[대상]_[조건]_[기대]`

### 6-A. 매퍼 INV-A 실측 (`commonTest/`, 드라이버 없음) — **DR-1 폐쇄, 필수**
- `test_toEntity_toDto_왕복_aliases순서_category보존` — `aliases = ["A", "B", "C"]`(다중·순서 유의미)·in-set `category`를 가진 `TermEntry`를 `toEntity(source, createdAt, isBookmarked = false, seenAt = null).toDto()` 왕복 후 **원본 DTO와 aliases(순서 포함)·category·keyword·summary·etymology·namingReason 동등**. 실제 JSON 인코드/디코드를 태우는 순수 왕복(라이브 드라이버 불요).
- `test_toEntity_toDto_왕복_빈aliases_보존` — `aliases = emptyList()` 왕복 후에도 `emptyList()`(JSON `[]` 왕복, silent 손실 없음).
- `test_toEntity_toDto_집합밖category_pass-through` — `category = "네트웤"`(오타)·`"Database"`(영문) 등 6집합 밖 값이 매퍼 왕복에서 **거부·정규화 없이 그대로 보존**(M1 pass-through 상속).
- `test_toEntity_toDto_버전필드_null과값_왕복보존` — `schemaVersion=null,promptVersion=null`(pre-versioning)과 `schemaVersion=2,promptVersion="2026-07"`(서버 배달) 두 경우 모두 Int↔Long 변환 후 무손실(INV-9·INV-B).
- `test_toEntity_DB전용필드_주입값보존` — `toEntity(source=AI, createdAt=123, isBookmarked=true, seenAt=456)`가 생성한 `Term`의 `source="AI"`·`isBookmarked=1`·`createdAt=123`·`seenAt=456` 확인(호출자 주입 필드 정확 매핑, 비대칭 매퍼).

### 6-B. 스키마·쿼리 DB 왕복 (`androidUnitTest/`, in-memory JDBC 드라이버)
> M1 fixture와 동일 배치 이유: 네이티브 드라이버는 JVM 단위테스트에서 미실행이므로, JVM에서 도는 `:shared:testDebugUnitTest`(=androidUnitTest)에서 `JdbcSqliteDriver(IN_MEMORY)` + `DevEtymDatabase.Schema.create(driver)`로 실 DB를 띄워 스키마·쿼리를 실측한다. 네이티브 actual은 §5 링크 green이 커버.

- `test_term_insertOrReplace_selectByKeyword_왕복` — 원본 `TermEntry`(DTO)를 `toEntity(source, createdAt, isBookmarked, seenAt)`로 변환한 `Term`의 필드를 `insertOrReplaceTerm` 인자에 바인딩해 넣고(**insert 값은 반드시 `toEntity` 출력에서 나와야 한다 — DTO 필드를 `insertOrReplaceTerm`에 손으로 직접 바인딩(예: `summary = 원본.summary`, `aliases = aliasesJson.encodeToString(원본.aliases)`)하는 것을 금지한다. 손바인딩하면 `toEntity`가 실행되지 않아 아래 canary가 `toEntity`측 대칭 스왑을 태우지 못하고 무측정으로 green 통과한다**), `selectTermByKeyword`로 되읽어, 되읽은 **raw `Term`의 `keyword`·`summary`·`etymology`·`namingReason`·`aliases`(raw JSON 문자열)를 `toDto()`를 거치지 않고 원본 DTO 값과 직접 컬럼 단언**한다(`aliases`는 `aliasesJson.encodeToString(원본.aliases)`와 비교). **대칭 스왑 canary(DR-M2-1 폐쇄)**: 오라클을 `toDto()` 왕복 대칭성이 아니라 raw 컬럼에 고정하므로, `toEntity`/`toDto`가 유사 TEXT 필드(예: `summary`↔`etymology`)를 대칭으로 뒤바꿔도 green을 통과하지 못한다 — 반응형 `bookmarked`/`recent` Flow 소비자(profile 지배축 5)가 `toDto` 없이 직접 읽는 raw `Term` 컬럼이 오라클에 고정된다. 스키마 컬럼 순서·타입 정합도 이 직접 단언으로 함께 실측(되읽은 raw 컬럼이 곧 저장 컬럼).
- `test_term_pinning버전컬럼_저장복원` — `seenAt`/`schemaVersion`/`promptVersion`에 값을 넣고 되읽어 보존 확인(컬럼이 처음부터 존재·nullable 동작, INV-6·INV-9 스키마 준비).
- `test_bookmarked_isBookmarked1만_createdAt내림차순` — 북마크/비북마크 혼재 삽입 후 `bookmarked().executeAsList()`가 `isBookmarked=1`만 `createdAt` 내림차순으로 반환.
- `test_recent_searchedAt내림차순_limit적용` — `searchHistory` 다건 삽입 후 `recent(limit).executeAsList()`가 `searchedAt` 내림차순·`LIMIT` 적용.
- `test_searchHistory_delete_clear` — `deleteSearch`(단건)·`clearAllSearch`(전체) 후 잔여 확인.

## 7. 열린 질문 (비준이 판정할 항목)

1. **엔티티 타입 정체성 — SQLDelight 생성 `Term` 직접 사용 (제안)**: 별도 손수 `TermEntity` data class를 두지 않고 `.sq`에서 생성된 `Term`을 엔티티로 쓴다. 이 `Term`이 spec 2-3 `Flow<List<Term>>`의 반환 타입과 동일해 이중 타입·중복 변환을 피한다. 매퍼는 `TermEntry.toEntity(): Term` / `Term.toDto(): TermEntry`. **대안**: 손수 `TermEntity` + 생성 `Term` 이중(중복). — 제안: 생성 `Term` 직접. 비준 판정 필요(spec 1-1의 `TermEntity.toDto()` 명명을 생성 타입 `Term.toDto()`로 해석하는 것의 정합성 확인).
2. **`aliases`/`source` 변환 위치 — 매퍼 함수 (제안) vs SQLDelight 컬럼 어댑터**: 제안은 변환을 매퍼에 둬 **INV-A 매핑측 실측이 드라이버 없는 순수 commonTest로 성립**(§6-A)하게 한다. 컬럼 어댑터를 쓰면 생성 `Term.aliases`가 `List<String>`으로 리치해지나(타입세이프↑), JSON 인코딩이 어댑터↔DB 경계로 이동해 **INV-A 실측이 라이브 DB를 경유**해야 한다(순수 왕복 불가). — 제안: 매퍼 변환. 비준이 어댑터를 선호하면 §6-A 오라클을 §6-B(DB 경유)로 이동해야 함을 명시.
3. **`toEntity` 서명 — DB 컨텍스트 주입(`source`/`createdAt`/`isBookmarked`/`seenAt`)**: DTO엔 없는 DB 전용 필드를 호출자(M4)가 주입한다(비대칭 매퍼). `createdAt`/`seenAt` 시계는 매퍼가 아니라 호출자가 주입(매퍼에 `Clock` 없음 → 결정성). 네 필드 모두 **기본값 없는 필수 인자**다(DR-1 폐쇄, §3-4) — 보존-임계 필드의 재주입 누락을 컴파일 에러로 강제해 M4 read-modify-write의 silent 북마크 소실·unpin을 막는다. — 이 비대칭·시계 외부화가 M4 upsert 정책과 정합하는지 확인. 대안(매퍼가 `Clock` 소유·기본 `source` 가정, 혹은 `isBookmarked`/`seenAt` 기본값 유지)은 테스트 비결정·정책 누수·silent-corruption이라 기각 제안.
4. **in-memory 테스트 의존성(`sqlite-driver` JdbcSqliteDriver) 배치**: §6-B DB 왕복 가드를 `androidUnitTest`(JVM)에만 둔다(네이티브엔 JDBC 없음) — M1 번들 fixture 테스트와 동일 배치. 필수 DR-1 실측(§6-A)은 `commonTest`(드라이버 무관)에 남긴다. — 이 이원 배치(필수=commonTest 순수, 보조=androidUnitTest DB)가 비준 기준에 맞는지 확인. `sqldelight` 테스트 좌표(`app.cash.sqldelight:sqlite-driver:2.3.2`) 추가는 착수 시 Maven 확인.
5. **반응형 재방출 검증 깊이 — M2는 `.executeAsList()`, 재방출은 M4/M5 (제안)**: M2는 `bookmarked`/`recent` 쿼리를 정의하고 결과·정렬을 직접 실행으로 실측한다. `.asFlow()`가 DB 변경 시 자동 재방출하는 *행위*는 coroutines-extensions·`runTest`가 필요하고 본질적으로 repository/VM 관심사라 M4/M5로 미룬다. — 이 경계가 ADR-0003의 "반응형 쿼리 필수"를 M2에서 과소 측정하는지 비준 판정(대안: M2에 `runTest` 스모크 1건 추가). **미월 시 명시**: `.asFlow()` 배선이 컴파일은 되나 재방출 행위는 M2에서 무측정 — M4/M5 DoD로 상속.

## 8. 안전·규율

- 마일스톤 경계 **사람 비준** 없이 다음(M3)으로 넘어가지 않는다. **하네스는 push·머지·`-draft` 제거를 하지 않는다.**
- **M1→M2 바인딩 폐쇄 확인 — INV-A 매핑측**: M1 §7-1이 M2로 이월한 매퍼(`toEntity`/`toDto`)의 `aliases`(순서)·`category` 보존 실측은 **이 슬라이스 §6-A가 폐쇄한다**(DoD 필수). 비준자는 §6-A가 DoD에 필수로 걸려 있는지, 그리고 이 도메인 헤드라인 불변식(DTO↔엔티티 aliases 누락)이 M2에서 실측됨을 확인한다. **잔여 이월은 여전히 downstream**: 번들 *로더*(M3 `BundleDbSource`)의 실제 로드 경로 회귀 가드(M1 §7-4)와 서버 read-through category 소유(INV-13, M3)는 M2 소관이 아니다.
- **브랜치 보존(defer+stacked)**: 완료 마일스톤 브랜치 삭제·로컬머지 금지(소급 PR 소스). 지우자는 지시·충동이 있어도 재확인 먼저.
- 네이밍은 젠더중립/여성형 기본.
- 진행 상태 정본은 ROADMAP(디스크). 이 슬라이스는 시간 안 타는 명세만.

## Open Questions

> 비준 종료(ESCALATE — cap 6 도달, Blocker 1 잔존) 시점의 **명시 이월**. 미탐색이지만 알려진 클래스를 암묵적으로 넘기지 않고, 여기에 적어서 넘긴다("본다는 걸 적어서 넘긴다").

- [x] 이번 비준 종료 라운드의 carry-forward(미탐색이지만 알려진 클래스): **없음(빈 목록)**. 아래 M4/캐시 트랙 상속 항목은 앞선 라운드(round 2·3)에서 이미 명시 이월된 것이며, 종료 시점 신규 이월 클래스는 기록되지 않았다. 잔존 Blocker 1(round 6 DR-1: `NativeSqliteDriver`/Native `kotlinx.serialization` 실행 정확성이 §5 링크 green으로 무측정 — 링크 green은 klib 소비=컴파일/링크만 증명하고 쿼리 결과·직렬화 왕복 정확성을 실행으로 확인하지 않음)은 carry-forward로 면제되지 않고 **사람 게이트로 상신**된다.
- [ ] (M4 상속·DR-M2-2) bookmarked(및 pinned) 로우 upsert 시 `createdAt`을 `isBookmarked`/`source`와 함께 보존 — `INSERT OR REPLACE`(DELETE+INSERT)가 `createdAt`을 새 fetch 시각으로 덮으면 `bookmarked` 목록(§3-1 `createdAt DESC`)이 새로고침마다 조용히 재정렬된다. M2 스키마는 `createdAt` 컬럼을 제공해 보존을 '가능'하게 하나 정책은 M4 소관 → M4 DoD/ROADMAP이 보존 목록에 `createdAt`을 명시하고 정렬 안정성을 하류에서 실측한다.
- [ ] (M4/캐시 트랙 상속·DR-M2-3) `toDto`의 `Long?→Int?`는 M2 소유 경로(DTO `Int?` 출처)에서 무손실 확인됨(§3-4·INV-9) — 새 M2 처방 불요. `schemaVersion`을 Int 범위로 보장(또는 `toDto` 범위 가드)할 책임은 §3-4/INV-9대로 M4/캐시 트랙 DoD에 상속되어 있으며, 각 DoD에 걸려 있는지는 사람 게이트가 추적.
- [ ] (사람 게이트) M2 §3 구현 착수 승인: _대기_.

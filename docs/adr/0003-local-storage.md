# ADR 0003: 로컬 저장 — SQLDelight vs Room KMP

## Status
Accepted (2026-07-05) — 착수 재확인 완료. **SQLDelight 2.3.2로 확정.** 근거는 버전 비호환이 아니라 **iOS Native 성숙도·최신성 리스크**다(아래 Decision). Room 3.0(room3)의 Kotlin 버전 비호환 우려는 실측으로 반박됨(References의 room3 POM 확인).

## Context
로컬 DB는 **히스토리·북마크·AI 캐시**를 담는다. SwiftData는 공유 불가라 CMP에서 갈아타야 하는 거의 유일한 코어 지점이다([`architecture.md`](../architecture.md) §4.2).

요구·제약:
- 스키마는 **단순**하다 — 엔티티 둘(`term`, `searchHistory`). 복잡한 관계·마이그레이션 부담이 낮다.
- **반응형 쿼리(`Flow`) 필수** — 데이터 변경이 UI에 자동 반영되어야 한다(수동 재조회 제거는 [ADR-0002](0002-code-idiom-principle.md) 원칙).
- **iOS 성숙도**가 중요하다 — 드라이버가 iOS(Native)에서 안정적이어야 한다.
- **버전 정렬 제약**: M0 골격이 **Kotlin 2.3.21 · SKIE 0.10.12**로 확정됐고, SKIE 0.10.12는 Kotlin 2.3.21을 상한으로 못박는다(2.4 거부, 실측). 로컬 저장 라이브러리는 이 상한과 정렬돼야 한다.
- 후보: **SQLDelight**(.sq에 SQL 먼저, 타입세이프 Kotlin API 생성) vs **Room KMP**(@Entity/@Dao 어노테이션). 드라이버만 플랫폼별(`AndroidSqliteDriver` / `NativeSqliteDriver`).

## Decision
**SQLDelight 2.3.2로 확정한다.**

착수 재확인(2026-07-05) 결과:
- **버전 호환은 양쪽 다 성립한다** — 이게 결정 기준이 *아니다*:
  - SQLDelight **2.3.2**(2026-03-16, Kotlin 2.3.10 기반)는 Kotlin 2.3.21에서 Native klib 소비가 안전(동일 마이너, 상위 컴파일러가 소비).
  - Room **3.0.0**(room3, 2026-07-01)은 릴리스 시점이 최신일 뿐 `kotlin-stdlib` **2.1.20**에 의존 → **Kotlin ~2.1.x로 빌드**됐고, 상위호환 규칙상 2.3.21 컴파일러가 그 klib을 소비할 수 있다. 즉 Room도 SKIE 상한과 **호환**된다(초기 조사의 "Room은 2.4 요구로 불가"는 오판이었고 실측으로 반박).
- **결정 기준은 iOS Native 성숙도·최신성 리스크다:**
  - Room의 KMP 정식 재작성 **3.0.0은 확정일(2026-07-05) 기준 4일 전 릴리스**다. 이 앱은 iOS Native 안정성을 명시 우선하는데, 갓 재작성된 메이저를 iOS Native 경로에 얹는 건 그 우선순위와 상충한다.
  - SQLDelight는 `NativeSqliteDriver` 트랙레코드가 길고, `.asFlow()` 반응형이 표준이며, 스키마가 사소(테이블 2개)해 SQL-first의 초기 부담이 거의 0이다.

## Consequences

### Positive (SQLDelight 채택)
- 성숙한 iOS 지원 → 리스크 낮음. 컴파일 타임 타입세이프 API.
- 반응형 쿼리(`.asFlow()`)로 목록(북마크·히스토리) UI 자동 갱신 → 우회 패턴 제거(ADR-0002).

### Negative
- SQL을 직접 써야 한다(어노테이션 대비 초기 러닝). 스키마 변경 시 `.sq`와 마이그레이션을 손으로 관리.
- **버전 정렬 확정**: 플러그인 `app.cash.sqldelight` 2.3.2 + `runtime`/`coroutines-extensions`/`android-driver`/`native-driver` 동일 버전. **Kotlin 2.3.21 유지가 전제**(SKIE 0.10.12 상한). 착수 시 실빌드로 2.3.21×2.3.2 klib 소비를 최종 확인.

### Neutral
- 어느 쪽을 택하든 **드라이버만 `expect`/`actual`**로 갈리고, Repository 위층은 영향받지 않는다(교체 비용 낮음).

## Alternatives Considered
1. **Room KMP 3.0(room3)** — 유력 대안이며 **버전 호환은 성립**한다(kotlin-stdlib 2.1.20 기반, 2.3.21 소비 가능). @Entity/@Dao 어노테이션이 Android 경험자에게 친숙하고 `Flow` 반환을 지원. 기각 사유는 호환이 아니라 **재작성 최신성 리스크**(2026-07-01 릴리스) — iOS Native 안정성 우선과 상충. (호환 라인 3.0을 두고 굳이 구 라인 2.8.4를 쓸 이유도 없다.)
2. **SQLite 드라이버 직접(raw)** — 기각. 타입세이프·마이그레이션·반응형을 직접 구현해야 해 과함.
3. **멀티플랫폼 Settings/파일 저장** — 기각. 키-값·직렬화 파일은 쿼리·정렬·부분 갱신이 필요한 목록 데이터에 부적합.

## 권고가 뒤집히는 트리거 (갱신)
- **Room KMP 재작성(3.x)이 몇 릴리스에 걸쳐 안정화**되고 iOS Native 실사용 트랙레코드가 쌓이면 → Room 재평가(어노테이션 생산성·KSP2·Android 자산 재사용). *(구 트리거 "Kotlin 2.4 해금"은 무효 — 버전은 이미 호환.)*
- Android 인력 합류 + 기존 Room 자산·습관 재사용 유인이 크면 → Room 검토.
- 반대로 스키마가 복잡해지고 세밀한 SQL 제어가 필요해지면 → SQLDelight 고수(오히려 강화).

## Implementation Notes (착수 시 좌표)
```
plugins { id("app.cash.sqldelight") version "2.3.2" }
commonMain : app.cash.sqldelight:runtime:2.3.2
             app.cash.sqldelight:coroutines-extensions:2.3.2   // .asFlow()
androidMain: app.cash.sqldelight:android-driver:2.3.2
iosMain    : app.cash.sqldelight:native-driver:2.3.2
```
- 드라이버 아티팩트명은 SQLDelight 2.x 관례 기반 — 착수 시 `app.cash.sqldelight` Maven 그룹에서 최종 확인.

## References
- 설계: [`../architecture.md`](../architecture.md) §4.2
- 관련 ADR: [ADR-0002](0002-code-idiom-principle.md)(반응형 쿼리로 수동 재조회 제거)
- SQLDelight 2.3.2 릴리스(2026-03-16): https://github.com/sqldelight/sqldelight/releases/tag/2.3.2
- SQLDelight 지원 타깃·Kotlin(2.3.10)·iOS native: https://klibs.io (SQLDelight runtime)
- Room 3.0 릴리스노트(room3 네임스페이스): https://developer.android.com/jetpack/androidx/releases/room3
- **Room 3.0.0 실측**: `room3-runtime-3.0.0.pom` → `kotlin-stdlib` 2.1.20 의존(= Kotlin ~2.1.x 빌드, 2.3.21 소비 가능 → 버전 호환 확인, 비호환 우려 반박).
- Kotlin 2.4.0 릴리스(2026-06-03, SKIE 0.10.12 상한 초과): https://blog.jetbrains.com/kotlin/2026/06/kotlin-2-4-0-released/

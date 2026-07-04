# ADR 0003: 로컬 저장 — SQLDelight vs Room KMP

## Status
Proposed — 착수 시점에 현행 버전·iOS 지원을 `klibs.io`로 재확인 후 확정한다.

## Context
로컬 DB는 **히스토리·북마크·AI 캐시**를 담는다. SwiftData는 공유 불가라 CMP에서 갈아타야 하는 거의 유일한 코어 지점이다([`architecture.md`](../architecture.md) §4.2).

요구·제약:
- 스키마는 **단순**하다 — 엔티티 둘(`Term`, `SearchHistory`). 복잡한 관계·마이그레이션 부담이 낮다.
- **반응형 쿼리(`Flow`) 필수** — 데이터 변경이 UI에 자동 반영되어야 한다(수동 재조회 제거는 [ADR-0002](0002-code-idiom-principle.md) 원칙).
- **iOS 성숙도**가 중요하다 — 드라이버가 iOS(Native)에서 안정적이어야 한다.
- 후보: **SQLDelight**(.sq에 SQL 먼저, 타입세이프 Kotlin API 생성) vs **Room KMP**(@Entity/@Dao 어노테이션). 드라이버만 플랫폼별(`AndroidSqliteDriver` / `NativeSqliteDriver`).

## Decision
**제안: SQLDelight를 우선 후보로 둔다.** 단, 이 결정은 **Proposed** 상태이며 착수 시 현행 버전·Room KMP의 iOS 성숙도를 확인해 확정한다.

근거(제안):
- KMP 트랙레코드가 길고 iOS(Native) 드라이버가 오래 검증됐다.
- `.asFlow()` 기반 **반응형 쿼리**가 표준이라 요구를 바로 만족한다.
- SQL을 직접 다뤄 생성 API가 명시적이다 — 스키마가 단순한 이 앱에 부담이 없다.

## Consequences

### Positive (SQLDelight 채택 시)
- 성숙한 iOS 지원 → 리스크 낮음. 컴파일 타임 타입세이프 API.
- 반응형 쿼리로 목록(북마크·히스토리) UI 자동 갱신 → 우회 패턴 제거.

### Negative
- SQL을 직접 써야 한다(어노테이션 대비 초기 러닝). 스키마 변경 시 `.sq`와 마이그레이션을 손으로 관리.
- **버전 정렬** — Kotlin·CMP·SQLDelight 플러그인 호환 조합을 맞춰야 한다.

### Neutral
- 어느 쪽을 택하든 **드라이버만 `expect`/`actual`**로 갈리고, Repository 위层은 영향받지 않는다(교체 비용 낮음).

## Alternatives Considered
1. **Room KMP** — 유력 대안. @Entity/@Dao 어노테이션이 Android 경험자에게 친숙하고 `Flow` 반환을 지원한다. **iOS(Native) 성숙도를 klibs.io로 확인**해 충분하면 역전 가능.
2. **SQLite 드라이버 직접(raw)** — 기각. 타입세이프·마이그레이션·반응형을 직접 구현해야 해 과함.
3. **멀티플랫폼 Settings/파일 저장** — 기각. 키-값·직렬화 파일은 쿼리·정렬·부분 갱신이 필요한 목록 데이터에 부적합.

## 권고가 뒤집히는 트리거
- 착수 시점에 **Room KMP의 iOS 지원이 충분히 성숙**함이 확인되고 어노테이션 개발 경험(생산성)을 우선하면 → Room KMP.
- 이후 Android 인력이 합류하고 기존 Room 자산·습관을 재사용할 유인이 크면 → Room KMP.
- 반대로 스키마가 복잡해지고 세밀한 SQL 제어가 필요해지면 → SQLDelight 고수.

## References
- 설계: [`../architecture.md`](../architecture.md) §4.2
- 관련 ADR: [ADR-0002](0002-code-idiom-principle.md)(반응형 쿼리로 수동 재조회 제거)
- 확인처: `klibs.io`(iOS·멀티플랫폼 지원/호환 버전) · kotlinlang.org — 착수 시 재확인 원칙

# ADR 0012: 콘텐츠 정본을 D1로 승격 — 앱 번들은 스냅샷, 파일은 생성물

## Status
**Accepted** (2026-08-25 발의 · **2026-08-25 사람 비준**) — 비준에 따라 [`../cache-delivery-milestones.md`](../cache-delivery-milestones.md) §1 **INV-11의 전단(SSOT 귀속) 조항을 대체했고**, [ADR-0006](0006-server-cache-boundary.md) Decision 5의 *"서버는 SSOT가 아니라 freshness 담당"* 문장을 갱신했다. **후단(오프라인 우선·서버 온리 금지)은 그대로 살아 있다.** 배치 = ROADMAP W 트랙 **W0c**(650 D1 시딩), W1b보다 선행.

## Context

웹 트랙 W(「크게」 — 650장 + 웹 AI)를 착수하면서 **콘텐츠 정본이 어디 있는가**가 처음으로 두 표면이 걸린 질문이 됐다.

현행 배치(실측 2026-08-25):

| 무엇 | 어디 | 무엇을 담고 있나 |
|---|---|---|
| authored 정본 | `shared/src/commonMain/composeResources/files/terms.json` (508K, 650개) | db-expand 파이프라인 산출물. 사람 검수를 거친 큐레이션 |
| 생성분 | D1 `devetym-cache`.`entries` | AI가 라이브 요청에서 만든 tail. write-once 동결(INV-2) |
| 조회 순서 | 번들 → D1 → Claude API (INV-1) | 앱은 캐시에 투명 |

이 배치는 표면이 **앱 하나일 때** 정합했다. 웹이 붙으면 셋 중 하나를 골라야 한다.

1. 웹이 `terms.json` 사본을 갖는다 → 수정 지점이 2개가 된다.
2. 웹이 앱 저장소의 `terms.json`을 빌드 입력으로 읽는다 → 2026-08-05 결정 ②의 형태. 650은 정합하나 **D1의 생성분(tail)은 웹에서 페이지가 되지 못한다** — 색인 대상에서 통째로 빠진다.
3. **정본을 D1로 올리고 파일을 생성물로 내린다** → 이 ADR.

사람 발의(2026-08-25): *"이건 캐시라고 했는데 이걸 정식 디비로 승격시켜야 하는 것 같아. 메인 역할이 바뀌는 거지, 이 디비가 메인. 앱은 설치할 때 같이 줄 수 있으니까 번들 처리."*

**핵심 관찰**: 이 제안은 오프라인 보장을 건드리지 않는다. 조회 순서(번들 → D1 → AI)도, "서버가 죽어도 앱은 산다"도 그대로다. 옮겨지는 것은 **authoring 정본의 귀속**뿐이며, INV-11이 그 한 조항을 오프라인 보장과 한 문장에 묶어 둔 탓에 전체가 재론 금지처럼 보였을 뿐이다.

## Decision

**D1 `devetym-cache`를 콘텐츠 SSOT로 승격한다. `terms.json`은 정본이 아니라 D1에서 뽑아낸 스냅샷이 된다.**

1. **정본 = D1 `entries`.** authored 650과 AI 생성분이 **같은 테이블**에 산다. 웹·앱·승격 잡이 전부 여기를 본다.

2. **`terms.json`은 생성물이다.** 파일 상단에 「generated — 직접 편집 금지」 마커를 박고, **손편집을 금지**한다. 저장소에는 계속 커밋한다(아래 5).

3. **authoring 파이프라인의 착지점이 이동한다.** `Scripts/db-expand`·`generate_db.py`의 출력 대상이 `terms.json` → D1이 된다. 검수(`--validate-only` 650 통과)는 write 게이트(INV-7 `validator.py`)와 같은 지점으로 합류한다.

4. **`origin` 컬럼을 신설한다** — `'authored' | 'generated'`. 시딩 우선순위·색인 자격([ADR-0013](0013-web-route-contract.md))·승격 잡 대상 선정이 전부 이 값으로 갈린다. 이 구분 없이는 "사람이 검수한 것"과 "AI가 방금 만든 것"이 한 테이블에서 구별되지 않는다.

5. **커밋된 스냅샷이 백업이자 리뷰 지점이다.** D1이 정본이 되면 콘텐츠 변경이 코드 리뷰를 빠져나간다 — 지금은 `terms.json` diff가 그 역할을 했다. 그래서 **모든 콘텐츠 변경 후 익스포트를 돌려 스냅샷을 커밋하는 것이 운영 의무**다. 이건 편의가 아니라 D1을 SSOT로 삼는 대가의 지불이다.

6. **시딩 충돌은 authored가 이긴다.** 현행 write 경로는 `INSERT ... ON CONFLICT DO NOTHING`(INV-4 first-write-wins)이라, **이미 AI가 생성해 둔 행이 있는 용어에 authored 650을 부으면 AI판이 남는다.** 검수를 거친 쪽이 져서는 안 된다. → **authoring path에 한해** first-write-wins의 명시적 예외를 둔다: `origin='authored'` 쓰기는 `origin='generated'` 행을 덮어쓰고, 구본은 `entry_versions`에 보존한다. **read path는 무변경** — INV-2(read path 재생성 금지)·INV-4(라이브 동시성 first-write-wins)는 그대로다. 이는 INV-5(gated-newest-wins)의 게이트를 「사람 검수」로 채우는 경우에 해당한다.

7. **조회 순서·클라이언트 계약은 무변경.** 번들 → D1 → AI(INV-1). 앱은 여전히 캐시에 투명하고(`Source` = `BUNDLE` vs 네트워크), 앱 코드는 한 줄도 바뀌지 않는다. **INV-11 후단(오프라인 우선·서버 온리 금지)은 유지된다** — 번들 스냅샷이 계속 앱에 실리기 때문이다.

8. **INV-11 전단을 다음으로 대체한다**: ~~*"서버 딜리버리는 SSOT가 아니라 freshness를 담당한다. authoring 정본은 db-expand 파이프라인 산출물이다."*~~ → **"D1이 콘텐츠 SSOT이며, 번들은 릴리스 시점의 스냅샷이다. 서버가 죽어도 앱은 번들로 살아 있어야 한다는 요구는 그대로이며, 그래서 번들은 선택이 아니라 필수다."**

## Consequences

### Positive
- **웹·앱 드리프트가 구조적으로 불가능해진다.** 두 표면이 같은 행을 읽는다. 웹은 `terms.json` 사본을 두지 않는다.
- **"수정 지점 1"이 유지된다** — 위치만 파일에서 D1로 옮겨간다(2026-08-05 결정 ②의 의도 보존).
- **INV-12 승격 플라이휠이 병합 잡에서 익스포트 1회로 축소된다.** tail을 번들로 "승격"하는 게 아니라 정본 스냅샷을 다시 뽑는 것뿐이다.
- **Later 백로그 [P1] 「번들 위 원격 오버라이드 오버레이」가 별도 기능이 아니라 이 구조의 귀결이 된다.** 번들 용어에 오류가 있으면 D1 정본을 고치고, 앱은 이미 D1을 2계층으로 보고 있다. 오버레이 매니페스트·TTL 폴링을 따로 짓던 계획이 상당 부분 불요해진다.
- 웹의 SSR 폴백이 성립할 근거가 생긴다([ADR-0013](0013-web-route-contract.md)) — 생성분이 정본 테이블에 있으므로 페이지가 될 자격이 있다.

### Negative
- **콘텐츠 변경이 코드 리뷰를 빠져나갈 수 있다.** 지금은 `terms.json` diff가 강제 리뷰 지점이었다. 완화 = Decision 5(익스포트·커밋 의무)이나, **이건 규율이지 강제 장치가 아니다.** 잊으면 정본에 백업이 없는 상태가 된다.
- **D1이 콘텐츠 단일 장애점이 된다.** 완화 = 커밋된 스냅샷으로 빌드는 재현 가능(ADR-0013 Decision 1). 그래도 authoring은 D1 가용성에 묶인다.
- **시딩 마이그레이션이 실작업이다.** 650행 + 별칭. `normalizeTermKey` 정의가 파이프라인과 Worker 사이에서 어긋나면 **페이지 수와 충돌 수가 조용히 달라진다** — 실측상 구분자 처리 하나로 별칭 1,316 vs 1,286, 교차 충돌 3 vs 4로 갈린다.
- **authored 엔트리에 실을 `prompt_version`이 없다.** 현행 스키마는 `NOT NULL`이고 값은 `"v2-pathA:" + 프롬프트 sha256` 형식인데, 650은 그 프롬프트로 만들어지지 않았다. 센티널(예: `authored:db-expand-v<N>`)을 정하고 버전 태깅(INV-9)의 의미를 두 갈래로 넓혀야 한다.
- 무료 플랜 D1 한도 안에 있으나(650행은 무의미한 크기), 이제 **한도 소진이 콘텐츠 장애로 번역된다** — 종전엔 캐시 미스로 열화될 뿐이었다.

### Neutral
- `payload` shape·클라이언트 `TermEntry` 계약 무변경. ADR-0006의 INV-1(앱 코드 무변경) 유지.
- `hit_count`(INV-12 hot 선정 입력)는 `origin='generated'`에만 의미가 있다 — authored는 이미 승격된 상태다.

## Alternatives Considered

1. **현행 유지 — `terms.json` 정본, D1은 캐시.** 웹이 사본을 갖거나 앱 저장소 빌드에 묶인다. 표면이 둘이 된 시점에 드리프트가 시간 문제가 된다. **기각.**
2. **웹이 `terms.json`을 빌드 입력으로 읽고 D1은 캐시로 유지** (2026-08-05 결정 ②의 문면). 650은 정합하나 **AI 생성분이 웹에서 영원히 페이지가 되지 못한다** — 캐시 플라이휠(INV-12)이 SEO에 아무것도 기여하지 못한다. **기각.**
3. **별도 콘텐츠 DB 신설, 캐시는 캐시로 유지.** 정본과 캐시가 2벌이 되어 동기화 부담이 생기고, "웹은 어느 쪽을 읽나"가 다시 열린다. **기각.**
4. **파일 정본 유지 + D1 단방향 미러.** 정본이 여전히 파일이므로 생성분이 정본에 못 들어오고, 결국 대안 2와 같은 결과. **기각.**

## References
- 대체 대상 조항: [`../cache-delivery-milestones.md`](../cache-delivery-milestones.md) §1 INV-11 전단 · [ADR-0006](0006-server-cache-boundary.md) Decision 5
- 함께 제안된 ADR: [ADR-0013](0013-web-route-contract.md) (웹 라우트 계약 — 이 ADR의 정본 승격을 전제로 한다)
- 관련 ADR: [ADR-0009](0009-web-framework-rendering.md)(웹 렌더링) · [ADR-0007](0007-ai-prompt-quality.md)(품질 게이트)
- D1 스키마: `~/devetym-proxy/migrations/cache/0001_term_cache.sql`
- authoring 파이프라인: `Scripts/db-expand` · `Scripts/generate_db.py` · [`../db-expand/spec.md`](../db-expand/spec.md)
- 진행 상태: [`../../ROADMAP.md`](../../ROADMAP.md) W 트랙 (배치 = **W0c**, W1b보다 선행)

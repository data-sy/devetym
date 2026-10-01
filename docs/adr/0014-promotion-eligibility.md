# ADR 0014: 승격 자격 — `branch` 필터와 번들 규격, 그리고 탈락분은 리라이트한다

## Status
**Accepted** (2026-09-02 발의 · **2026-09-02 사람 비준**) — [ADR-0013](0013-web-route-contract.md) Decision 4의 「품질 게이트가 곧 색인 게이트다」를 **구체화**한다(뒤집지 않는다). [ADR-0012](0012-content-canon-d1.md)(콘텐츠 정본 D1 승격)를 전제로 한다.

## Context

[ADR-0013](0013-web-route-contract.md) Decision 4는 승격을 이렇게 적었다:

> 승격 잡(INV-7의 `critic` = 승격 시점 게이트)을 통과하면 `origin`이 `authored`로 올라가고, 다음 빌드에서 SSG 집합에 편입되며 그때 색인된다.

W1c 착수 시점에 이 문장을 실제 데이터에 대 보니 **두 군데가 비어 있었다.** 둘 다 규범이라 코드로 메울 수 없다.

### 1. `origin='generated'`는 후보 집합이 아니다

프로덕션 D1 실측(2026-09-02):

| origin | branch | 행 |
|---|---|---|
| authored | `term_entry` | 650 |
| generated | `term_entry` | **10** |
| generated | `not_dev_term` | 13 |
| generated | `possible_typo` | 1 |

`not_dev_term`·`possible_typo` 14행은 **정상 산출물이지만 페이지가 되면 안 되는 행**이다. `origin`만으로 색인 자격을 가르면 사이트맵에 "개발 용어가 아닙니다"라는 페이지가 실린다. ADR-0013은 *"생성분도 페이지가 될 자격이 있다"*만 말하고 이 필터를 규정하지 않았다.

### 2. 정량 게이트가 승격 경로에 없다

`critic`(INV-7)은 **nuanced 전담**이다 — 프롬프트 본문이 *"길이·카테고리·null·alias 개수·keyword 형식 같은 정량 항목은 검사하지 않습니다 — 이미 코드 validator가 통과시킨 입력이므로"* 라고 명시한다. 그 전제가 승격 경로에서는 성립하지 않는다. `validator.py`는 **authoring 파이프라인의 write 시점 게이트**이고, 런타임 생성 경로(웹·앱 → 워커 → D1)는 그 게이트를 지나지 않는다.

실측(2026-09-02) — 후보 10건을 `validator.py`에 통과시키면:

```
passed 0 / failed 18
  길이 초과 15 (etymology 6 · namingReason 7 · summary 2)
  keyword 공백 2 ('service mesh' · 'dark launch')
  한글 alias 없음 1 (shedlock)
```

**원인은 프롬프트 불일치가 아니다.** 워커 프롬프트(`~/devetym-proxy/src/prompt.js:51-53`)는 번들과 **같은 범위**(summary 20\~30자 · etymology 60\~120자 · namingReason 150\~270자)를 지시한다. 다른 것은 **루프**다: db-expand 파이프라인에는 "validator 100%까지 재생성"이 있었고, 런타임에는 없어 모델이 범위를 넘겨도 그대로 정본 테이블에 앉는다.

### 3. 승격은 웹만의 일이 아니다

`origin='authored'`가 되면 그 행은 `export_bundle.py`의 추출 대상이 되어 **앱 번들 `terms.json`에도 실린다**([ADR-0012](0012-content-canon-d1.md) Decision 5). 그래서 길이·형식 규격을 정하는 쪽은 웹 색인이 아니라 **앱**이고, "웹 페이지니까 길어도 된다"는 논거는 성립하지 않는다.

## Decision

**승격 자격 = `branch='term_entry'` + 번들과 동일한 규격. 규격 위반은 탈락이 아니라 리라이트 대상이다.**

1. **후보 집합은 `origin='generated' AND branch='term_entry'`다.** 이 필터는 조회 SQL과 코드 **양쪽**에 건다(`Scripts/db-expand/promote_select.py`). 한쪽만 걸면 행을 손으로 뽑는 날 다른 쪽이 조용히 없어진다.

2. **승격 자격의 품질 기준은 번들과 같다.** `validator.py` 정량 통과 **그리고** `critic` nuanced 통과. **웹 색인용으로 기준을 따로 두지 않는다** — authored 규격이 두 벌이 되면 `validator`를 번들 전체에 못 돌리고, 앱 카드 레이아웃이 전제한 분량이 흔들린다.

3. **규격을 벗어난 후보는 버리지 않고 리라이트한다.** 사실관계·내용은 유지하고 길이·형식만 맞춰 다시 쓴 뒤, `validator` 재통과 → `critic` 순으로 간다. 이미 사용자에게 서빙된 본문이므로 "탈락 = 삭제"가 아니다.

4. **keyword 교정은 정규화 키를 보존하는 선에서만 한다.** `service mesh` → `service-mesh`는 되지만(둘 다 `servicemesh`), 키가 바뀌는 교정은 **이미 공유된 URL을 죽인다**. 키가 바뀌어야 할 만큼 틀린 keyword는 승격이 아니라 별건이다.

5. **`critic`은 수동을 유지한다**(claude.ai 임시챗 격리, 기존 런북). 후보 축적 속도 실측 = **5주에 10건(주 2건)** — 자동화가 값을 못 낸다. **재검토 트리거**: 미처리 후보가 30건을 넘거나 주 10건을 넘으면 자동화를 다시 판정한다.

6. **승격 실행에 새 경로를 만들지 않는다.** 통과분은 `merge.py` → `seed_d1.py`(충돌 규칙: authored > generated, 밀려난 본은 `entry_versions`) → `export_bundle.py` → `terms.json` 커밋 → 웹 재빌드·배포. 시딩이 authored 센티널을 다시 매기므로 승격분의 `prompt_version`을 손으로 옮기지 않는다.

## Consequences

### Positive
- **앱 번들과 웹 페이지의 규격이 한 벌로 유지된다.** `validator`를 번들 전체에 계속 돌릴 수 있다.
- **색인 게이트가 이미 있는 검수 게이트다.** 별도 SEO 품질 정책을 발명하지 않는다(ADR-0013 Decision 4의 의도 그대로).
- **새 기계가 거의 없다.** W1c의 신규 코드는 후보 선별·분류기 하나이고, 승격 이후는 W0c가 지어 둔 경로가 그대로 처리한다.
- **사이트맵이 `not_dev_term`에 오염되지 않는다.**

### Negative
- **승격이 자동이 아니라 편집 작업이다.** 정량 통과율이 0/10이라 런타임 생성물은 **사실상 전부 리라이트를 거친다.** 처리량이 사람·모델의 편집 시간에 묶인다.
- **사용자가 본 본문과 색인된 본문이 달라질 수 있다.** 리라이트가 문장을 바꾸기 때문이다(사실관계는 유지).
- **런타임 생성물의 규격 이탈은 이 ADR이 고치지 않는다.** 승격 경로 밖(앱 사용자가 지금 보는 응답)에서는 여전히 규격을 벗어난 본문이 나간다 — 이건 워커 쪽 별건으로 남는다(아래 Alternatives 2).

### Neutral
- 워커·웹 코드 무변경. 데이터와 파이프라인만 움직인다.
- `noindex` 상태의 생성분도 사람에게는 계속 정상 동작한다 — 승격은 색인 자격만 바꾼다.

## Alternatives Considered

1. **웹 색인용 기준을 따로 둔다**(길이 룰을 색인 자격에서 제외) — 오늘 10건 중 7건이 즉시 통과해 가장 싸다. 그러나 authored 규격이 두 벌이 되어 `validator`를 번들 전체에 못 돌리고, 승격분이 앱 번들로 들어가면서 카드 레이아웃 전제가 흔들린다. **기각** (2026-09-02 사람 판정).
2. **생성 시점에 고친다** — 워커에 정량 게이트 + 1회 재생성을 넣어 애초에 규격 밖 행이 앉지 않게 한다. 라이브 앱 사용자까지 함께 개선된다는 점에서 **본질에 더 가까운 해법**이지만, ⓐ 이미 앉아 있는 10건은 이 방법으로 안 고쳐지고 ⓑ 실패 시 호출이 2배가 되며 응답이 느려진다. **지금은 기각하되 열린 항목으로 남긴다** — 후보 축적이 빨라지면 리라이트 비용이 이 변경 비용을 넘는다.
3. **`critic`을 자동화하고 승격 잡을 무인화한다** — 착수 브리프가 제시한 갈래 (a). 주 2건 규모에서 무인화 비용을 회수하지 못한다. **기각**(Decision 5의 재검토 트리거로 되살릴 수 있게 남긴다).
4. **`origin`만으로 자격을 판정한다** — ADR-0013 문면을 좁게 읽은 형태. `not_dev_term` 13행이 색인 대상이 된다. **기각.**
5. **규격 위반 후보를 그냥 버린다** — 승격 잡은 단순해지지만, 사용자에게 이미 서빙된 본문을 이유 없이 영구 `noindex`로 둔다. 캐시 플라이휠이 SEO 플라이휠이 된다는 ADR-0013의 전제가 사실상 꺼진다. **기각.**

## References
- 구체화 대상: [ADR-0013](0013-web-route-contract.md) Decision 4 (품질 게이트 = 색인 게이트)
- 전제: [ADR-0012](0012-content-canon-d1.md) (D1 정본 · `origin` 컬럼 · 익스포트 의무)
- 품질 게이트 분리: [`../cache-delivery-milestones.md`](../cache-delivery-milestones.md) §1 INV-7 · §2 M5(승격 잡)
- 도구: `Scripts/db-expand/promote_select.py`(후보 선별·분류) · `validator.py`(정량) · `prompts/critic-v2.paste.md`(nuanced) · `seed_d1.py`(충돌 규칙) · `export_bundle.py`(왕복)
- 수동 라운드 절차: [`../db-expand/runbook-manual-round.md`](../db-expand/runbook-manual-round.md)
- 진행 상태: [`../../ROADMAP.md`](../../ROADMAP.md) W 트랙 W1c

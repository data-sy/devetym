# W0c 샌드박스 로드맵 — `sandbox/w0c-d1-seeding`

> **이 브랜치 작업의 SSOT.** 진행 상태·발견·백로그·오류·실측값을 전부 여기 모은다.
> **최종 갱신 2026-08-26.** 수명은 브랜치와 같다 — 흡수되면 §8 절차로 접고 이 파일을 지운다.

---

## 0. 관할 경계 — 무엇이 여기서 이기나

정본을 하나 더 만드는 문서라, **이기는 범위를 먼저 못 박는다.** 안 그러면 ROADMAP과 어긋나는 순간
다음 세션이 어느 쪽을 믿을지 모른 채 과거로 끌려간다.

| 무엇 | 정본 | 이 문서의 역할 |
|---|---|---|
| **이 브랜치의 진행 상태·발견·브랜치 내부 백로그** | **이 문서** | 여기가 이긴다 |
| W 트랙 전체 순서 (W0c→W1a→W1b→W1c), 다른 마일스톤 | [`ROADMAP.md`](ROADMAP.md) Now | 여기서 재서술하지 않는다. 포인터만 |
| 콜드 세션 진입점 | [`🤖-26-08-25-web-large-track-handoff.md`](🤖-26-08-25-web-large-track-handoff.md) | 상동 |
| 규범 — ADR·specs·architecture·INV | 각 문서 | **이 문서는 규범을 못 고친다.** 충돌을 발견하면 §4에 「ADR 개정 필요」로 올리고 사람 판정을 받는다 |
| 브랜치 **밖으로** 나가는 버그·개선 | GitHub Issues ([ADR-0008](docs/adr/0008-issue-tracking.md)) | 브랜치 안에서 나고 안에서 죽는 것만 §6에 남긴다. 밖으로 나가면 Issue를 파고 여기엔 번호만 |

**소멸 조건**: W0c가 main에 흡수되는 커밋에서 이 파일을 삭제한다. 남길 값(실측·함정)은 §8이 지정한 곳으로 옮긴다.

---

## 1. 왜 샌드박스인가

[ADR-0012](docs/adr/0012-content-canon-d1.md)로 D1이 콘텐츠 **정본**이 됐다. 승격 전이라면 시딩 실수는 캐시 오염이었지만,
승격 후에는 **정본 손상**이다. 그래서 W0c는 로컬 D1에서 짓고 실 D1은 읽기만 한다.

**⚠️ 샌드박스 브랜치는 D1을 격리하지 않는다.** 원격 `devetym-cache`는 브랜치와 무관하게 하나뿐이라
`--remote` DML 한 줄이 정본을 바로 바꾼다. 격리선은 브랜치가 아니라 실행 경로(`scripts/d1.mjs`)에 박혀 있다.

---

## 2. 환경 — 완료 (2026-08-26)

```
브랜치  sandbox/w0c-d1-seeding
├── ~/devetym         (feat/web-w0-foundation 기반)
└── ~/devetym-proxy   (main 기반 · 커밋 512df30)

데이터
├── 로컬 D1   .wrangler/state/v3/d1 · 실 DDL 적용 · tail 18행 미러
├── 픽스처    ~/devetym-proxy/test/fixtures/prod-generated-tail.json (실 D1 읽기 전용 익스포트)
└── 원격      devetym-cache — 읽기만. rows_written 0 실측

가드
├── npm run db:local       무엇이든 허용 (로컬 miniflare)
├── npm run db:read        원격. SELECT/PRAGMA/EXPLAIN/WITH만, 세미콜론 우회 차단
└── npm run db:seed:local  픽스처로 로컬 초기화
```

베이스라인: `npm test` 69/69 통과. 스키마·소스 무변경.

---

## 3. W0c 작업 — 순서와 완료 오라클

`normalizeTermKey`부터인 이유: 정의가 어긋나면 **페이지 수와 충돌 수가 조용히 달라진다.**
이걸 안 박고 시딩하면 뒤 단계가 전부 잘못된 수 위에서 검증된다.

| # | 작업 | 완료 오라클 | 상태 |
|---|---|---|---|
| **1** | **`normalizeTermKey` 정의 확정** — 파이프라인(`Scripts/`)·Worker(`src/index.js`)·웹이 한 함수를 공유 | 세 지점이 같은 입력에 같은 키를 낸다는 테스트 + **별칭 수·충돌 수를 하나의 값으로 확정** | ⬜ |
| **2** | `origin` 컬럼 마이그레이션 (`'authored' \| 'generated'`) | 로컬에 적용 후 기존 69 테스트 무회귀 · 기존 18행이 `generated`로 백필됨 | ⬜ |
| **3** | `prompt_version` 센티널 확정 (`authored:db-expand-v<N>`) | authored 행이 `NOT NULL` 제약을 통과하고, INV-9 버전 태깅이 두 갈래를 구분해 읽힌다 | ⬜ |
| **4** | authored 650 시딩 (로컬) | 로컬 entries = 650 + generated tail · 별칭 = §7 확정값 · **충돌 0건이 우연이 아님을 로그로 증명** | ⬜ |
| **5** | authored > generated 충돌 규칙 (authoring path 한정) | **충돌을 일부러 심은 뒤** authored가 이기고 구본이 `entry_versions`에 남는다 · read path 무변경(INV-2·INV-4) | ⬜ |
| **6** | 익스포트 잡 (스냅샷 커밋 의무의 실행 수단) | D1 → `terms.json` 왕복 후 **현행 파일과 의미적 동일** · 상단에 「generated — 직접 편집 금지」 마커 | ⬜ |
| **7** | 원격 적용 〔**사람 판정 필요**〕 | 로컬 1~6 전부 녹색인 뒤에만 연다. §4-A 참조 | ⬜ |

---

## 4. 열린 질문 — 결정 대기

**A. 원격 적용을 누가 어떻게 치나** 〔사람〕
로컬이 다 녹색이어도 실 D1에 붓는 순간은 되돌리기 어렵다. 선택지: (a) 사람이 직접 wrangler 실행 ·
(b) 별도 DB `devetym-cache-dev`에 먼저 붓고 확인 후 본DB (무료 플랜 10개 중 2개 사용) · (c) Claude가 가드 해제 후 실행.
→ **미정.** 1~6이 끝나기 전에는 열지 않는다.

**B. 승격 잡의 입력 필터에 `branch`가 필요하다** 〔ADR-0013 관련 · 개정 필요 가능성〕
ADR-0013은 "생성분도 페이지가 될 자격이 있다"를 전제하는데, **현 tail 18행 중 12행은 페이지가 되면 안 된다**
(`not_dev_term` 11 · `possible_typo` 1). `origin='authored'`만으로 색인 자격을 가르면 이 12행이 승격 대상에 섞인다.
→ W1c 착수 전에 판정. 규범 변경이면 새 ADR.

**C. authored `keyword`와 generated `term_key`의 형식이 다르다**
authored는 슬러그(`aa-tree`, `aba-problem`), generated는 입력 정규화형(`bash`, `symantec's`, `개발`).
§3-1이 이걸 하나로 접을지, 두 형식을 공존시킬지를 정해야 시딩 키가 정해진다.
→ **§3-1의 산출물.** 여기서 갈린다.

---

## 5. 브랜치 내부 백로그

- (없음)

---

## 6. 오류·함정 로그

브랜치 안에서 나고 안에서 죽는 것만. 밖으로 나가면 Issue를 파고 번호만 남긴다.

| 날짜 | 무엇 | 어떻게 됐나 |
|---|---|---|
| 08-26 | `~/devetym-proxy`는 Node 22 요구(`.nvmrc`), 시스템 기본은 v20.19.5 | 세션마다 `source ~/.nvm/nvm.sh && nvm use 22`. 안 하면 wrangler가 경고만 뱉고 버전 출력이 깨진다 |

---

## 7. 이 브랜치에서 실측한 값

| 항목 | 값 | 시점 |
|---|---|---|
| 원격 `devetym-cache` entries / aliases / entry_versions | **18 / 9 / 0** | 08-26 |
| generated branch 분포 | `not_dev_term` 11 · `term_entry` 6 · `possible_typo` 1 | 08-26 |
| generated `prompt_version` 종류 | 1종 — `v2-pathA:956ba44a7c48` | 08-26 |
| **authored 650 × generated 18 충돌** | **0건** (naive lowercase 정규화 기준) | 08-26 |
| generated `term_entry` 6개가 650에 없음 | `bash` `protocol` `squash` `production` `idempotency` `shedlock` | 08-26 |
| authored `keyword` 형식 | 슬러그 — `aa-tree` · `aba-problem` · `abstract-factory` | 08-26 |
| 베이스라인 테스트 | 69/69 통과 | 08-26 |

⚠️ 「충돌 0건」은 **naive 정규화 기준**이다. §3-1이 정의를 바꾸면 이 값이 바뀔 수 있다 — 확정 후 재측정해 이 표를 갱신한다.

---

## 8. 흡수 절차 — 이 문서를 지우는 방법

1. §7 실측값 중 **살아남을 것**을 핸드오프 §4「인용해도 되는 실측값」으로 옮긴다.
2. §6 함정 중 **다시 밟을 수 있는 것**을 핸드오프 §5「실측으로 얻은 함정」으로 옮긴다.
3. §4 열린 질문 중 **미해결로 남는 것**을 ROADMAP 백로그 또는 GitHub Issue로 옮긴다.
4. ROADMAP Now의 W0c를 완료 처리하고 다음 한 걸음을 W1a로 바꾼다.
5. **이 파일을 삭제한다.** 같은 커밋에서. 남겨 두면 stale 정본이 된다.

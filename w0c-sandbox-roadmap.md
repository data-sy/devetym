# W0c 샌드박스 로드맵 — `sandbox/w0c-d1-seeding`

> **이 브랜치 작업의 SSOT.** 진행 상태·발견·백로그·오류·실측값을 전부 여기 모은다.
> **최종 갱신 2026-08-26 (좌표 반전 + CI 반영).** 수명은 브랜치와 같다 — 흡수되면 §8 절차로 접고 이 파일을 지운다.

---

## ▶ 이어서 하자 — 여기부터 읽는다

**사람이 *"이어서 진행할래"* 라고 하면 이 절을 그대로 보여준다.**

```
W0c · 650 D1 시딩  ─ sandbox/w0c-d1-seeding
│
├── 환경 ─────────────────────────────────── ✅ 완료 (2026-08-26)
│   ├── 브랜치 2개          devetym · devetym-proxy            ✅
│   ├── 로컬 D1            실 DDL + 실 tail 18행 미러           ✅ 512df30
│   ├── 격리 L1 vitest     69/69 · 계정 무관                    ✅
│   ├── 격리 L2 dev --local 앱과 동일 요청 왕복 실증             ✅ bash 200·14ms
│   ├── 좌표 반전          배포·원격 스키마변경 차단             ✅ b3d9d09
│   │   └── ✗ d1 execute 구멍은 남김 — 계정 분리는 W1a로 이월    (§4-D)
│   ├── CI                push·PR에서 npm test                 ✅ b3d9d09
│   └── 문서              SSOT + ROADMAP·핸드오프·README 배너   ✅ 9385d54·d189d89
│
├── 본체 ─────────────────────────────────── ⬜ 미착수 (0/7)
│   ├── 1. normalizeTermKey 정의 확정      ⬜ ← 다음 차례
│   ├── 2. origin 컬럼 (DEFAULT 'generated' 필수)  ⬜
│   ├── 3. prompt_version 센티널           ⬜
│   ├── 4. authored 650 로컬 시딩          ⬜
│   ├── 5. authored > generated 충돌 규칙  ⬜
│   ├── 6. 익스포트 잡                     ⬜
│   └── 7. 원격 적용 〔사람 판정〕          ⬜  ※ 좌표 복원 없이는 시도 자체가 실패
│
└── 흡수 ─────────────────────────────────── ⬜ §8 절차
```

### 다음 한 걸음 = §3-1 `normalizeTermKey` 정의 확정

**왜 이것부터인가**: authored `keyword`는 슬러그(`aa-tree`)고 generated `term_key`는 입력
정규화형(`bash`·`개발`)이다(§4-C). 정의를 안 박고 시딩하면 **페이지 수와 충돌 수가 조용히 달라지고**
(별칭 1,316 vs 1,286 · 충돌 3 vs 4), 뒤 6단계가 전부 잘못된 수 위에서 검증된다.

**착수 제안 (이대로 제안하면 된다)**:
1. 현행 3지점의 정의를 실측 비교 — `~/devetym-proxy/src/index.js`의 `normalizeTermKey` ·
   `~/devetym/Scripts/`의 파이프라인 측 · 앱 `normalizeKeyword`
2. 650 + tail 18을 **양쪽 정의로 각각 돌려** 별칭 수·충돌 수를 실제로 갈라 보인다
3. 사람에게 **결정 브리핑 1건** — 구분자를 지울 것인가(`b트리` 충돌 발생) 살릴 것인가.
   트레이드오프는 「검색 유입 가능한 별칭 수」 대 「충돌로 잃는 페이지」
4. 확정 정의를 **한 함수로** 못 박고 세 지점이 같은 키를 낸다는 테스트 추가

### 이어서 하기 전 확인 (2줄)

```bash
cd ~/devetym-proxy && source ~/.nvm/nvm.sh && nvm use 22 && npm test   # 69/69 = 환경 정상
npm run db:local "SELECT COUNT(*) n FROM entries"                       # 18 = 픽스처 살아있음
```

⚠️ **`nvm use 22`를 빠뜨리면** wrangler가 조용히 이상하게 군다(§6).

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

**⚠️ 샌드박스 브랜치는 그 자체로 D1을 격리하지 않는다.** 원격 `devetym-cache`는 브랜치와 무관하게
하나뿐이다. 그래서 격리를 **명령 가드가 아니라 좌표 반전**으로 얻는다(2026-08-26 전환) — 이 브랜치의
`wrangler.toml`이 프로덕션 이름·ID를 아예 모른다. 가드는 `npx wrangler`를 직접 치면 우회되지만,
주소가 없으면 우회할 대상이 줄어든다.

### 격리 3층 — W0c는 1~2층에서 끝난다

| 층 | 무엇 | 계정 접촉 | 상태 |
|---|---|---|---|
| **L1** | `npm test` — 실 DDL + 실 워커 코드 · fetch 스텁 | 없음 | ✅ 69개 |
| **L2** | `npm run dev` (`wrangler dev --local`) — 앱과 동일한 요청으로 실제 왕복 | 없음 | ✅ 실증 |
| **L3** | 원격 샌드박스(별도 Worker·D1) | 있음(프로덕션 아님) | ⬜ **§3-1~6엔 불필요** |

**L2 실증 (08-26)**: 앱과 동일한 본문으로 `bash`→200·14ms, `idempotency`→200·2ms, `개발`→`not_dev_term`.
로컬 D1 `hit_count`가 올라가 응답원이 로컬임을 확인. 캐시 히트는 Anthropic 호출·한도 검사 **앞에서** 리턴한다.

⚠️ **L2의 유일한 누수**: 캐시 **미스**는 진짜 Anthropic API로 나간다(자리표시자 키라 401로 거절되지만
요청 자체는 나간다). W0c는 시딩 작업이라 미스를 유발할 일이 거의 없다. 완전 격리가 필요해지면 로컬 스텁.

### 좌표 반전이 막는 것 / 못 막는 것 (전부 08-26 실측)

| 명령 | 결과 |
|---|---|
| `wrangler deploy` | ✅ `devetym-proxy-sandbox`로 감. **iOS 실사용자 경로 무영향** |
| 바인딩 경유 D1(`CACHE_DB`) 원격 | ✅ 실재하지 않는 UUID라 실패 |
| `d1 migrations apply --remote devetym-cache` | ✅ 설정에서 못 찾아 거부. **되돌리기 가장 어려운 것이 막힘** |
| `d1 execute devetym-cache --remote "…"` | ❌ **통과한다** — 이름을 설정이 아니라 **계정 전체**에서 해석 |

마지막 줄이 남은 구멍이다. 완전 차단은 **계정 분리 또는 DB 스코프 API 토큰**이 필요하고,
W0c 규모엔 과하다고 판정했다(§4-D). 실수로 밟히는 경로는 아니다 —
스크립트·문서·설정 어디에도 `devetym-cache` 문자열이 남아 있지 않다.

---

## 2. 환경 — 완료 (2026-08-26)

```
브랜치  sandbox/w0c-d1-seeding
├── ~/devetym         (feat/web-w0-foundation 기반)
└── ~/devetym-proxy   (main 기반 · 512df30 환경 · b3d9d09 좌표 반전+CI)

좌표 (이 브랜치에서만)
├── Worker    devetym-proxy-sandbox
├── CACHE_DB  devetym-cache-sandbox   id=…0001 (실재하지 않음)
├── USAGE_DB  devetym-usage-sandbox   id=…0000 (실재하지 않음)
└── RATE_LIMIT id=0×32                          (실재하지 않음)
   🔁 흡수 시 PROD 값 복원 — 원본은 wrangler.toml 머리말에 있다

명령
├── npm test              L1 · 69개 · 계정 무관
├── npm run dev           L2 · wrangler dev --local · localhost:8787
├── npm run db:local "<sql>"   로컬 D1 조회
└── npm run db:reset:local     마이그레이션 + 픽스처로 초기화

데이터
├── 로컬 D1   .wrangler/state/v3/d1 · 실 DDL · tail 18행
├── 픽스처    ~/devetym-proxy/test/fixtures/prod-generated-tail.json (실 D1 읽기 전용 익스포트)
└── 원격      devetym-cache — 08-26 익스포트 1회 외 무접촉. rows_written 0 실측

CI  .github/workflows/test.yml — push·PR에서 npm test.
    Cloudflare 계정에 접속하지 않는다(miniflare가 로컬 D1·KV를 세움 → 시크릿 불요).
```

베이스라인: 좌표 반전 후 재구성한 로컬 D1 위에서 `npm test` 69/69 통과.

## 3. W0c 작업 — 순서와 완료 오라클

`normalizeTermKey`부터인 이유: 정의가 어긋나면 **페이지 수와 충돌 수가 조용히 달라진다.**
이걸 안 박고 시딩하면 뒤 단계가 전부 잘못된 수 위에서 검증된다.

| # | 작업 | 완료 오라클 | 상태 |
|---|---|---|---|
| **1** | **`normalizeTermKey` 정의 확정** — 파이프라인(`Scripts/`)·Worker(`src/index.js`)·웹이 한 함수를 공유 | 세 지점이 같은 입력에 같은 키를 낸다는 테스트 + **별칭 수·충돌 수를 하나의 값으로 확정** | ⬜ |
| **2** | `origin` 컬럼 마이그레이션 (`'authored' \| 'generated'`) — **`DEFAULT 'generated'` 필수** | 로컬 적용 후 69 테스트 무회귀 · 기존 18행이 `generated`로 백필 · **`src/index.js:450`의 7컬럼 INSERT가 수정 없이 계속 성공** | ⬜ |
| **3** | `prompt_version` 센티널 확정 (`authored:db-expand-v<N>`) | authored 행이 `NOT NULL` 제약을 통과하고, INV-9 버전 태깅이 두 갈래를 구분해 읽힌다 | ⬜ |
| **4** | authored 650 시딩 (로컬) | 로컬 entries = 650 + generated tail · 별칭 = §7 확정값 · **충돌 0건이 우연이 아님을 로그로 증명** | ⬜ |
| **5** | authored > generated 충돌 규칙 (authoring path 한정) | **충돌을 일부러 심은 뒤** authored가 이기고 구본이 `entry_versions`에 남는다 · read path 무변경(INV-2·INV-4) | ⬜ |
| **6** | 익스포트 잡 (스냅샷 커밋 의무의 실행 수단) | D1 → `terms.json` 왕복 후 **현행 파일과 의미적 동일** · 상단에 「generated — 직접 편집 금지」 마커 | ⬜ |
| **7** | 원격 적용 〔**사람 판정 필요**〕 | 로컬 1~6 전부 녹색 + **좌표를 PROD로 복원**한 뒤에만 연다. §4-A 참조 | ⬜ |

---

## 4. 열린 질문 — 결정 대기

**A. 원격 적용을 누가 어떻게 치나** 〔사람〕
로컬이 다 녹색이어도 실 D1에 붓는 순간은 되돌리기 어렵다. 선택지: (a) 사람이 직접 wrangler 실행 ·
(b) 별도 DB `devetym-cache-dev`에 먼저 붓고 확인 후 본DB (무료 플랜 10개 중 2개 사용) · (c) Claude가 가드 해제 후 실행.
→ **미정.** 1~6이 끝나기 전에는 열지 않는다. 좌표가 반전돼 있어 **복원 없이는 시도 자체가 실패**한다(§1 표).

**B. 승격 잡의 입력 필터에 `branch`가 필요하다** 〔ADR-0013 관련 · 개정 필요 가능성〕
ADR-0013은 "생성분도 페이지가 될 자격이 있다"를 전제하는데, **현 tail 18행 중 12행은 페이지가 되면 안 된다**
(`not_dev_term` 11 · `possible_typo` 1). `origin='authored'`만으로 색인 자격을 가르면 이 12행이 승격 대상에 섞인다.
→ W1c 착수 전에 판정. 규범 변경이면 새 ADR.

**C. authored `keyword`와 generated `term_key`의 형식이 다르다**
authored는 슬러그(`aa-tree`, `aba-problem`), generated는 입력 정규화형(`bash`, `symantec's`, `개발`).
§3-1이 이걸 하나로 접을지, 두 형식을 공존시킬지를 정해야 시딩 키가 정해진다.
→ **§3-1의 산출물.** 여기서 갈린다.

---

**D. 계정 분리까지 갈 것인가** 〔사람 · 08-26 시점 "가지 않음"으로 잠정 판정〕
`d1 execute`가 계정 전체에서 이름을 해석하는 구멍(§1 표 마지막 줄)은 계정 분리 또는
DB 스코프 API 토큰으로만 닫힌다. 블라스트 반경이 큰 둘(배포·스키마 변경)은 이미 막혔고,
남은 것은 손으로 프로덕션 이름을 타이핑해야 밟히는 경로다.
→ **W0c 동안은 가지 않는다.** W1a(프록시 하드닝)에서 재검토.

---

## 5. 브랜치 내부 백로그

- **[P2] Anthropic 로컬 스텁** — L2의 캐시 미스가 진짜 API로 나간다(§1 ⚠️). W0c엔 실해가 없으나
  W1a에서 웹 경로를 실측할 땐 필요해진다.

---

## 6. 오류·함정 로그

브랜치 안에서 나고 안에서 죽는 것만. 밖으로 나가면 Issue를 파고 번호만 남긴다.

| 날짜 | 무엇 | 어떻게 됐나 |
|---|---|---|
| 08-26 | `~/devetym-proxy`는 Node 22 요구(`.nvmrc`), 시스템 기본은 v20.19.5 | 세션마다 `source ~/.nvm/nvm.sh && nvm use 22`. 안 하면 wrangler가 경고만 뱉고 버전 출력이 깨진다 |
| 08-26 | `wrangler dev --local`이 캐시 히트에도 500 `server_misconfigured` | 키 검사(`src/index.js:95`)가 캐시 조회보다 **앞**에 있다. `.dev.vars`에 자리표시자 키를 넣으면 해소 — 히트 경로는 그 값을 쓰지 않는다 |
| 08-26 | **명령 가드는 벽이 아니었다** | `scripts/d1.mjs`로 `--remote` DML을 막았으나 `npx wrangler`를 직접 치면 우회된다. **좌표 반전으로 교체**(b3d9d09). 가드는 로컬 헬퍼로 축소 |
| 08-26 | **좌표 반전도 완전하지 않다** | `d1 execute`는 DB 이름을 wrangler.toml이 아니라 계정 전체에서 해석한다 — 프로덕션 이름을 손으로 주면 통과한다. `migrations`는 설정에서 찾아 거부한다. **둘의 해석 규칙이 다르다** |

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
| 베이스라인 테스트 | 69/69 통과 (좌표 반전 후 재확인) | 08-26 |
| L2 로컬 왕복 지연 | `bash` 14ms · `idempotency` 2ms · `개발` 3ms (전부 200) | 08-26 |
| 캐시 히트의 위치 | Anthropic 호출·일일 한도 검사 **앞**에서 리턴 (`src/index.js:143`) | 08-26 |
| write-back 실패 시 거동 | `waitUntil` + `catch` 삼킴 — 앱은 안 죽고 **캐시만 조용히 멈춘다** (`src/index.js:519`) | 08-26 |

⚠️ 「충돌 0건」은 **naive 정규화 기준**이다. §3-1이 정의를 바꾸면 이 값이 바뀔 수 있다 — 확정 후 재측정해 이 표를 갱신한다.

---

## 8. 흡수 절차 — 이 문서를 지우는 방법

1. §7 실측값 중 **살아남을 것**을 핸드오프 §4「인용해도 되는 실측값」으로 옮긴다.
2. §6 함정 중 **다시 밟을 수 있는 것**을 핸드오프 §5「실측으로 얻은 함정」으로 옮긴다.
3. §4 열린 질문 중 **미해결로 남는 것**을 ROADMAP 백로그 또는 GitHub Issue로 옮긴다.
4. ROADMAP Now의 W0c를 완료 처리하고 다음 한 걸음을 W1a로 바꾼다.
5. **이 파일을 삭제한다.** 같은 커밋에서. 남겨 두면 stale 정본이 된다.

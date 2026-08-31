# W0c 샌드박스 로드맵 — `sandbox/w0c-d1-seeding`

> **이 브랜치 작업의 SSOT.** 진행 상태·발견·백로그·오류·실측값을 전부 여기 모은다.
> **최종 갱신 2026-09-01 (§3-7 (b) 리허설 통과 · 프로덕션 ①에서 권한 차단).** 수명은 브랜치와 같다 — 흡수되면 §8 절차로 접고 이 파일을 지운다.

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
├── 본체 ─────────────────────────────────── 🚧 6/7 · 리허설까지 완료
│   ├── 1. normalizeTermKey 정의 확정      ✅ N1(구분자 삭제) · 세 지점 동치 실측
│   │   ├── Worker  normalizeTermKey       ✅ vitest 74/74 (69→+5)
│   │   ├── 앱      normalizeKeyword       ✅ JVM 140 · 네이티브 130 · 0 fail
│   │   │   └── + BundleDbSource 2패스 (keyword가 alias를 이김)
│   │   ├── 파이프라인 term_key.py          ✅ 케이스표 + JS 교차실행 3,398건 불일치 0
│   │   └── ✗ 앱 로컬 DB 재정규화는 남김 — 릴리스 블로커, §5-P0
│   ├── 2. origin 컬럼 (DEFAULT 'generated' 필수)  ✅ d635c3d
│   │   ├── 18행 백필 generated · NULL 0 · CHECK 2값 제약   ✅
│   │   ├── 체인 재생(.wrangler 삭제 후) 18/9/0 재현        ✅
│   │   ├── vitest 79/79 (74→+5) · read path 무변경         ✅
│   │   └── L2 왕복 bash 200·14ms·hit_count 1·origin 유지   ✅
│   ├── 3. prompt_version 센티널           ✅ 56ea002 · 0371064
│   │   ├── 형식 확정  authored:<번들 정규화 JSON sha256[:12]>  ✅ = efa8f264dc67
│   │   │   └── ✗ 손 번호(db-expand-v<N>) 기각 — Worker가 같은 문제서 이미 기각
│   │   ├── payload 대칭 (컬럼만 채우면 반쪽)              ✅ 히트 왕복 실증
│   │   ├── 접두로 두 갈래 분리 조회 · 두 축 드리프트 0    ✅
│   │   └── 번들 650 전수 shape 게이트·카테고리 통과       ✅ 부적합 0
│   ├── 4. authored 650 로컬 시딩          ✅ a710c51 · e2533ec
│   │   ├── entries 668 · aliases 1,301 · authored 650   ✅ §7 정확 일치
│   │   ├── PK 충돌 0 · 엔트리간 3 · 가려짐 88 — 로그로 증명 ✅
│   │   ├── FK 0 · 축 드리프트 0 · payload 부적합 0        ✅
│   │   ├── 멱등(2회차 불변) · 0에서 재현 1942문장         ✅
│   │   └── L2 왕복: aa-tree·AA 트리·aatree·집계 전부 히트  ✅ 2~6ms
│   ├── 5. authored > generated 충돌 규칙  ✅ b92dd1e
│   │   ├── 고치기 전 패배를 먼저 관측                     ✅ not_dev_term 생존
│   │   ├── DO UPDATE · 조건 2개(generated · 스냅샷 변경)  ✅
│   │   ├── hit_count 보존 · entry_versions 보존(INV-5)    ✅ 첫 write
│   │   ├── read path·write-back 무변경                    ✅ INV-2·INV-4
│   │   └── 실 SQLite 엔진 테스트 + 음성 대조 7속성        ✅
│   ├── 6. 익스포트 잡                     ✅ ce3ffdb · b35ef5b
│   │   ├── D1 → terms.json **바이트 동일**              ✅ 왕복 닫힘
│   │   ├── 「직접 편집 금지」를 검사로 구현              ✅ bundle-snapshot.json
│   │   ├── D1 없이 닫는 테스트(실 SQLite)               ✅
│   │   └── 음성 대조 2종(손편집·정렬 회귀)              ✅
│   └── 7. 원격 적용                        🚧 ← **여기 · 사람 1줄 남음**
│       ├── 사람 판정 (b) 선택 · 이어서 1번(배포→개명→시딩)  ✅ 09-01
│       ├── 프로덕션 백업                                    ✅ 09-01 02:05
│       ├── dev DB 리허설 (프로덕션 클론)                     ✅ 9지표 전부 일치
│       │   └── 개명 6문장까지 · 비-N1 키 0 · 왕복 바이트 동일
│       ├── ① wrangler.toml 좌표 복원  〔사람〕               ⬜ ← **막힌 지점(권한)**
│       └── ②~⑥ 0002·배포·시딩·개명·검증                     ⬜ ①만 되면 Claude가 이어감
│
└── 흡수 ─────────────────────────────────── ⬜ §8 절차
```

### 다음 한 걸음 = ① `wrangler.toml` 좌표 복원 〔사람 1줄〕 → 그다음 Claude가 ②~⑥

**막힌 것은 판단이 아니라 권한이다.** Claude가 `~/devetym-proxy/wrangler.toml`의 좌표를
프로덕션으로 되돌리려 했으나 자동 모드 안전 분류기가 편집을 차단했다(Bash·Edit 양쪽).
차단 의도가 "프로덕션으로 도구를 다시 겨누지 말 것"이라 우회하지 않았다.

**사람이 할 것 — 네 값뿐이다** (`main`과의 설정 차이는 이게 전부. diff 확인 완료):

```
name          = "devetym-proxy"                                    # -sandbox 제거
RATE_LIMIT id = "513c44bf6df942eab2262397bbec04de"
USAGE_DB      = "devetym-usage"  / "e76366e6-34e1-4a1a-8ed7-c771bd650580"
CACHE_DB      = "devetym-cache"  / "a42d4408-ff64-40d4-8a6a-c71672fd71c2"
```

되돌린 뒤 `npm test`(83) 확인. **그다음은 §3-7c의 ②~⑥을 그대로 실행하면 된다.**

**대안**: 사람이 `/config`로 권한 모드를 바꾸거나 settings에 규칙을 추가하면 Claude가 ①도 한다.

### 재개 상태 요약 (새 세션이 이것만 알면 된다)

| | |
|---|---|
| 프로덕션 현재 | entries **21** · aliases **12** · entry_versions 0 · 아직 **미적용** |
| 적용 후 기대 | entries **671** · aliases **1,304** · entry_versions **0** · authored 650 |
| 백업 | `~/devetym-d1-backup-20260901-020554.sql` |
| 스테이징 | `devetym-cache-dev` (`1be332f5-d3fb-48cb-9ba7-fc481c2a0fec`) — 시딩·개명까지 적용된 상태로 남아 있다 |
| 실행 순서 | `0002 → 배포 → 시딩 → 개명 → 검증` (§3-7c). **배포가 시딩보다 먼저인 이유는 §3-7b** |
| SQL 재생성 | `python3 Scripts/db-expand/seed_d1.py > seed.sql` · 개명 6문장은 §3-7c |

### 이어서 하기 전 확인 (2줄)

```bash
cd ~/devetym-proxy && source ~/.nvm/nvm.sh && nvm use 22 && npm test   # 83/83 = 환경 정상
npm run db:local "SELECT COUNT(*) n FROM entries"                       # 668 = 650 시딩 완료
cd ~/devetym && python3 Scripts/db-expand/test_term_key.py              # PASS = 세 지점 동치
python3 Scripts/db-expand/test_authored_version.py                      # PASS = 센티널·payload 대칭
python3 Scripts/db-expand/test_seed_d1.py                               # PASS = 시딩 회계 + 충돌 규칙(실 SQLite)
python3 Scripts/db-expand/test_export_bundle.py                         # PASS = 익스포트 왕복 + 손편집 탐지
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
| **L1** | `npm test` — 실 DDL + 실 워커 코드 · fetch 스텁 | 없음 | ✅ 83개 |
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
├── npm test              L1 · 83개 · 계정 무관
├── npm run dev           L2 · wrangler dev --local · localhost:8787
├── npm run db:local "<sql>"   로컬 D1 조회
└── npm run db:reset:local     마이그레이션 + 픽스처로 초기화

데이터
├── 로컬 D1   .wrangler/state/v3/d1 · 실 DDL · **668행**(generated 18 + authored 650)
├── 픽스처    ~/devetym-proxy/test/fixtures/prod-generated-tail.json (실 D1 읽기 전용 익스포트)
└── 원격      devetym-cache — 08-26 익스포트 1회 외 무접촉. rows_written 0 실측

CI  .github/workflows/test.yml — push·PR에서 npm test.
    Cloudflare 계정에 접속하지 않는다(miniflare가 로컬 D1·KV를 세움 → 시크릿 불요).
```

베이스라인: 좌표 반전 후 재구성한 로컬 D1 위에서 `npm test` 83/83 통과(§3-1 +5 · §3-2 +5 · §3-3 +4).

## 3. W0c 작업 — 순서와 완료 오라클

`normalizeTermKey`부터인 이유: 정의가 어긋나면 **페이지 수와 충돌 수가 조용히 달라진다.**
이걸 안 박고 시딩하면 뒤 단계가 전부 잘못된 수 위에서 검증된다.
→ **08-26 확정: N1(구분자 삭제).** 시딩 수 668 · 별칭 1,301로 고정됐다(§7).

| # | 작업 | 완료 오라클 | 상태 |
|---|---|---|---|
| **1** | **`normalizeTermKey` 정의 확정** — 파이프라인(`Scripts/`)·Worker(`src/index.js`)·앱이 한 정의를 공유 | 세 지점이 같은 입력에 같은 키를 낸다는 테스트 + **별칭 수·충돌 수를 하나의 값으로 확정** | ✅ **N1** · §7 |
| **2** | `origin` 컬럼 마이그레이션 (`'authored' \| 'generated'`) — **`DEFAULT 'generated'` 필수** | 로컬 적용 후 74 테스트 무회귀 · 기존 18행이 `generated`로 백필 · **`src/index.js:450`의 7컬럼 INSERT가 수정 없이 계속 성공** | ✅ **d635c3d** · 79/79 · §7 |
| **3** | `prompt_version` 센티널 확정 — **`authored:<번들 해시>`** (손 번호 기각) | authored 행이 `NOT NULL` 제약을 통과하고, INV-9 버전 태깅이 두 갈래를 구분해 읽힌다 | ✅ **56ea002·0371064** · 83/83 |
| **4** | authored 650 시딩 (로컬) | 로컬 entries = **668** · aliases = **1,301** (§7 확정값) · **충돌 0건이 우연이 아님을 로그로 증명** | ✅ **a710c51** · §7 |
| **5** | authored > generated 충돌 규칙 (authoring path 한정) | **충돌을 일부러 심은 뒤** authored가 이기고 구본이 `entry_versions`에 남는다 · read path 무변경(INV-2·INV-4) | ✅ **b92dd1e** · §7 |
| **6** | 익스포트 잡 (스냅샷 커밋 의무의 실행 수단) | D1 → `terms.json` 왕복 후 **바이트 동일**(의미적 동일보다 강하게 잡음) · 마커는 주석 대신 **깨지는 검사**로(`bundle-snapshot.json`) | ✅ **ce3ffdb** · §7 |
| **7** | 원격 적용 〔**사람 판정 필요**〕 | 로컬 1~6 전부 녹색 + **좌표를 PROD로 복원**한 뒤에만 연다. §4-A 참조 | ⬜ |

---

## 3-7c. 프로덕션 실행 순서 — **확정 · 사람이 쳐야 함 (2026-09-01)**

사람이 **1번(워커 배포 → 좌초 정리 → 시딩)** 을 선택했다. dev 리허설은 **개명까지 포함해 전부 통과**했다.
Claude가 여기서 멈춘 이유는 판단이 아니라 **권한**이다 — `wrangler.toml`의 좌표를 프로덕션으로
되돌리는 편집이 하네스 안전 분류기에 차단됐다(파일 편집·Bash 양쪽). 우회는 시도하지 않았다.

### 왜 이 순서인가

`0002 → 배포 → 시딩 → 개명`. 배포와 시딩 사이에만 창이 열리고, 그 창에서 생기는 generated 행은
**§3-5 충돌 규칙이 흡수한다**(authored 승리 + `entry_versions` 보존). 반대로 시딩을 먼저 하면
286개 keyword·784개 별칭이 현 워커가 조회하지 않는 키로 앉아 중복 행이 쌓인다.

### ① 좌표 복원 〔사람 — 여기가 막힌 지점〕

`~/devetym-proxy/wrangler.toml`에서 **네 값**을 되돌린다. 다른 설정 차이는 없다(diff 확인 완료):

```
name          = "devetym-proxy"                                    # ← -sandbox 제거
RATE_LIMIT id = "513c44bf6df942eab2262397bbec04de"
USAGE_DB      = "devetym-usage"  / "e76366e6-34e1-4a1a-8ed7-c771bd650580"
CACHE_DB      = "devetym-cache"  / "a42d4408-ff64-40d4-8a6a-c71672fd71c2"
```

머리말의 「좌표가 반전돼 있다」 배너도 함께 갱신한다. `npm test` 83 통과 확인.

### ② ~ ⑥ 〔이후는 Claude가 이어서 할 수 있다〕

```bash
cd ~/devetym-proxy && source ~/.nvm/nvm.sh && nvm use 22

# ② 스키마 — origin 컬럼 (기존 21행이 generated로 백필된다)
npx wrangler d1 migrations apply devetym-cache --remote

# ③ 워커 배포 — src/index.js 변경은 normalizeTermKey 한 함수뿐이다
npm run deploy

# ④ 시딩 (SQL은 이미 생성돼 있다)
npx wrangler d1 execute devetym-cache --remote --file=<seed.sql>

# ⑤ 좌초 5건 재정규화 (#21의 서버 쪽 쌍둥이)
npx wrangler d1 execute devetym-cache --remote --file=<renormalize.sql>

# ⑥ 검증 — 합격 기준: entries 671 · authored 650 · generated 21 · aliases 1,304 ·
#    entry_versions 0 · FK 0 · 드리프트 0 · 비-N1 키 0
#    그다음 실 앱 경로로 `AA 트리` → aa-tree 왕복
```

⑤의 SQL은 리허설에서 검증된 6문장이다(aliases를 먼저 옮긴다 — entries PK가 바뀌면 FK가 매달린다).

### 롤백

`~/devetym-d1-backup-20260901-020554.sql` (프로덕션 전체 · 09-01 02:05).
표적 롤백은 §3-7 7단계. 배포 롤백은 `wrangler rollback` 또는 main 재배포.

⚠️ **`devetym-cache-dev`는 남겨 뒀다** (무료 3/10). 프로덕션이 끝나면 지울지 판정한다 —
다음 스키마 변경 때 또 필요하면 유지가 싸다.

---

## 3-7. 원격 적용 런북 〔선택지 (a) — 사람이 직접 실행〕

> **비용**: (a)도 (b)도 **무료다.** D1 무료 티어는 5GB·읽기 500만행/일이고 이번 쓰기는
> **1,942행 · 약 1.5MB**다. (b)가 DB를 하나 더 쓰지만 무료 10개 중 3개가 될 뿐이다.
> **돈이 선택지를 가르지 않는다 — 위험과 손품이 가른다.**

### ⚠️ 먼저 알아야 할 것 둘

1. **원격 generated 행은 08-26 이후 늘어났을 수 있다.** 앱이 라이브라 write-back이 계속 돈다.
   §7의 18/9/0은 **08-26 스냅샷**이고 지금 값이 아니다. 늘어난 행이 650과 겹치면
   §3-5 충돌 규칙이 작동해 authored가 이기고 구본이 `entry_versions`로 간다 — 설계대로다.
   다만 **적용 후 기대 행 수가 668이 아니다.** 668이 아니라 `현재 generated 수 + 650 - 겹친 수`다.
   그래서 0단계에서 현재 값을 먼저 재고 기대값을 계산한다.

2. **좌표 복원 순서.** §8 흡수 절차가 좌표 복원을 담당하는데 §3-7이 그 앞에 있다.
   → **병합을 먼저 하기를 권한다.** 이유: `wrangler.toml` 머리말이 스스로 적어 둔 대로
   "이 diff가 PR에 뜨므로 **좌표 복원이 리뷰 항목**이 된다 — 규율이 아니라 코드 리뷰가 받친다."
   샌드박스 브랜치에서 임시로 되돌렸다 다시 뒤집는 방법은 이 안전장치를 **우회**하고,
   중간에 실패하면 좌표가 살아 있는 작업 트리가 남는다.
   → 대신 `w0c-sandbox-roadmap.md` 삭제는 7단계가 끝난 **뒤로** 미룬다.

### 0. 현재 원격 상태 측정 (읽기 전용)

```bash
cd ~/devetym-proxy && source ~/.nvm/nvm.sh && nvm use 22
npx wrangler d1 execute devetym-cache --remote --json --command \
  "SELECT (SELECT COUNT(*) FROM entries) e, (SELECT COUNT(*) FROM aliases) a,
          (SELECT COUNT(*) FROM entry_versions) v,
          (SELECT COUNT(*) FROM entries WHERE branch='term_entry') te"
```

이 값을 적어 둔다. **기대 행 수 계산의 기준이다.**

### 1. 백업 — 되돌릴 수단을 먼저 만든다

```bash
npx wrangler d1 export devetym-cache --remote \
  --output ~/devetym-d1-backup-$(date +%Y%m%d).sql
```

repo 밖에 둔다(정본 내용이라 커밋하면 리뷰 없이 콘텐츠가 들어간다).

### 2. 병합 — 좌표 복원 포함

`wrangler.toml`을 머리말의 PROD 값으로 되돌린다:

```
name          = "devetym-proxy"
RATE_LIMIT id = "513c44bf6df942eab2262397bbec04de"
USAGE_DB      = "devetym-usage"  / "e76366e6-34e1-4a1a-8ed7-c771bd650580"
CACHE_DB      = "devetym-cache"  / "a42d4408-ff64-40d4-8a6a-c71672fd71c2"
```

**PR 리뷰 항목 = 이 네 줄이 복원됐는가.** `npm test` 통과 확인 후 병합.

### 3. 스키마 — `origin` 컬럼

```bash
npx wrangler d1 migrations apply devetym-cache --remote
```

`migrations`는 이름을 **설정에서** 찾으므로 2단계가 끝나야 동작한다(§1 표).
적용 후 기존 행이 전부 `generated`로 백필됐는지 확인:

```bash
npx wrangler d1 execute devetym-cache --remote --json --command \
  "SELECT origin, COUNT(*) n FROM entries GROUP BY origin"
```

### 4. 시딩

```bash
cd ~/devetym && python3 Scripts/db-expand/seed_d1.py > /tmp/authored-seed.sql   # stderr에 회계 로그
cd ~/devetym-proxy && npx wrangler d1 execute devetym-cache --remote --file=/tmp/authored-seed.sql
```

**로그의 「entries 고유 키 650 · PK 충돌 0」을 눈으로 확인하고 나서** 붓는다.

### 5. 검증

```bash
npx wrangler d1 execute devetym-cache --remote --json --command \
 "SELECT (SELECT COUNT(*) FROM entries) e,
         (SELECT COUNT(*) FROM entries WHERE origin='authored') au,
         (SELECT COUNT(*) FROM entries WHERE origin='generated') ge,
         (SELECT COUNT(*) FROM aliases) a,
         (SELECT COUNT(*) FROM entry_versions) v,
         (SELECT COUNT(*) FROM aliases x LEFT JOIN entries y ON x.term_key=y.term_key
           WHERE y.term_key IS NULL) fk_dangling,
         (SELECT COUNT(*) FROM entries
           WHERE (origin='authored') != (prompt_version LIKE 'authored:%')) drift"
```

합격 기준: `au`=650 · `fk_dangling`=0 · `drift`=0 · `e` = (0단계 generated 수) + 650 − (겹친 수) ·
`v` = 겹친 수.

그다음 **실 앱 경로**로 왕복 확인(무영향 오라클 — 기존 iOS 앱이 계속 정상):

```bash
curl -s -X POST https://devetym-proxy.<계정>.workers.dev/v1/messages \
  -H 'Content-Type: application/json' -H 'X-Device-Id: <아무 값>' \
  -d '{"model":"claude-sonnet-5","max_tokens":1024,
       "system":[{"type":"text","text":"당신은 개발 용어의 어원과 작명 이유를 설명하는 사전 데이터 제공자입니다."}],
       "messages":[{"role":"user","content":"AA 트리"}]}'
```

한글 별칭이 `aa-tree`를 돌려주면 시딩·별칭·정규화가 원격에서 다 닫힌 것이다.

### 6. 익스포트 왕복 + 커밋

```bash
npx wrangler d1 execute devetym-cache --remote --json --command \
  "SELECT payload, prompt_version FROM entries WHERE origin='authored'" > /tmp/rows.json
cd ~/devetym && python3 Scripts/db-expand/export_bundle.py --rows /tmp/rows.json --check
```

`바이트 동일`이 나와야 한다. 나오면 원격 정본과 커밋된 스냅샷이 같다는 뜻이고,
ADR-0012의 「익스포트·커밋 의무」가 처음으로 실제로 닫힌다.

### 7. 롤백 — 전체 복원이 아니라 표적 제거

시딩은 **authored 행 추가 + 겹친 generated 덮어쓰기**만 한다. 그래서 되돌리기가 좁다:

```sql
-- (1) 덮어쓴 generated 본을 entry_versions에서 복원
INSERT INTO entries (term_key, branch, payload, prompt_version, schema_version, created_at, hit_count, origin)
SELECT v.term_key, 'term_entry', v.payload, v.prompt_version, v.schema_version, v.created_at, 0, 'generated'
  FROM entry_versions v
 WHERE v.prompt_version LIKE 'v2-pathA:%'
ON CONFLICT(term_key) DO UPDATE SET
  payload = excluded.payload, prompt_version = excluded.prompt_version, origin = 'generated';
-- ⚠️ branch는 entry_versions에 없다 — not_dev_term/possible_typo였던 행은 분기가 복원되지 않는다.
--    정확한 복원이 필요하면 1단계 백업 SQL을 쓴다.
-- (2) 시딩이 넣은 authored 행 제거
DELETE FROM aliases WHERE term_key IN (SELECT term_key FROM entries WHERE origin='authored');
DELETE FROM entries WHERE origin='authored';
```

전체 복원이 필요하면 1단계 백업 SQL. **D1 Time Travel은 쓰기 전에 현재 플랜에서
보존 기간이 얼마인지 대시보드에서 확인할 것** — 여기 적어 두고 믿을 만큼 확인된 바 없다.

### 8. 마무리

7단계까지 녹색이면 §8 절차로 `w0c-sandbox-roadmap.md`를 삭제하고 ROADMAP의 다음 걸음을
**W1a 프록시 하드닝**으로 바꾼다.

---

## 3-7b. (b) 리허설 결과 — 2026-09-01 · **dev DB 전부 녹색 · 프로덕션 직전 정지**

사람이 **(b)** 를 선택해 실행했다. dev DB(`devetym-cache-dev`, id `1be332f5-…`)를 만들고
**프로덕션을 통째로 클론**해 그 위에서 0002 + 시딩을 돌렸다.

### 실측 — 프로덕션 현재 상태 (08-26 스냅샷은 stale했다)

| | 08-26 | **09-01 실측** |
|---|---|---|
| entries | 18 | **21** (term_entry 7 · not_dev_term 13 · possible_typo 1) |
| aliases | 9 | **12** |
| entry_versions | 0 | 0 |

앱이 라이브라 write-back이 계속 돌았다. **기대 행 수는 668이 아니라 671이다.**

### 리허설 결과 (dev DB · 프로덕션 클론 위)

| 항목 | 기대 | 실측 | |
|---|---|---|---|
| entries | 671 | 671 | ✅ |
| authored / generated | 650 / 21 | 650 / 21 | ✅ |
| aliases | 1,304 | 1,304 | ✅ |
| entry_versions | 0 | 0 | ✅ (충돌 0건이라 밀린 행 없음) |
| FK dangling · 축 드리프트 · 별칭이 엔트리 가림 | 0 | 0 | ✅ |
| prompt_version 종류 | 2 | 2 | ✅ |
| 익스포트 왕복 | 바이트 동일 | **바이트 동일** | ✅ |

650×21 충돌은 **여전히 0건**이다(650 키 대조 실측). 백업 = `~/devetym-d1-backup-20260901-020554.sql`.

### 🚨 프로덕션 적용을 막는 것 — 순서 문제 (로드맵에 없던 것)

**현재 배포된 워커는 아직 옛 정규화(N0 = trim+lower)를 쓴다.** 시딩 키는 N1(구분자 삭제)이다.
이 상태로 프로덕션에 부으면:

| | 수 | 무슨 일이 나나 |
|---|---|---|
| N0==N1인 keyword | **364** | 바로 도달한다 ✅ |
| N0≠N1인 keyword | **286** | 현 워커가 **영영 조회하지 않는 키**로 앉는다. 사용자가 `aa-tree`를 찾으면 미스 → Anthropic 과금 → **`aa-tree`로 새 generated 행 생성**. 같은 용어가 두 행이 된다 |
| N0≠N1인 별칭 | **784** | 상동 |

ADR-0012 이후 이건 캐시 오염이 아니라 **정본에 모순된 행을 심는 것**이다.

→ **시딩은 N1 워커 배포와 같은 창에서, 배포 이후에 해야 한다.** 로드맵의 §3-7은 "D1에 붓는다"만
적었고 **워커 배포가 선행 조건이라는 걸 놓쳤다.** 이건 사람 판정 사항이다 — 라이브 iOS 사용자를
받는 워커를 배포하는 일이라 D1 쓰기와 별개의 결정이다.

**배포 자체의 범위는 좁다**: `main...HEAD`의 `src/index.js` 변경은 `normalizeTermKey` 한 함수뿐이다
(나머지는 테스트·설정·마이그레이션). 동작 변경도 하나다.

### 부수 발견 — 기존 프로덕션 행 5건이 N1 배포 후 좌초한다

`normalizeTermKey`가 바뀌면 옛 키로 저장된 행은 도달 불가가 된다. **이슈 [#21](https://github.com/data-sy/devetym/issues/21)의
서버 쪽 쌍둥이**이고, 지금까지 아무 데도 추적되지 않았다.

| 테이블 | 옛 키 | N1이면 |
|---|---|---|
| entries | `하네스 엔지니어링` (not_dev_term) | `하네스엔지니어링` |
| aliases | `bourne again shell` → bash | `bourneagainshell` |
| aliases | `test harness` → harness | `testharness` |
| aliases | `운영 환경` → production | `운영환경` |
| aliases | `테스트 하네스` → harness | `테스트하네스` |

피해는 작다(미스 5건 → 약 $0.12 + 중복 행). 대상 키가 이미 존재하지 않으므로 `UPDATE`로
재정규화하면 한 번에 정리된다 — 같은 창에서 처리하는 게 싸다.

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

**C. authored `keyword`와 generated `term_key`의 형식이 다르다** 〔**해소 2026-08-26**〕
authored는 슬러그(`aa-tree`), generated는 입력 정규화형(`bash`·`개발`)이었다.
→ **하나로 접었다.** 정규화를 트림에서 **구분자 삭제(N1)** 로 바꿔 `aa-tree`·`AA tree`·`aatree`가
한 키 `aatree`로 수렴한다. 두 형식 공존은 채택하지 않았다 — 실측상 접어서 **잃는 페이지가 0건**이라
공존시킬 이유가 없었다(§7).

⚠️ 착수 전 서술은 트레이드오프를 「별칭 수 대 **충돌로 잃는 페이지**」로 잡았고 `b트리` 충돌을
예상했는데, **둘 다 틀렸다.** 실측하니 entries PK 충돌은 어느 후보에서도 0건이고, 늘어난다던
별칭 충돌도 entries-우선 규칙이 흡수해 3건 그대로다. 예측한 `b트리` 충돌은 존재하지 않았다 —
`b-tree`의 별칭 `B 트리`가 그 키를 내는 유일한 출처였다.

---

**E. authored 센티널을 손 번호로 할 것인가** 〔**해소 2026-08-26 · 사람 판정**〕
ADR-0012는 `authored:db-expand-v<N>`를 **예시로** 적었다(Decision 아님 · Negative 절의 미해결 비용).
→ **내용 해시로 확정.** `authored:` + 번들 정규화 JSON sha256[:12].
같은 문제에 대해 Worker가 이미 하드코딩 상수를 기각했고(`derivePromptVersion` 주석),
한 문제에 두 답을 두면 "왜 여긴 다르지"를 나중에 아무도 답 못 한다. 잃는 것 = 값만 보고
최신 여부를 못 읽는다(커밋 로그를 봐야 한다). 650은 기계 생성 파일이라 손 번호의
장점인 가독성이 잘 안 산다.

⚠️ **origin과 prompt_version 접두는 중복 축이 아니다.** origin = 갈래(닫힌 2값),
prompt_version = 그 갈래 안의 개정(열린 집합, 접두가 네임스페이스). 교차 CHECK는 걸지 않았다 —
SQLite는 제약 추가에 테이블 재빌드가 필요한데 정본 테이블엔 과한 위험이다. 드리프트가 날 수 있는
유일한 지점(시딩)을 테스트로 잠갔다.

---

**D. 계정 분리까지 갈 것인가** 〔사람 · 08-26 시점 "가지 않음"으로 잠정 판정〕
`d1 execute`가 계정 전체에서 이름을 해석하는 구멍(§1 표 마지막 줄)은 계정 분리 또는
DB 스코프 API 토큰으로만 닫힌다. 블라스트 반경이 큰 둘(배포·스키마 변경)은 이미 막혔고,
남은 것은 손으로 프로덕션 이름을 타이핑해야 밟히는 경로다.
→ **W0c 동안은 가지 않는다.** W1a(프록시 하드닝)에서 재검토.

---

## 5. 브랜치 내부 백로그

- **[P0 · 앱 릴리스 블로커] 로컬 DB 재정규화 → [#21](https://github.com/data-sy/devetym/issues/21)**
  §3-1이 `normalizeKeyword`를 바꿔 기존 사용자 기기의 `term.keyword`·`searchHistory.keyword`가
  옛 키로 남는다(북마크가 해제된 것처럼 보이고 중복 행이 생긴다).
  **W0c는 막지 않는다** — 코드·테스트까지만 하고 릴리스를 열지 않았다. 상세는 이슈.
- **[P3 · 관찰] 번들 경로와 캐시 경로가 같은 용어에 다른 `promptVersion`을 준다** —
  번들에서 읽으면 `null`, 캐시에서 받으면 `authored:efa8f264dc67`. 앱은 이 값으로 분기하지
  않고 저장만 하므로(실측) 지금은 무해하다. "버전 다르면 갱신" 류 로직을 넣는 날 첫 지뢰가 된다.
- **[P2] `entry_versions`를 아무도 안 치운다** — §3-5가 이 테이블의 첫 write를 만들었다.
  번들 개정 1회당 650행이 쌓이고 프루닝 주체가 없다. 스냅샷 단위 태깅의 알려진 대가다
  (엔트리별 해시로 가면 줄지만 "스냅샷 X 전체 무효화"를 잃는다 — §4-E에서 그래서 안 갔다).
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
| 08-26 | **`\s`·`str.isspace()`·`trim()`은 셋 다 Kotlin `isWhitespace()`와 집합이 다르다** | 세 지점 어디서든 언어 기본 공백 판정을 쓰면 조용히 갈라진다(파이썬은 U+0085를 공백으로 보고 Kotlin은 아니다 · JS `trim()`은 BOM을 자르고 Kotlin은 아니다). 세 구현 모두 **코드포인트 집합을 명시**하고 `test_term_key.py`가 실제로 교차 실행해 고정 |
| 08-26 | **측정 없이 세운 트레이드오프가 둘 다 틀렸다** | 착수 전 서술은 「구분자를 지우면 페이지를 잃는다 · `b트리`가 충돌한다」였는데 실측하니 페이지 소실 0건 · `b트리` 충돌 없음(§4-C). **후보 정의를 실 데이터에 돌려보기 전에는 판정 브리핑을 쓰지 않는다** |
| 09-01 | **`~/devetym-proxy/node_modules`가 작업 중 사라졌다** | `vitest: command not found`로 드러남. 원인 미상 — 이 세션이 실행한 명령 중 삭제하는 것은 없었고(`rm -rf`는 `.wrangler/state/v3/d1`만), dev 로그도 정상 종료였다. `npm ci`로 복구·83/83 재확인. **package-lock.json이 있어 복구가 1분**이었다는 게 요점 |
| 08-26 | **좌표 반전도 완전하지 않다** | `d1 execute`는 DB 이름을 wrangler.toml이 아니라 계정 전체에서 해석한다 — 프로덕션 이름을 손으로 주면 통과한다. `migrations`는 설정에서 찾아 거부한다. **둘의 해석 규칙이 다르다** |

---

## 7. 이 브랜치에서 실측한 값

| 항목 | 값 | 시점 |
|---|---|---|
| 원격 `devetym-cache` entries / aliases / entry_versions | **18 / 9 / 0** | 08-26 |
| generated branch 분포 | `not_dev_term` 11 · `term_entry` 6 · `possible_typo` 1 | 08-26 |
| generated `prompt_version` 종류 | 1종 — `v2-pathA:956ba44a7c48` | 08-26 |
| **authored 650 × generated 18 충돌** | **0건** (N1 확정 정의 기준 · 재측정) | 08-26 |
| generated `term_entry` 6개가 650에 없음 | `bash` `protocol` `squash` `production` `idempotency` `shedlock` | 08-26 |
| authored `keyword` 형식 | 슬러그 — `aa-tree` · `aba-problem` · `abstract-factory` | 08-26 |
| 베이스라인 테스트 | **83/83** 통과 (§3-1 +5 · §3-2 +5 · §3-3 +4) | 08-26 |
| L2 로컬 왕복 지연 | `bash` 14ms · `idempotency` 2ms · `개발` 3ms (전부 200) | 08-26 |
| 캐시 히트의 위치 | Anthropic 호출·일일 한도 검사 **앞**에서 리턴 (`src/index.js:143`) | 08-26 |
| write-back 실패 시 거동 | `waitUntil` + `catch` 삼킴 — 앱은 안 죽고 **캐시만 조용히 멈춘다** (`src/index.js:519`) | 08-26 |
| **§3-2 후** entries origin 분포 | `generated` 18 · `authored` 0 · NULL **0** | 08-26 |
| 마이그레이션 체인 재생 (`.wrangler/state/v3/d1` 삭제 후) | entries **18** / aliases **9** / entry_versions **0** — 착수 전과 동일 | 08-26 |
| `origin` CHECK 제약 | 허용 2값 외 INSERT 거부 실측 (`'imported'` → 예외 · 행 미생성) | 08-26 |
| read path 영향 | **없음** — `src/index.js:343` SELECT가 4컬럼을 명시해 새 컬럼이 보이지 않는다 | 08-26 |
| **§3-3** 현행 번들 authored 센티널 | **`authored:efa8f264dc67`** (정규화 JSON sha256[:12]) | 08-26 |
| 원본 바이트 해시 vs 정규화 해시 | `656cd69d7a4c` ≠ `efa8f264dc67` — 포맷 민감성이 실재함 | 08-26 |
| 번들 650 shape 게이트·정본 카테고리 | **부적합 0** · 분포 기타 137 · 자료구조/동시성/패턴 103 · DB/네트워크 102 | 08-26 |
| 번들 엔트리 필드 | 6종(keyword·aliases·category·summary·etymology·namingReason) — **버전 2필드 없음** → 시딩이 실어야 함 | 08-26 |
| 앱의 `promptVersion` 사용 | **저장만·분기 없음** (`TermMapper.kt`·`LocalTermStore.kt`) — 번들 경로는 `null`, 캐시 경로는 센티널 | 08-26 |

### §3-4 시딩 실측 (09-01)

| 항목 | 값 |
|---|---|
| entries / aliases / authored | **668** / **1,301** / **650** — 착수 전 확정값과 정확 일치 |
| entries PK 충돌 | **0** (파이썬 사전 집계 + 668 = 18+650 산술로 이중 확인) |
| 별칭 삽입 대상 / 실제 착지 | 1,292 / **1,292** — 실행 시점 탈락 0 = authored×generated 충돌 0 |
| 엔트리 키에 가려진 별칭 | 88 (entries-우선 규칙이 흡수) |
| 엔트리간 별칭 충돌 | **3** — `집계`(aggregate 유지/aggregation 버림) · `분기`(branch/fork) · `샤딩`(shard/sharding) |
| 자기접힘 | 1 (`blue-green-deployment`) |
| FK dangling / 별칭이 엔트리 키를 가림 / 축 드리프트 / payload 부적합 | **0 / 0 / 0 / 0** |
| `prompt_version` 종류 | **2** — `authored:efa8f264dc67` · `v2-pathA:956ba44a7c48` |
| 멱등 | 2회차 적용 후 668/1,301 불변 |
| 0에서 재현 | `db:reset:local` → `db:seed:local` = **1,942 문장**(650+1,292) → 668/1,301 |
| L2 왕복 (실 워커) | `aa-tree` `AA 트리` `aatree` → 전부 `aa-tree` · `집계` → `aggregate` · `blue green deployment` → `blue-green-deployment` · 전부 200 · **2~6ms** |
| 무영향 확인 | `bash`(generated)는 자기 태그 `v2-pathA:` 그대로 200 |

### §3-6 익스포트 실측 (09-01)

| 항목 | 값 |
|---|---|
| D1 authored → `terms.json` | **바이트 동일** (518,789 바이트) — "의미적 동일"보다 강한 오라클 |
| 재현 형식 | `json.dumps(indent=2, ensure_ascii=False)` + 끝 개행. 이 조합만 원본과 일치 |
| 정렬 | `keyword` **원문** 사전순 — 정규화 키 정렬과 **다른 순서**다(혼동하면 매 커밋 650줄 diff) |
| 투영 | 6필드만. `schemaVersion`·`promptVersion`은 D1 전용이라 뺀다 |
| 왕복 증명 | D1 태그 `authored:efa8f264dc67` → 익스포트 → 다시 같은 센티널로 해시 |
| 테스트 경로 | **D1 불요** — terms.json → 시딩 SQL → 실 SQLite → 익스포트 → 바이트 비교 |
| 음성 대조 (1) | terms.json 한 글자 손편집 → 스냅샷 불일치 탐지 (`efa8f2…` vs `f16164…`) |
| 음성 대조 (2) | 익스포트 정렬을 정규화 키로 교체 → 바이트 불일치 + 센티널 불일치 2건 |

**「직접 편집 금지」를 어디 뒀나** — JSON 배열엔 주석을 못 달고, 배열 첫 원소로 넣으면 앱이
strict 디코드에서 깨진다(필수 5필드). 그래서 마커를 **읽히는 글자가 아니라 깨지는 검사**로
만들었다: `Scripts/db-expand/bundle-snapshot.json`이 센티널을 기록하고 테스트가 대조한다.
앱 번들(`composeResources`)에 넣지 않은 이유 = 앱이 안 쓰는 파일을 배포에 싣지 않으려고.

⚠️ 이 장치가 잡는 것은 **사고**지 고의가 아니다. 둘 다 고치면 통과한다 — 그건 우회이지 실수가 아니다.

### §3-5 충돌 규칙 실측 (09-01) — 650과 겹치는 generated 3종을 일부러 심고

| 심은 것 | 고치기 전 (`DO NOTHING`) | 고친 뒤 (`DO UPDATE`) |
|---|---|---|
| `aatree` term_entry (AI 오본·hits 42) | generated 생존 — **검수본이 짐** | authored · term_entry · **hits 42 보존** |
| `abaproblem` not_dev_term (hits 7) | 생존 → 실 워커가 `return_not_dev_term` 응답 | authored · term_entry · hits 7 |
| `abstractfactory` possible_typo (hits 3) | 생존 → `return_possible_typo` | authored · term_entry · hits 3 |

- `entry_versions` **3행**에 구본 보존 — 원래 태그 `v2-pathA:956ba44a7c48` 그대로. **이 테이블의 첫 write**
- 총계 불변: entries 668 · aliases 1,301 · generated 18 (심은 3건이 650에 흡수됨)
- **멱등**: 2회차 적용에도 `entry_versions` 3 유지 (조건이 틀리면 실행마다 650씩 자란다)
- **스냅샷 교체**: 번들 한 글자 수정 → 태그 `efa8f264dc67`→`3570679b6083`, 650행 전부 갱신 ·
  구본 650 보존 · 편집 내용 실제 반영. **센티널의 존재 이유가 여기서 확인됐다**
- 실 워커: `aba-problem`·`ABA 문제`→검수 요약 · `abstract-factory`→정상 엔트리 · `bash`(generated) 무영향

⚠️ **고치기 전 상태를 먼저 관측한 것이 요점이다.** 실측상 실제 충돌은 0건이라 "고쳤다"를
증명할 자연 사례가 없었다 — 심지 않으면 `DO NOTHING`과 `DO UPDATE`가 같은 결과를 낸다.

**N1 정의의 값이 여기서 실물로 확인됐다** — `aatree`(구분자 없는 표기)와 `AA 트리`(한글 별칭)가
같은 행에 도달한다. N0였다면 앞의 것은 미스였다(§7 세 후보 비교의 272건).


### §3-1 확정값 — 시딩 수의 근거 (실제 구현으로 측정, 08-26)

정의 **N1**: 구분자(공백류 ∪ {`-`, `_`})를 **내부까지 전부 삭제**한 뒤 lowercase.
`aa-tree` · `AA tree` · `aatree` → `aatree`.

| 항목 | 값 | 비고 |
|---|---|---|
| **entries 행** | **668** | authored 650 + generated 18 |
| entries PK 충돌 | **0** | 0이 아니면 행이 사라진다 — 이 정의가 안전한 이유 |
| **aliases 행** | **1,301** | entries에 이미 있는 키는 별칭으로 안 넣는다(`insertAliases`) |
| 엔트리간 별칭 충돌 | **3** | `집계`{aggregate,aggregation} · `분기`{branch,fork} · `샤딩`{shard,sharding} — 전부 authored 내부, first-write-wins가 흡수 |
| 그중 authored×generated | **0** | |
| 자기접힘 | 1 | `blue-green-deployment`의 별칭 2개가 `블루그린배포` 한 키로 — 같은 엔트리라 무해 |
| 실 번들 650 정규화 keyword 고유성 | **650/650** | 구분자 변이 중복 없음 |

**왜 N1인가 — 세 후보 실측 비교**

| | N0 `trim+lower` | **N1 삭제** | N2 `구분자→공백` |
|---|---|---|---|
| 표기변이 도달 (858개) | 306 | **858** | 577 |
| 무공백 한글 별칭 도달 | 0/609 | **609/609** | 0/609 |
| entries PK 충돌 | 0 | **0** | 0 |
| 기존 generated 18행 키 이동 | — | **0** | 0 |

N0에서는 authored 650 중 **272건이 자기 자신의 공백 표기로 도달 불가**였다(`aa tree` → miss).
N1은 그 272건을 전부 회수하면서 **아무 페이지도 잃지 않는다.**

**세 지점 동치 오라클**

| 지점 | 파일 | 실행 결과 |
|---|---|---|
| Worker | `~/devetym-proxy/src/index.js` `normalizeTermKey` | vitest 74/74 (69→+5) |
| 앱 | `shared/.../data/AppJson.kt` `normalizeKeyword` | JVM 140 · **네이티브(iosSimulatorArm64) 130** · 0 fail |
| 파이프라인 | `Scripts/db-expand/term_key.py` `normalize_term_key` | 케이스표 + **JS 교차 실행 3,398건 · 불일치 0** |

`test_term_key.py`는 미러링한 표만 비교하는 게 아니라 실 번들 650의 keyword·aliases·표기변이와
유니코드 경계 문자를 **파이썬과 JS 구현에 실제로 통과시켜** 키를 바이트 비교한다.
Kotlin 축은 같은 케이스표를 JVM·네이티브 양쪽에서 실행한다(`isWhitespace`·`lowercase`가 엔진 의존이라 필요).

**함께 바뀐 것**
- `BundleDbSource` 인덱스가 **2패스**가 됐다 — keyword 전량 먼저, 그다음 alias.
  서버 `lookupCache`의 entries-우선 규칙과 같아졌다. 1패스면 `cache-aside`의 별칭 `lazy loading`이
  엔트리 `lazy-loading`의 자기 keyword 키를 선점한다(실측 1건 → 2패스로 0건).
- `validator.check_keyword_unique`·`merge.py` 충돌 검사가 **정규화 키 기준**이 됐다.
  원문 비교면 `aa-tree`와 `aa_tree`가 둘 다 통과해 D1 PK에서 조용히 하나가 버려진다.

---

## 8. 흡수 절차 — 이 문서를 지우는 방법

1. §7 실측값 중 **살아남을 것**을 핸드오프 §4「인용해도 되는 실측값」으로 옮긴다.
2. §6 함정 중 **다시 밟을 수 있는 것**을 핸드오프 §5「실측으로 얻은 함정」으로 옮긴다.
3. §4 열린 질문 중 **미해결로 남는 것**을 ROADMAP 백로그 또는 GitHub Issue로 옮긴다.
4. ROADMAP Now의 W0c를 완료 처리하고 다음 한 걸음을 W1a로 바꾼다.
5. **이 파일을 삭제한다.** 같은 커밋에서. 남겨 두면 stale 정본이 된다.

# 🤖 웹 트랙 W — **W0c 완료 · W1a 코드 완료(브랜치) · 다음 = 사람 4줄 → 배포 → W1b**

> **콜드 세션 시작점.** 사람이 *"뭐 하고 있었어? 이어서 하자"* 라고 물으면 **이 문서로 답한다.**
> 상태 정본은 [`ROADMAP.md`](ROADMAP.md) Now의 「▶ 재개 지점」 — 충돌하면 ROADMAP이 이긴다.
> **최종 갱신 2026-09-01 밤 (W1a 코드 완료 — `devetym-proxy` `feat/w1a-proxy-hardening`, 미배포).** 선행 핸드오프·규모 판정 브리프는 2026-08-25 서류 정돈에서 삭제했다(판정 결과는 §1·§2에 흡수).

**한 줄**: <https://devetym.com> 이 라이브다. **W0c가 끝났고(650 정본이 프로덕션 D1에 있다) W1a의 코드도 끝났다 — 다만 브랜치에 있고 배포되지 않았다.** W1a에서 코드로 닫히는 것(CORS·3층 한도·표면 분리·프롬프트 이전·관측)은 전부 됐고 테스트 83→131. **남은 것은 사람만 할 수 있는 4줄**(Turnstile 위젯·워크스페이스 키·시크릿 주입·배포+앱 실측)이다. **웹 본체(650장·검색·AI)는 아직 한 줄도 없다.**

---

## 0. 새 세션이 *"이어서 하자"* 를 들었을 때 — **읽고 바로 이 말을 하면 된다**

> **W0c는 끝났고(프로덕션 D1에 650) W1a는 코드까지 끝났다** — `devetym-proxy`의
> `feat/w1a-proxy-hardening` 브랜치에 있고 **배포되지 않았다.** 다음은 코드가 아니라
> **사람 4줄**(§2-W1a의 「남은 것」)이고, 그게 끝나면 바로 **W1b 웹 본체**다.

### 지금 상태 한 눈

| | |
|---|---|
| W0c | **✅ 7/7 완료** — 키·origin·센티널·시딩·충돌규칙·익스포트·**원격 적용** |
| 프로덕션 D1 | entries **671** · authored **650** · generated **21** · aliases **1,304** · entry_versions 0 |
| 배포 워커 | `devetym-proxy` `c9ccd526` — N1 정규화 |
| 닫힘 증명 | 익스포트가 커밋된 `terms.json`과 **바이트 동일** · 키 1,975개 전부 N1 |
| 백업 | `~/devetym-d1-backup-20260901-020554.sql` (적용 **전** 상태) |
| 스테이징 | `devetym-cache-dev` — 리허설용, 남겨 뒀다 |
| W1a | **코드 ✅ / 배포 ⬜** — `feat/w1a-proxy-hardening` 2커밋. 테스트 **131** 통과 |
| W1a 로컬 오라클 | dry-run · vitest 131 · 마이그레이션 2 DB · 프롬프트 바이트 대조 — **4축 녹색** |
| 프롬프트 정본 | 앱 → 워커 이전 완료. `sha256[:12]=956ba44a7c48` **불변**(프로덕션 태그와 동일) |

### 남은 일 — 전부 사람 손이다

**① 🙋 `main` 푸시** — `sandbox/w0c-d1-seeding` 은 `devetym`·`devetym-proxy` 양쪽에서
**2026-09-01 `main`에 병합됐고** 브랜치 전용 SSOT도 같은 커밋에서 삭제했다. 남은 건 푸시뿐이다.
**브랜치는 지우지 않는다**(보존 규율). W1a 브랜치도 같이 밀면 된다.

**② 🙋 W1a 마무리 4줄** — 아래 §2-W1a의 「남은 것」. 대시보드 발급 2건 + 시크릿 주입 + 배포/실측.

**③ W1b 웹 본체** — ②가 닫히면 바로. 여기부터 다시 Claude 자율.

⚠️ **`wrangler.toml`의 좌표는 이제 프로덕션이다.** `npm run deploy`·`d1 migrations apply --remote`가
라이브에 닿는다. 샌드박스 격리는 2026-09-01에 해제됐다.

### W1a를 켜는 순서 — 바꾸지 말 것

```
① Cloudflare 대시보드 → Turnstile → Add widget (도메인 devetym.com)   〔사람〕
② Anthropic Console → Workspaces → 새 워크스페이스 + spend limit → API key  〔사람〕
③ cd ~/devetym-proxy && source ~/.nvm/nvm.sh && nvm use 22
   openssl rand -base64 32 | npx wrangler secret put WEB_COOKIE_SECRET
   npx wrangler secret put TURNSTILE_SECRET        # ①의 secret key
   npx wrangler secret put ANTHROPIC_API_KEY_WEB   # ②의 키
④ npx wrangler d1 migrations apply devetym-usage --remote   # surface·outcome 컬럼
⑤ npm run deploy
⑥ 실 iOS 앱에서 새 용어 1건 생성 — 정상 성공해야 한다
```

**⑤가 ⑥보다 먼저인 건 당연하지만, ⑥ 없이는 W1a가 끝난 게 아니다.** 프롬프트 소유권이
서버로 옮겨졌으므로 **앱이 받는 응답 내용이 바뀔 수 있는 유일한 창**이 여기다. 이식이
바이트 동일이라 안 바뀌어야 정상이고, 그걸 눈으로 확인하는 단계다.

**시크릿 없이 ⑤만 해도 안전하다** — 세 층이 꺼진 채 통과하고 앱 경로는 종전과 같다.
다만 웹을 공개하기 전에 최소 `WEB_COOKIE_SECRET`은 있어야 브라우저 층이 실재한다.

**롤백**: `git revert` 없이 이전 배포로 되돌리려면 `npx wrangler rollback`. 캐시 킬 스위치와
달리 W1a는 한 줄 토글이 없다 — 표면 분리가 코드 경로 전체에 걸쳐 있기 때문이다.

이 문서는 **W 트랙 전체 순서**의 정본이다. W0c의 세부 절차·SQL·리허설 기록은
브랜치 SSOT에 있었고 흡수 커밋에서 삭제했다 — 필요하면 `sandbox/w0c-d1-seeding` 커밋 이력에서 찾는다.

```bash
# 환경 확인 (3줄)
cd ~/devetym-proxy && source ~/.nvm/nvm.sh && nvm use 22 && npm test   # 131/131
npm run check:prompt                                                  # 프롬프트 바이트 대조
cd ~/devetym && python3 Scripts/db-expand/test_seed_d1.py             # PASS
python3 Scripts/db-expand/test_export_bundle.py                       # PASS
```

⚠️ **`nvm use 22`를 빠뜨리면** wrangler가 조용히 이상하게 군다.

---

## 1. 무엇이 끝났나 (2026-08-25)

| | 상태 | 근거 |
|---|---|---|
| **규모 판정 「크게」** | ✅ 사람 결정 | 650장 + 웹 AI. 재론 금지 |
| **ADR-0009·0010·0011** | ✅ 비준 | 스택 · 남용 방지 · 프롬프트 소유권 |
| **W0a 기반** | ✅ | Astro 5.18 + `@astrojs/cloudflare` 12.6 · `SITE_URL` 단일 지점 · 토큰 자동 추출 · 폰트 woff2 · **클라이언트 JS 0바이트** |
| **W0b 도메인 결선** | ✅ | `devetym.com` 200 · `noindex` 해제 · 사이트맵 200 · 미리보기 서브도메인 차단 |
| **ADR-0012·0013 비준** | ✅ 사람 비준 (2026-08-25) | 둘 다 `Accepted`. INV-11 전단·ADR-0006 D5 갱신 완료. 함께 정해진 것 = **승격 잡을 W1c로 W 트랙 안에서 닫는다**(선택지 (b)) |

**도메인**: Amazon Registrar 등록($16/yr·자동 갱신 ✅), **네임서버만 Cloudflare 위임**(소유·결제는 Amazon 유지 — 이전은 하지 않기로 결정). Route 53 호스팅 영역 삭제로 $0.50/월 회피.

---

## 2. 다음 순서 — 위에서 아래로

### ✅ 게이트 해소 · ADR-0012·0013 비준 완료 (2026-08-25 사람)

| ADR | 결정 | 눈 뜨고 수용한 대가 |
|---|---|---|
| [0012](docs/adr/0012-content-canon-d1.md) `Accepted` | **D1을 콘텐츠 정본으로 승격.** 앱 번들 = 스냅샷 | 콘텐츠 변경이 코드 리뷰를 빠져나갈 수 있다(완화 = 익스포트·커밋 **규율**) · D1이 콘텐츠 단일 장애점 |
| [0013](docs/adr/0013-web-route-contract.md) `Accepted` | **SSG + 조회 전용 SSR 폴백.** 색인 자격은 품질 게이트가 연다 | 라우트 둘의 캐시 헤더·404 정합 · 승격~색인 사이에 빌드 1회 지연 |

**함께 정해진 것**: 승격 잡(캐시 M5)을 **W1c로 W 트랙 안에서 닫는다**. 안 지으면 생성분은 영원히 `noindex`이고 ADR-0013의 최대 이점이 잠긴다.

### W0c · 650 D1 시딩 〔✅ 완료 2026-09-01〕
`origin` 컬럼 신설(✅) · `prompt_version` 센티널(✅) · authored 650 시딩(✅ 로컬) · **authored > generated 충돌 규칙**(✅ — 실증: 안 고치면 검수된 용어에 `not_dev_term` 오판이 살아남아 앱이 "개발 용어 아님"을 답한다) · 익스포트 잡(✅ 바이트 동일 왕복). **W1b보다 반드시 먼저.** 원격 적용까지 완료 — 남은 것 없음.

### W1a · 프록시 하드닝 〔코드 ✅ 2026-09-01 · 배포 ⬜〕

`devetym-proxy` `feat/w1a-proxy-hardening` (2커밋, **미푸시·미배포**). 테스트 83 → **131**.

| 항목 | 상태 | 무엇이 됐나 |
|---|---|---|
| CORS 고정 allowlist | ✅ | allowlist 밖 Origin은 **서버가 403** — 헤더만으론 방어가 아니다(요청은 이미 도달·과금됨) |
| 표면 분리 캡 | ✅ | 웹 3층(쿠키 3·IP 15·웹 전역 30) / 앱 2층(10·200 **현행 유지**, KV 키도 그대로) |
| 서명 쿠키 식별 | ✅ | HMAC·HttpOnly·Partitioned. 시크릿 없으면 층만 빠지고 나머지 동작 |
| Turnstile | ✅ 배선 | 웹 생성 요청에만·fail-closed. **키 없으면 꺼진 채 통과**(대시보드 작업과 배포 분리) |
| 워크스페이스 키 | ✅ 배선 | `ANTHROPIC_API_KEY_WEB` 있으면 웹만 그 키. 없으면 앱 키 폴백 |
| 프롬프트 이전 | ✅ | `src/prompt.js` = `ClaudePrompt.kt` **바이트 동일**. 앱 코드 무변경(INV-1) |
| usage 표면 태그 | ✅ | `surface`·`outcome` 컬럼(0003). **차단 행도 적재** — F5 지표의 분자가 생겼다 |

**남은 것 = 사람 4줄** (§0 「W1a를 켜는 순서」): ① Turnstile 위젯 발급 ② Anthropic 워크스페이스+키
③ 시크릿 3개 주입 ④ `d1 migrations apply devetym-usage --remote` + `npm run deploy` + **앱 실측**.

완료 오라클: **기존 iOS 앱에서 생성 정상 성공**(무영향 실측) + 브라우저에서 토큰 없이 호출 시 차단.
⚠️ 앞의 것은 로컬로 못 닫는다 — 프롬프트가 서버로 옮겨져 **앱이 받는 내용이 바뀔 수 있는 유일한 창**이 배포 직후다.

### W1b · 웹 본체
정적 650장 + 검색 + 상세 + AI 폴백. **한글 별칭 1,097개를 title·h1·구조화 데이터에 1급으로 올린다** — 안 하면 650장은 한국어 검색에 존재하지 않는다.
완료 오라클: **배포된 실 URL 650개 전수 200 응답** + AI 생성 왕복 성공. **로컬 빌드 성공은 오라클이 아니다.**
**배포 직후 사람 1건**: Search Console → Sitemaps → `sitemap-index.xml` 제출(소유권은 2026-08-25 확인 완료, 계측 가동 중). 이걸로 색인률 배선이 닫힌다.

### W1c · 승격 잡 (= 캐시 M5) 〔2026-08-25 사람 선택 (b)〕
`critic` 게이트(INV-7)를 통과한 `origin='generated'` 행을 `authored`로 승급 → 다음 빌드에서 SSG 집합·사이트맵에 편입되며 그때 색인된다. **이게 없으면 웹 AI로 자란 콘텐츠는 영원히 `noindex`**다. 완료 오라클: 승격된 용어가 **배포 후 실 URL에서 `noindex` 없이 200** + 사이트맵에 등장.

### W2 · W3
W2 = 카테고리 허브·관련 용어·구조화 데이터·얇은 콘텐츠 대응(317개가 300자 미만). W3 = 8주 실측 리뷰(**검색 수요 가정이 여기서 사후 판정된다**).

---

## 3. 하지 말 것

- **설계를 다시 하지 않는다.** 정본 = [`docs/design/web-transition-design.md`](docs/design/web-transition-design.md). 규모 재론 금지.
- **W1b를 W0c·W1a보다 먼저 열지 않는다.** D1 시딩 전에 650장을 구우면 정본이 아닌 것을 구워 두 번 짓는다. 방어 없이 웹 AI를 열면 웹 폭주가 앱 사용자를 429로 막는다.
- **`web/src/styles/tokens.css`를 손으로 고치지 않는다.** 생성물이다 — 앱 `ui/theme/*.kt`를 고쳐야 한다.
- **`SITE_URL`을 wrangler `[vars]`에 넣지 않는다.** 페이지가 prerender라 도메인은 빌드 시점에 구워진다 — 런타임 주입은 어긋난 값을 두 벌 만든다.
- **ADR·specs·architecture·INV를 승인 없이 고치지 않는다.** 뒤집으려면 새 ADR.
- **과정 어휘로 사람에게 설명하지 않는다.** 결정과 트레이드오프로 말하고, 참고 자료는 한 번에 한 탭만.
- 검색 수요 실측을 다시 제안하지 않는다 — 사람이 그 위를 지나가기로 결정했다.

---

## 4. 인용해도 되는 실측값

| 항목 | 값 | 시점 |
|---|---|---|
| 엔트리 / 고유 keyword | 650 / 650 (충돌 0) | 08-21 |
| 한글 keyword | **0** — 표제어는 전부 영어 | 08-21 |
| 고유 별칭 키 / 그중 한글 | 1,316 / **1,097** | 08-21 |
| 교차 충돌 | 3 (집계·분기·샤딩) | 08-21 |
| 본문 중앙값 / 300자 미만 | 303.5자 / 317개(48.8%) | 08-21 |
| 앱 한도 | `APP_DAILY_LIMIT` 10 / `APP_GLOBAL_DAILY_LIMIT` 200 — W1a에서도 **불변** | 09-01 |
| 웹 한도 | 쿠키 **3** / IP **15** / 웹 전역 **30** (하루 최대 지출 ≈ 30 × $0.023 ≈ **$0.69**) | 09-01 |
| Anthropic 월 예산 / 7월 실적 | $30 상한 / **$0.20** | — |
| 용어 1건 생성 단가 | **$0.023** (캐시 실증: 같은 용어 10회 → 호출 1회) | 07-28 |
| 웹 빌드 / Worker 기동 | 750ms / 24ms | 08-25 |
| 폰트 | woff2 5종 291KB→121KB (DM Sans는 **미탑재** — 앱에서도 실사용 0) | 08-25 |
| **프로덕션 D1** | entries **671** · authored **650** · generated **21** · aliases **1,304** · entry_versions 0 | **09-01** |
| 배포 워커 | `devetym-proxy` `c9ccd526` (N1 정규화) · 바인딩 KV+D1 2개 | 09-01 |
| 시딩 소요 | 2,592 문장 · 5,827행 쓰기 · **3.5초** (D1 무료 티어에서 무의미한 규모) | 09-01 |
| authored 센티널 | `authored:efa8f264dc67` = 번들 정규화 JSON sha256[:12] | 09-01 |
| 프로덕션 키 위생 | 1,975개 전부 N1 · 비-N1 **0** | 09-01 |
| Turnstile site key | `0x4AAAAAAEkZxJ7JdEVEtZ47` — **공개값**(페이지 HTML에 박히는 값). W1b 프론트에서 쓴다 | 09-02 |
| 웹 워크스페이스 | Anthropic `DevEtym-Web` — 웹 전용 키의 소속. 앱 키는 무관하게 유지 | 09-02 |
| W1a 프롬프트 해시 | `956ba44a7c48` — 이전 전후 **불변**(프로덕션 generated 행의 태그와 동일) | 09-01 |
| W1a 테스트 / 번들 | 131개 통과 / 배포 번들 34.8KiB(gzip 10.9) | 09-01 |
| W0c 적용 전 백업 | `~/devetym-d1-backup-20260901-020554.sql` (entries 21 · aliases 12) | 09-01 |
| 스테이징 D1 | `devetym-cache-dev` `1be332f5-d3fb-48cb-9ba7-fc481c2a0fec` (무료 3/10) | 09-01 |

✅ **익스포트는 2026-09-01 닫혔다 — D1 → `terms.json`이 현행 파일과 바이트 동일.** ADR-0012가 "규율이지 강제 장치가 아니다"라고 적어 둔 익스포트·커밋 의무에 실행 수단이 생겼다. 「직접 편집 금지」 마커는 JSON 배열에 주석을 못 달아 **깨지는 검사**로 구현했다(`Scripts/db-expand/bundle-snapshot.json` + 테스트 대조).

✅ **충돌 규칙은 2026-09-01 확정됐다 — authored > generated, 밀려난 본은 `entry_versions`(INV-5 첫 write).** 갱신 조건은 `origin='generated'` 또는 `prompt_version` 변경 둘뿐이라 재실행이 멱등이다. `hit_count`는 보존한다. **read path·write-back은 안 건드렸다**(INV-2·INV-4) — 바뀐 것은 authoring path SQL뿐.

✅ **시딩은 2026-09-01 프로덕션까지 끝났다 — entries 671 · authored 650 · generated 21 · aliases 1,304.** 로컬(668/1,301)·dev 클론·프로덕션 세 곳에서 예측값과 정확히 일치했다. PK 충돌 0 · 별칭 전량 착지 · 멱등 · 0에서 재현. 실 앱 경로에서 `aatree`(무공백)·`AA 트리`(한글 별칭)·`추상 팩토리`가 전부 정본 행에 도달한다 — **N1 정의의 값이 프로덕션에서 확인된 지점**이다. 프로덕션 키 1,975개 전부 N1이고 좌초 행은 없다.

✅ **authored 센티널은 2026-08-26 확정됐다 — `authored:` + 번들 정규화 JSON sha256 앞 12자.** 현행 값 `authored:efa8f264dc67`. 손 번호(`db-expand-v<N>`)는 기각했다 — 원천을 고치고 번호를 안 올리면 신·구가 한 태그로 섞이는데 알아챌 수단이 없다. 시딩은 컬럼뿐 아니라 **payload JSON 안에도** `schemaVersion`·`promptVersion`을 실어야 한다 — 캐시 히트가 payload를 앱에 그대로 배달하므로, 컬럼만 채우면 같은 용어가 전달 경로에 따라 두 모양이 된다.

✅ **정규화 정의는 2026-08-26 확정됐다 — 구분자(공백류·하이픈·언더스코어) 삭제.** 확정값은 **entries 668 · aliases 1,301 · 엔트리간 별칭 충돌 3 · entries PK 충돌 0**이다. 착수 전 예상치(별칭 1,286 / 충돌 4 / `b트리` 충돌)는 **실측으로 기각됐다.** 웹은 프록시의 JS 구현을 공유하므로 네 번째 구현을 만들지 않는다. 세 지점(앱 `AppJson.kt` · Worker `src/index.js` · 파이프라인 `Scripts/db-expand/term_key.py`)의 동치는 `test_term_key.py`가 교차 실행으로 고정한다.

## 5. 실측으로 얻은 함정 (다시 밟지 말 것)

1. **미리보기 서브도메인을 실 도메인 붙인 뒤에도 켜 두면** 같은 내용을 색인 허용 상태로 서빙해 중복 콘텐츠로 경쟁한다 → `workers_dev = false`로 껐다.
2. **`www` 301을 Astro 미들웨어로 못 한다.** 어댑터의 `_routes.json`이 prerender 경로를 Worker 호출에서 제외해 미들웨어가 아예 안 탄다(`assets.run_worker_first`로도 안 뒤집힘). → **Cloudflare Redirect Rule**로 걸어야 한다(사람 · ROADMAP 백로그에 설정값).
3. **`robots.txt`가 선언한 사이트맵이 404였다.** 없는 사이트맵을 Search Console에 제출하면 오류로 남는다 → `@astrojs/sitemap` 추가로 해소.
4. **Cloudflare가 robots.txt에 「Managed Content」를 자동 주입한다.** 판정: **그대로 둔다** — 차단 목록은 전부 학습용 수집기(유입 0)이고, 인용·링크를 붙이는 답변형 봇과 검색 크롤러는 허용된 상태다.
5. **🔑 정규화 정의를 바꾸면 워커 배포가 데이터 시딩보다 먼저다** (W0c §3-7에서 실측으로 잡음). N1 키로 먼저 붓고 옛 N0 워커가 돌고 있으면, keyword **286개**·별칭 **784개**가 워커가 조회하지 않는 키로 앉는다 → 사용자가 찾을 때마다 과금 + 같은 용어의 두 번째 행 생성. ADR-0012 이후 이건 캐시 오염이 아니라 **정본에 모순을 심는 것**이다. 반대 순서(배포 먼저)의 창은 안전하다 — 그 사이 생긴 generated는 시딩의 충돌 규칙이 흡수한다.
6. **정규화를 바꾸면 기존 행이 좌초한다 — 앱뿐 아니라 서버도.** 이슈 [#21](https://github.com/data-sy/devetym/issues/21)은 앱 로컬 DB만 다뤘는데, 프로덕션 D1에도 옛 키 행이 **5건**(`하네스 엔지니어링` 등) 있었다. 배포 창에서 `UPDATE`로 같이 정리했다. **정규화 변경 = 세 저장소(앱 DB · 서버 D1 · 번들) 전부 점검**이다.
7. **격리는 명령 가드가 아니라 좌표 반전으로 얻는다.** 래퍼 스크립트는 `npx wrangler`를 직접 치면 우회된다. 단 **완전하지 않다**: `d1 execute`는 DB 이름을 `wrangler.toml`이 아니라 **계정 전체**에서 해석하고 `d1 migrations apply`는 설정에서 찾는다 — **둘의 해석 규칙이 다르다.** 블라스트 반경이 큰 쪽(배포·스키마 변경)은 반전으로 막히고, `execute`는 안 막힌다.
8. **`\s`·`str.isspace()`·`trim()`은 셋 다 Kotlin `isWhitespace()`와 집합이 다르다.** 파이썬은 U+0085를 공백으로 보고 Kotlin은 아니다 · JS `trim()`은 BOM을 자르고 Kotlin은 아니다. 여러 언어가 같은 키를 만들어야 하면 **코드포인트 집합을 명시**하고 교차 실행 테스트로 고정해야 한다 — 각 구현은 자기 일관적이라 갈라져도 조용하다.
9. **측정 전에 판정 브리핑을 쓰지 않는다.** W0c에서 착수 전 트레이드오프 서술이 **두 번 다 틀렸다**(「구분자를 지우면 페이지를 잃는다」→ 소실 0건 · 「`b트리`가 충돌한다」→ 그 충돌은 존재하지 않았다). 후보를 실 데이터에 돌려본 뒤에 쓴다.
10. **`~/devetym-proxy`는 Node 22를 요구한다**(`.nvmrc`, 시스템 기본은 v20). `source ~/.nvm/nvm.sh && nvm use 22`를 빠뜨리면 wrangler가 경고만 뱉고 조용히 이상하게 군다.
11. **CORS 헤더는 방어가 아니다.** 브라우저가 막는 것은 응답 *읽기*뿐이고, 요청은 이미 서버에 도달했고 과금도 끝났다. 제3자 사이트의 무료 게이트웨이화(T4)를 실제로 막는 것은 **서버의 403**이다. 그래서 W1a 테스트의 핵심 단언은 헤더가 아니라 `Anthropic 미호출`이다.
12. **서드파티 쿠키는 Safari에서 죽는다.** 웹 식별 쿠키를 `workers.dev` 오리진에서 심으면 Safari가 차단해 **매 요청이 새 id를 받고 3건 한도가 사실상 사라진다**(IP 15·웹 전역 30은 그대로 문다). 층을 설계할 때 "어느 오리진에서 심는가"가 층의 존재 여부를 결정한다 → W1b에서 웹 API를 `devetym.com/api/*`로 붙여 first-party로 만들어야 이 층이 실재한다. `Partitioned`(CHIPS)는 Chrome 대비로 이미 실어 뒀다.
13. **관측 구멍은 "지표가 없다"가 아니라 "분자가 없다"로 나타난다.** 설계서 F5(앱의 429/402 발생률)를 보려면 차단된 요청이 로그에 있어야 하는데, usage 로그가 **성공 호출만** 적재하고 있었다. 컬럼을 더하는 일보다 **무엇을 적재하지 않고 있었는지**를 찾는 게 실제 작업이었다.
14. **좌표를 복원할 때는 좌표를 쓰는 모든 곳을 함께 본다.** W0c 좌표 복원(09-01)이 `wrangler.toml`만 되돌리고 `scripts/d1.mjs`·`package.json`의 DB 이름은 샌드박스로 남겨서 `db:*:local` 3개가 조용히 깨져 있었다. 테스트는 miniflare를 쓰므로 **전부 녹색이었다** — 깨진 걸 알려 줄 오라클이 없었다.
15. **프롬프트를 옮길 때의 오라클은 "돌아간다"가 아니라 "해시가 같다"다.** 이식이 한 글자 틀려도 코드는 돌고 테스트도 통과하며, 차이는 생성 품질과 `prompt_version` 분열로만 조용히 드러난다. 프로덕션이 이미 아는 값(`956ba44a7c48`)을 오라클로 쓰면 그 창이 닫힌다.

---

## 6. 포인터

| 무엇 | 어디 |
|---|---|
| 상태 정본 | [`ROADMAP.md`](ROADMAP.md) Now 「▶ 재개 지점」 |
| **사람이 할 일 (구체 절차)** | 같은 곳 「🙋 사람이 해야 하는 것」 — **남은 것 = `www`→apex 301 · Apple 리마인더 등록(2027-06-08) · ⏳사이트맵 제출(W1b 배포 후)**. ADR 비준·Apple 갱신일·**Search Console 소유권**은 ✅완료 |
| 설계 정본 | [`docs/design/web-transition-design.md`](docs/design/web-transition-design.md) |
| 웹 코드 | `~/devetym/web/` ([README](web/README.md) — 손대면 안 되는 것·앱과 의도적으로 다른 곳) |
| 서버 코드 | `~/devetym-proxy` (별도 repo) · 계약 = [ADR-0006](docs/adr/0006-server-cache-boundary.md) |
| 운영 비용 원장 | [`docs/cost/running-costs.md`](docs/cost/running-costs.md) |
| 버그·개선 | GitHub Issues ([ADR-0008](docs/adr/0008-issue-tracking.md)) |
| 씨딩 복붙본 | `~/Downloads/devetym-release/` — **폐기 금지**, 착지 링크를 `devetym.com`으로 교체해 재사용. 발사 = 웹 본체 완성 후 |
| 살아 있는 다른 트랙 | `~/Downloads/devetym-release` 정돈(미착수, 웹과 독립) |

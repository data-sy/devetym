# DevEtym 웹

유입(acquisition)용 웹 표면. **앱을 대체하지 않는다** — 채널 확장이다(웹 = 검색·씨딩 착지면, 앱 = 무제한 표면).

- 설계 정본: [`../docs/design/web-transition-design.md`](../docs/design/web-transition-design.md)
- 결정: [ADR-0009](../docs/adr/0009-web-framework-rendering.md)(스택) · [ADR-0010](../docs/adr/0010-web-abuse-prevention.md)(남용 방지) · [ADR-0011](../docs/adr/0011-prompt-ownership-transfer.md)(프롬프트) · [ADR-0012](../docs/adr/0012-content-canon-d1.md)(D1 = 콘텐츠 정본) · [ADR-0013](../docs/adr/0013-web-route-contract.md)(SSG + 조회 전용 SSR 폴백) — **5건 모두 `Accepted`**
- 진행 상태 정본: [`../ROADMAP.md`](../ROADMAP.md) W 트랙

## 지금 상태 — **W1c 로컬 종결 · 원격 반영 대기**

**<https://devetym.com> 라이브** — 용어 상세 **650장** · 검색 · AI 폴백 · 전체 색인(`/terms`).
배포 워커 `devetym-web` `eeba288a` (2026-09-02).

⚠️ **레포는 660장인데 라이브는 아직 650장이다.** W1c 승격 라운드 001(생성분 10건 → `authored`)이
로컬에서 끝나 번들·빌드·테스트는 660으로 서 있으나, **원격 D1 시딩과 재배포가 아직 실행되지 않았다**
(사람 실행 대기 — [ROADMAP](../ROADMAP.md) 「사람이 해야 하는 것」). 그때까지 이 아래 표의 숫자는 라이브 기준이다.

| | |
|---|---|
| ✅ W0a·W0b (08-25) | Astro 스캐폴드 · `SITE_URL` 단일 지점 · 토큰 자동 추출 · 폰트 · 사이트맵 · 클라 JS 0바이트 |
| ✅ W0c (09-01) | **650개 어원 정본이 프로덕션 D1에** (entries 671 · aliases 1,304 · 키 전부 N1) |
| ✅ W1a (09-02) | 프록시가 웹/앱을 갈라 각자 캡을 쓴다 · CORS allowlist · Turnstile 켜짐 · 프롬프트 정본은 워커 소유 |
| ✅ **W1b (09-02)** | **용어 페이지 650장 SSG · 검색+자동완성 · AI 폴백 · 조회 전용 SSR 폴백 · `/terms`** |
| ⬜ **W1c** | **승격 잡** — `critic` 통과한 `generated`를 `authored`로. 없으면 웹 AI 생성분은 영원히 `noindex` |

**완료 오라클(W1b) 통과**: 배포된 실 URL **656개 전수 200** + 웹 표면 AI 생성 왕복 성공.
3층 한도가 실제로 무는 것도 확인 — 생성 1건에 쿠키·IP·전역 카운터가 전부 증가했고,
IP 버킷이 **실 클라이언트 IP의 해시와 일치**했다(= service binding이 `CF-Connecting-IP`를 보존한다).

### 무엇이 어디에 있나

| 파일 | 역할 |
|---|---|
| `src/pages/term/[slug].astro` | 용어 상세 **SSG**(레포 660장 · 라이브 650장). 한글 별칭을 title·h1에 1급으로 |
| `src/pages/term/[...rest].astro` | **조회 전용 SSR 폴백**. D1에 있으면 200+`noindex`, 없으면 404. **생성하지 않는다** |
| `src/pages/search.astro` | 검색 화면. 이 페이지에만 JS가 있다(6.7KB) |
| `src/pages/search-index.json.ts` | 빌드가 굽는 정적 검색 인덱스(109KB / gzip 35KB) |
| `src/pages/api/term.ts` | same-site 생성 착지점 → **service binding**으로 프록시 호출 |
| `src/pages/terms.astro` | 전체 색인 — 용어 전량을 홈에서 1클릭 깊이로 |
| `src/lib/terms.ts` | 번들 스냅샷 로더 + 빌드 시 단언. **SSR 라우트에서 import 금지**(518KB가 워커에 들어간다) |
| `src/lib/term-key.ts` | 정규화 **네 번째 구현**(웹). 교차 실행 오라클이 고정한다 — 아래 참조 |
| `src/lib/search.ts` · `messages.ts` · `lookup.ts` | 검색 매칭 · 사용자 문구 전수 매핑 · D1 조회 |

### 서버 계약 (가동 중)

프록시 = `~/devetym-proxy` (별도 repo · 계약 정본은 그 README 「웹 표면 (W1a)」 절).
웹은 `devetym.com/api/term`으로 부르고, 그 라우트가 **service binding**으로 프록시에 넘긴다.

```js
fetch("/api/term", {
  method: "POST",
  credentials: "include",           // 없으면 식별 쿠키가 안 오간다
  headers: { "content-type": "application/json", "X-Turnstile-Token": token },
  body: JSON.stringify({ keyword: "React" }),   // 웹은 프롬프트를 모른다
})
```

- 응답은 앱과 **같은 Anthropic shape**. 캐시 히트·정적 열람에는 Turnstile을 요구하지 않는다 — 유입 마찰 0.
- **Turnstile site key(공개값)**: `0x4AAAAAAEkZxJ7JdEVEtZ47`
- **429의 `scope`로 화면이 갈린다**(`src/lib/messages.ts` — `browser`만 앱을 권한다).
  문구 방향은 **2026-09-02 사람 결정(「담백 사실형」)**이고, 종전 Q4(2026-08-25 「앱 유도를 전면에」)를
  **완화한 최신 결정**이다. 되돌리려면 사람에게 다시 물을 것.
- ⚠️ **`fetch`로 프록시를 다시 부르지 않는다 — service binding이다.** 인터넷을 한 번 더 타면
  `CF-Connecting-IP`가 우리 워커 것으로 바뀔 위험이 있고, 그러면 **IP 15건/일 층이 전 사용자
  공용 버킷 하나로 붕괴**한다. 바인딩이 없으면 `/api/term`은 503을 낸다 — workers.dev로 폴백하지 않는다.

### 남은 함정

- 미리보기 서브도메인(`*.workers.dev`)은 **껐다**(`workers_dev = false`).
  ⚠️ `wrangler.toml`에서 **최상위 키는 첫 섹션 헤더 앞에 둘 것** — `[assets]` 뒤로 밀리면 그 섹션에
  삼켜져 `workers_dev`가 조용히 사라진다(2026-09-02 실측, wrangler 경고가 잡아 줬다).
- ⚠️ **`www.devetym.com`은 아직 301이 아니라 200이다.** Astro 미들웨어로는 못 고친다 —
  **Cloudflare Redirect Rule**로 걸어야 하며 설정값은 [ROADMAP](../ROADMAP.md) 「사람이 해야 하는 것」 ④에 있다.
  현재 완화 = canonical이 apex를 가리킨다.
- ⚠️ **`build.format`을 `"file"`로 바꾸지 말 것.** 용어 `index`(인덱스·색인)가 `dist/term/index.html`로
  구워지면 정적 호스트가 그걸 `/term/`의 디렉터리 인덱스로도 해석해 `/term/index`가 **307**이 된다.
  빌드·타입 검사 전부 녹색이고 **전수 200 오라클만 잡는다**.
- ⚠️ **404 라우트에서 `Astro.response.status = 200`은 먹지 않는다.** 그래서 용어 폴백이
  `term/[...rest].astro`라는 자기 라우트로 분리돼 있다. 합치지 말 것.
- ⚠️ **`innerHTML`로 만든 노드에는 Astro 스코프 CSS가 안 붙는다.** 검색 결과 스타일이
  `#results` 한정 `is:global` 블록에 있는 이유다. 스코프 블록으로 옮기면 조용히 무스타일이 된다.
- ⚠️ **배포 직후 전수 200이 실패할 수 있다**(자산 전파 지연 — 실측 72건 404 → 20초 뒤 0건).
  한 번 더 돌려 보고 판정한다.

## 명령

```bash
npm install
npm run dev        # 로컬 (http://localhost:4321)
npm run tokens     # Kotlin 정본 → src/styles/tokens.css 재추출 (prebuild가 자동 실행)
npm run fonts      # 앱 번들 TTF → public/fonts/*.woff2 (폰트 바뀔 때만)
npm run build
npm run deploy     # = astro build && wrangler deploy (프로덕션)
npm test           # 검색 골든 케이스 + term_key 4지점 교차 실행

# 완료 오라클 — 로컬/프로덕션 어느 쪽이든
node scripts/check-urls.mjs http://127.0.0.1:8788
node scripts/check-urls.mjs https://devetym.com
```

⚠️ **Node 22 필수**(`source ~/.nvm/nvm.sh && nvm use 22`). 빠뜨리면 wrangler가 조용히 이상하게 군다.
⚠️ `npm run deploy`는 **프로덕션에 닿는다.** 롤백은 `npx wrangler rollback`.

## 손대면 안 되는 것

**`src/styles/tokens.css`는 생성물이다.** 손으로 고치면 다음 빌드에 덮인다. 색·치수·타이포를 바꾸려면
`shared/src/commonMain/kotlin/com/robin/devetym/ui/theme/*.kt`(앱 정본)를 고친다 — 그게 요점이다.
추출기(`scripts/extract-tokens.mjs`)는 색 11개·타이포 21종 개수를 단언하므로, 앱이 토큰을 늘리면
**빌드가 깨져서** 알려준다. 조용히 어긋나지 않는다.

## 도메인 (`SITE_URL`)

호스트명은 코드 어디에도 박혀 있지 않다. 유일한 출처는 [`src/config/site.ts`](src/config/site.ts)이고,
canonical·OG·robots·사이트맵·내부 절대링크가 전부 거기서 읽는다.

**W0b에서 실제로 한 일은 셋뿐이었다**(2026-08-25 완료 — 재현이 필요할 때의 절차로 남긴다):

1. `SITE_URL=https://devetym.com npm run build && npx wrangler deploy` — **빌드 전에** 줘야 한다.
   페이지가 prerender라 도메인이 HTML에 구워지기 때문이다(런타임 `[vars]`로 주면 어긋난 값이 두 벌 생긴다).
2. DNS를 Worker에 연결.
3. Search Console 소유권 확인 → 색인률(K1) 측정 시작. 〔✅ 완료. **사이트맵 제출만 W1b 배포 후로 남아 있다** — 페이지가 없는데 사이트맵부터 내는 건 의미가 없다〕

`IS_CANONICAL_HOST`가 자동으로 따라온다 — 실 도메인이 되는 순간 `noindex`가 풀리고 `robots.txt`가
`Allow`로 바뀐다. **손댈 곳 없다.**

## 웹이 앱과 의도적으로 다른 곳

이걸 모르고 "앱이랑 맞추자"고 되돌리면 조용히 망가진다. 근거는 설계서 §3-1.

| | 앱 | 웹 | 왜 |
|---|---|---|---|
| 본문 폰트 | 시스템 폰트 | **시스템 폰트(동일)** | 한글이 커스텀 라틴 폰트 박스에 작게 낀다. DM Sans로 바꾸지 말 것 |
| DM Sans | 정의됐으나 실사용 0 | **싣지 않음** | `appTypography` 21종 중 아무도 참조하지 않는다 |
| 자동완성 | keyword prefix만 | **+ aliases** ✅ | 그대로 옮기면 한글 입력에 반응이 전혀 없다 |
| 교차 충돌 3건 | 검색에서 뒤 엔트리가 가려짐 | **검색에서도 둘 다 나온다** ✅ | 정적 페이지는 용어마다 자기 URL이 있다 |
| `normalizeKeyword` | Kotlin | **웹 이식본(`src/lib/term-key.ts`)** | repo가 갈라져 import 공유가 불가능했다. 대신 **교차 실행 오라클**로 갚는다 — `Scripts/db-expand/test_term_key.py`가 웹 이식본을 실제로 실행해 번들 전량 + 유니코드 경계에서 파이썬·프록시 구현과 바이트 비교한다(**3,444건 불일치 0**). 이 파일을 옮기거나 이름을 바꾸면 그 테스트가 깨진다(의도된 결합) |

## 아직 없는 것 (= W1c)

**승격 잡.** `critic` 게이트(INV-7)를 통과한 `origin='generated'` 행을 `authored`로 올려
다음 빌드에서 SSG 집합·사이트맵에 편입시키는 일이다. 지금은 웹 AI가 만든 용어가 자기 URL로
200을 내지만 **전부 `noindex`**이고, 이 잡이 없으면 영원히 그렇다 —
[ADR-0013](../docs/adr/0013-web-route-contract.md)의 최대 이점이 잠긴 채로 남는다.

그 다음은 W2(카테고리 허브 6 · 관련 용어 확장 · 얇은 콘텐츠 317건 대응) · W3(8주 실측 리뷰).

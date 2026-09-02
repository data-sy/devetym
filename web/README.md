# DevEtym 웹

유입(acquisition)용 웹 표면. **앱을 대체하지 않는다** — 채널 확장이다(웹 = 검색·씨딩 착지면, 앱 = 무제한 표면).

- 설계 정본: [`../docs/design/web-transition-design.md`](../docs/design/web-transition-design.md)
- 결정: [ADR-0009](../docs/adr/0009-web-framework-rendering.md)(스택) · [ADR-0010](../docs/adr/0010-web-abuse-prevention.md)(남용 방지) · [ADR-0011](../docs/adr/0011-prompt-ownership-transfer.md)(프롬프트) · [ADR-0012](../docs/adr/0012-content-canon-d1.md)(D1 = 콘텐츠 정본) · [ADR-0013](../docs/adr/0013-web-route-contract.md)(SSG + 조회 전용 SSR 폴백) — **5건 모두 `Accepted`**
- 진행 상태 정본: [`../ROADMAP.md`](../ROADMAP.md) W 트랙

## 지금 상태 — W0~W1a 완료, **다음이 W1b(본체)**

**<https://devetym.com> 라이브** · 색인 허용 · 사이트맵 200. 다만 **떠 있는 것은 착지 페이지 한 장뿐**이다.

| | |
|---|---|
| ✅ W0a·W0b (08-25) | Astro 스캐폴드 · `SITE_URL` 단일 지점 · 토큰 자동 추출 · 폰트 · 사이트맵 · 클라 JS 0바이트 |
| ✅ W0c (09-01) | **650개 어원 정본이 프로덕션 D1에** (entries 671 · aliases 1,304 · 키 전부 N1) |
| ✅ W1a (09-02) | 프록시가 웹/앱을 갈라 각자 캡을 쓴다 · CORS allowlist · Turnstile 켜짐 · 프롬프트 정본은 워커 소유 |
| ⬜ **W1b** | **용어 페이지 650장 · 검색 · AI 폴백** ← 여기부터가 이 디렉토리의 다음 일 |

### W1b가 쓸 서버 계약 (W1a에서 확정)

프록시 = `~/devetym-proxy` (별도 repo · 계약 정본은 그 README 「웹 표면 (W1a)」 절).

```js
fetch(API, {                        // ⚠️ devetym.com/api/* 로 붙일 것 — 아래 참조
  method: "POST",
  credentials: "include",           // 없으면 식별 쿠키가 안 오간다
  headers: { "content-type": "application/json", "X-Turnstile-Token": token },
  body: JSON.stringify({ keyword: "React" }),   // 웹은 프롬프트를 모른다
})
```

- 응답은 앱과 **같은 Anthropic shape**(캐시 히트는 합성 응답). 캐시 히트·정적 열람에는 Turnstile을 요구하지 않는다 — 유입 마찰 0.
- **Turnstile site key(공개값)**: `0x4AAAAAAEkZxJ7JdEVEtZ47`
- **429의 `scope`로 문구를 갈라야 한다**: `browser`(하루 3건 소진) = **앱 유도**, `ip`·`web` = "내일 다시".
  하나로 뭉치면 거짓말이 된다. 앱 유도를 전면에 두는 것은 사람 결정(Q4, 2026-08-25)이고,
  **문구 최종안은 2~3안을 만들어 사람이 고른다.**
- ⚠️ **API를 `devetym.com/api/*`(same-site)로 붙일 것.** 워커를 `workers.dev`로 직접 부르면 식별 쿠키가
  서드파티가 되어 Safari에서 차단되고 「브라우저당 3건」 층이 사실상 사라진다(IP 15·웹 전역 30은 그대로 문다).
  표면 판정은 same-site에서도 성립한다 — 비-GET 요청엔 동일 출처에도 `Origin`이 실린다. 옮기는 날 한 줄로 실측할 것.

### 남은 함정

- 미리보기 서브도메인(`*.workers.dev`)은 **껐다**(`workers_dev = false`) — 실 도메인과 같은 내용을 색인 허용
  상태로 서빙해 중복 콘텐츠로 경쟁하기 때문(2026-08-25 실측).
- ⚠️ **`www.devetym.com`은 아직 301이 아니라 200이다.** Astro 미들웨어로는 못 고친다 — 어댑터의
  `_routes.json`이 prerender 경로를 Worker 호출에서 제외해 미들웨어가 아예 안 탄다(`assets.run_worker_first`로도
  안 뒤집힘). **Cloudflare Redirect Rule**로 걸어야 하며 설정값은 [ROADMAP](../ROADMAP.md) 「사람이 해야 하는 것」 ④에 있다.
  현재 완화 = canonical이 apex를 가리킨다.

## 명령

```bash
npm install
npm run dev        # 로컬 (http://localhost:4321)
npm run tokens     # Kotlin 정본 → src/styles/tokens.css 재추출 (prebuild가 자동 실행)
npm run fonts      # 앱 번들 TTF → public/fonts/*.woff2 (폰트 바뀔 때만)
npm run build
npx wrangler deploy
```

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
| 자동완성 | keyword prefix만 | **+ aliases** (W1b) | 그대로 옮기면 한글 입력에 반응이 전혀 없다 |
| 교차 충돌 3건 | 검색에서 뒤 엔트리가 가려짐 | **둘 다 페이지를 가짐** | 정적 페이지는 용어마다 자기 URL이 있다 |
| `normalizeKeyword` | Kotlin | **프록시의 JS 구현을 공유** | 세 번째 구현을 만들면 드리프트 지점이 3개가 된다 |

## 아직 없는 것 (= W1b)

용어 페이지 650장 · 검색 · AI 폴백 · 한글 별칭 1,097개의 title/h1 승격.
**게이트였던 프록시 하드닝(W1a)은 2026-09-02에 끝났다** — 이제 열어도 웹 폭주가 앱 사용자를 429로 막지 않는다.
방어 값은 위 「W1b가 쓸 서버 계약」 참조.

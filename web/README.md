# DevEtym 웹

유입(acquisition)용 웹 표면. **앱을 대체하지 않는다** — 채널 확장이다(웹 = 검색·씨딩 착지면, 앱 = 무제한 표면).

- 설계 정본: [`../docs/design/web-transition-design.md`](../docs/design/web-transition-design.md)
- 결정: [ADR-0009](../docs/adr/0009-web-framework-rendering.md)(스택) · [ADR-0010](../docs/adr/0010-web-abuse-prevention.md)(남용 방지) · [ADR-0011](../docs/adr/0011-prompt-ownership-transfer.md)(프롬프트) · [ADR-0012](../docs/adr/0012-content-canon-d1.md)·[ADR-0013](../docs/adr/0013-web-route-contract.md) *(제안 · 비준 대기)*
- 진행 상태 정본: [`../ROADMAP.md`](../ROADMAP.md) W 트랙

## 지금 상태 — W0a·W0b 완료 (기반만)

**<https://devetym.com> 라이브** · 색인 허용 · 사이트맵 200.

**아직 없는 것**: 용어 페이지 650장 · 검색 · AI 폴백(W1b). 지금 떠 있는 것은 착지 페이지 한 장뿐이다.

- 미리보기 서브도메인(`*.workers.dev`)은 **껐다**(`workers_dev = false`) — 실 도메인과 같은 내용을 색인 허용 상태로 서빙해 중복 콘텐츠로 경쟁하기 때문(2026-08-25 실측).
- ⚠️ **`www.devetym.com`은 301이 아니라 200이다.** Astro 미들웨어로는 못 고친다 — 어댑터의 `_routes.json`이 prerender 경로를 Worker 호출에서 제외해 미들웨어가 아예 안 탄다(`assets.run_worker_first`로도 안 뒤집힘). **Cloudflare Redirect Rule**로 걸어야 하며 설정값은 [ROADMAP](../ROADMAP.md) `Later` 백로그에 있다. 현재 완화 = canonical이 apex를 가리킨다.

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

`devetym.com`을 사면(W0b) 할 일은 셋뿐이다:

1. `SITE_URL=https://devetym.com npm run build && npx wrangler deploy` — **빌드 전에** 줘야 한다.
   페이지가 prerender라 도메인이 HTML에 구워지기 때문이다(런타임 `[vars]`로 주면 어긋난 값이 두 벌 생긴다).
2. DNS를 Worker에 연결.
3. Search Console 소유권 확인 → 색인률(K1) 측정 시작.

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

## 아직 없는 것 (W1b 이후)

용어 페이지 650장 · 검색 · AI 폴백 · 사이트맵 · 한글 별칭 1,097개의 title/h1 승격.
**AI 생성은 프록시 하드닝(W1a)이 끝나기 전에 열지 않는다** — 방어 없이 열면 웹 폭주가 앱 사용자를 429로 막는다.

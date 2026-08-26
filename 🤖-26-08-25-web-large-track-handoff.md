# 🤖 웹 트랙 W — 기반·ADR 완료 · **W0c 착수(환경 완료, 본체 0/7)**

> **콜드 세션 시작점.** 사람이 *"뭐 하고 있었어? 이어서 하자"* 라고 물으면 **이 문서로 답한다.**
> 상태 정본은 [`ROADMAP.md`](ROADMAP.md) Now의 「▶ 재개 지점」 — 충돌하면 ROADMAP이 이긴다.
> **최종 갱신 2026-08-26 (W0c 샌드박스 환경 완료 반영).** 선행 핸드오프·규모 판정 브리프는 2026-08-25 서류 정돈에서 삭제했다(판정 결과는 §1·§2에 흡수).

**한 줄**: <https://devetym.com> 이 라이브다. **기반(W0a·W0b)은 끝났고 본체(650장·검색·AI)는 한 줄도 없다.** **ADR-0012·0013은 2026-08-25 비준됐다 — 웹 트랙에 사람 게이트는 더 없다.** **W0c는 2026-08-26 착수했다 — 격리 환경(로컬 D1·좌표 반전·CI)은 완료, 본체 7단계는 0/7.** 다음 한 걸음은 **`normalizeTermKey` 정의 확정**이고, 트리·제안은 [`w0c-sandbox-roadmap.md`](w0c-sandbox-roadmap.md) 「▶ 이어서 하자」에 있다.

---

## 0. 새 세션이 *"이어서 하자"* 를 들었을 때

**→ [`w0c-sandbox-roadmap.md`](w0c-sandbox-roadmap.md)의 「▶ 이어서 하자」 절을 읽고 그대로 보여준다.**
거기에 진행 트리·완료분·다음 한 걸음·착수 제안이 다 있다. 이 문서는 **W 트랙 전체 순서**의 정본으로 남는다.

```bash
# 환경이 살아 있는지 (2줄)
cd ~/devetym-proxy && source ~/.nvm/nvm.sh && nvm use 22 && npm test   # 69/69
npm run db:local "SELECT COUNT(*) n FROM entries"                       # 18
```

**한 줄 요약**: W0c는 **환경 구축이 끝났고 본체 7단계는 0/7**이다. 다음 = **§3-1 `normalizeTermKey` 정의 확정**.
[ADR-0012](docs/adr/0012-content-canon-d1.md)·[ADR-0013](docs/adr/0013-web-route-contract.md) 둘 다 `Accepted` — 사람 게이트는 없다.

- **W0c 착수 내용** = `~/devetym-proxy` D1에 **`origin` 컬럼 신설** · authored 650 시딩 ·
  **authored > generated 충돌 규칙** · `prompt_version` 센티널 · 익스포트 잡.
- **순서는 W0c → W1a → W1b → W1c**다. 앞당기지 않는다.
- **작업은 로컬 D1 샌드박스에서 한다** — 실 정본 무접촉. 좌표가 반전돼 있어 배포·원격 스키마 변경이 막혀 있다.

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

### W0c · 650 D1 시딩 〔지금 여기 · 프록시 측〕
`origin` 컬럼 신설 · authored 650 시딩 · **authored > generated 충돌 규칙**(현행 `ON CONFLICT DO NOTHING`이면 검수 안 된 AI판이 이긴다) · `prompt_version` 센티널 · 익스포트 잡. **W1b보다 반드시 먼저.**

### W1a · 프록시 하드닝 〔⚠️ W1b보다 반드시 먼저〕
`~/devetym-proxy` 수정 — CORS allowlist · Turnstile · **표면 분리 캡** · 워크스페이스 분리 · **프롬프트 소유권 이전(ADR-0011)** · **usage 로그에 표면 태그**. 웹 한도 3층: 쿠키 3건/일 · IP 15건/일 · 웹 전역 30건/일 (앱은 10/200 현행 유지).
**⚠️ 웹 사용자 식별은 로그인이 아니라 Worker가 심는 서명 쿠키**(HMAC·HttpOnly)다. 지우면 리셋되는 건 의도된 느슨함이고, 위조 불가능한 IP 층이 받친다.
완료 오라클: **기존 iOS 앱에서 생성 정상 성공**(무영향 실측) + 브라우저에서 토큰 없이 호출 시 차단.

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
| `DAILY_LIMIT` / `GLOBAL_DAILY_LIMIT` | 10 / 200 (`~/devetym-proxy/src/index.js:33-34`) | 08-21 |
| Anthropic 월 예산 / 7월 실적 | $30 상한 / **$0.20** | — |
| 용어 1건 생성 단가 | **$0.023** (캐시 실증: 같은 용어 10회 → 호출 1회) | 07-28 |
| 웹 빌드 / Worker 기동 | 750ms / 24ms | 08-25 |
| 폰트 | woff2 5종 291KB→121KB (DM Sans는 **미탑재** — 앱에서도 실사용 0) | 08-25 |

⚠️ **정규화 정의가 별칭 수를 바꾼다.** 구분자까지 지우는 정규화로 세면 별칭 1,286 / 충돌 4(`b트리`). 웹이 `normalizeTermKey`를 공유할 때 정의가 어긋나면 페이지 수와 충돌 수가 조용히 달라진다.

## 5. 실측으로 얻은 함정 (다시 밟지 말 것)

1. **미리보기 서브도메인을 실 도메인 붙인 뒤에도 켜 두면** 같은 내용을 색인 허용 상태로 서빙해 중복 콘텐츠로 경쟁한다 → `workers_dev = false`로 껐다.
2. **`www` 301을 Astro 미들웨어로 못 한다.** 어댑터의 `_routes.json`이 prerender 경로를 Worker 호출에서 제외해 미들웨어가 아예 안 탄다(`assets.run_worker_first`로도 안 뒤집힘). → **Cloudflare Redirect Rule**로 걸어야 한다(사람 · ROADMAP 백로그에 설정값).
3. **`robots.txt`가 선언한 사이트맵이 404였다.** 없는 사이트맵을 Search Console에 제출하면 오류로 남는다 → `@astrojs/sitemap` 추가로 해소.
4. **Cloudflare가 robots.txt에 「Managed Content」를 자동 주입한다.** 판정: **그대로 둔다** — 차단 목록은 전부 학습용 수집기(유입 0)이고, 인용·링크를 붙이는 답변형 봇과 검색 크롤러는 허용된 상태다.

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

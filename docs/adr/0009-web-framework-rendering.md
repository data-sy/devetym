# ADR 0009: 웹 프레임워크·렌더링 경계 — Astro + React 아일랜드, 650개 SSG

## Status
**Accepted** (2026-08-25) — 설계 위원회 산출(2026-08-05 `Proposed`), 2026-08-25 사람 비준 — 웹 규모 판정 「크게」와 함께 승인.

근거 정본: [`../design/web-transition-design.md`](../design/web-transition-design.md) 결정 D1·D2·D3·D4·D9.

## Context

유입(acquisition)을 목적으로 웹 표면을 추가한다(ROADMAP W 트랙). 앱은 유지하고 웹은 검색 유입 표면을 맡는다 — 마이그레이션이 아니라 채널 확장.

제약과 사실:

- 콘텐츠는 큐레이션 용어 **650개**(`shared/src/commonMain/composeResources/files/terms.json`). 빌드 타임에 전부 정적 생성 가능한 규모다.
- 백엔드는 이미 Cloudflare Worker(`devetym-proxy`, ADR-0006). D1·KV도 같은 계정.
- 1차 목적이 **검색 유입**이므로 렌더링 전략이 곧 성패다. 검색엔진이 읽을 HTML이 없으면 이 이행은 존재 이유를 잃는다.
- 유지 주체는 1인. 프레임워크 복잡도가 곧 방치 리스크다.
- 사용자 표현은 "리액트 웹앱"이었으나, 적대 검수 결과 실제로 필요한 형태는 **정적 사이트 650페이지 + 인터랙티브 섬 하나**(검색 + AI 폴백)로 판정됐다(설계서 §6-1).

## Decision

**Astro + React 아일랜드로 짓고, 650개 용어 페이지는 빌드 타임 정적 생성(SSG)한다. Cloudflare Workers에 배포한다.**

1. **렌더링(D1)** — 용어 상세 650개는 **SSG**. AI가 새로 생성한 용어는 on-demand 서버 렌더 후 D1에 적재되어 이후 정적처럼 서빙된다. **CSR-only SPA는 이 목표에서 실패로 간주하고 기각한다.**
2. **프레임워크(D2)** — **Astro**. React 컴포넌트를 그대로 쓰므로 "리액트" 요구는 아일랜드 안에서 충족되고, 나머지 649페이지는 JS를 싣지 않는다.
3. **Kotlin/Wasm(CMP 웹) 기각(D3)** — CMP 웹은 **캔버스 렌더링이라 DOM이 없다.** 검색엔진이 읽을 텍스트가 없으므로 이 이행의 1차 목적에 정면으로 반한다. **이 기각 자체가 "왜 React인가"의 답이다.**
4. **호스팅(D4)** — **Cloudflare Workers.** 프록시·D1·KV와 같은 계정·같은 대시보드에서 Turnstile·rate limit을 건다. 2026-01-16 Cloudflare가 Astro를 인수해 배포·런타임 정합이 최상이다.
5. **배치(D9)** — `~/devetym/web/` 서브디렉토리. `terms.json` 경로 참조만으로 **수정 지점 1**이 구조적으로 달성된다.
6. **디자인 자산 상속** — 새로 디자인하지 않는다. 토큰 정본은 `docs/design`이 아니라 **코드**(`ui/theme/AppColors.kt`·`AppTypography.kt`·`AppDimens.kt`)이며, 여기서 CSS 커스텀 프로퍼티로 1회 추출한다. 폰트는 `composeResources/font`의 DM 7파일(OFL)을 `@font-face`로 인라인.

## Consequences

### Positive
- 650페이지가 JS 없이 즉시 렌더 → Core Web Vitals·색인 양쪽에 유리.
- 백엔드와 같은 플랫폼이라 배포·관측·한도 설정이 한 면에서 이뤄진다.
- 아일랜드 경계가 곧 복잡도 경계 — 1인 유지보수에서 관리 범위가 좁다.
- 시각 정체성이 App Store 게시본과 일치(자산 상속).

### Negative
- Astro는 팀에 신규 기술이다. Compose 경험이 이전되지 않는다.
- 인수 직후라 CF 통합의 성숙도가 검증되지 않았다 — **W0 착수 시 실측이 필요하다.**
- 용어가 수만 개로 늘면 빌드 타임이 문제가 된다(현 650개에선 무관).

### Neutral
- 웹 코드는 Kotlin 자산을 실행 코드로 재사용하지 않는다. 넘어가는 것은 값·명세·계약뿐이다(설계서 §3).

## Alternatives Considered

1. **Next.js** — 콘텐츠 사이트에 과대하고, Cloudflare에서는 어댑터를 경유해 정합이 한 단계 낮다. 1인 유지보수 비용이 크다. 기각.
2. **Remix** — 데이터 중심 앱 지향. 650개 정적 생성이 주 유스케이스가 아니다. 기각.
3. **Vite SPA(CSR-only)** — 검색엔진에 읽을 HTML을 주지 못한다. 유입 목적에 실패. 기각.
4. **Kotlin/Wasm(CMP 웹)** — 같은 코드베이스로 웹을 낼 수 있다는 유혹이 있으나 캔버스 렌더링이라 DOM이 없다. **기각(재론 금지).**
5. **기존 `site/`(Jekyll) 확장** — 순수 SEO만이면 이것으로 충분했다. 그러나 AI 폴백(동적 호출·상태·Turnstile 위젯)을 담을 수 없다. 기각하되, **"이행하지 않는 것"이 유효한 대안이었음은 기록한다**(설계서 §6-1).

## References
- 설계 정본: [`../design/web-transition-design.md`](../design/web-transition-design.md)
- **구체화**: [ADR-0013](0013-web-route-contract.md)이 위 Decision 1의 렌더링 문장을 구체화한다 — *"AI가 새로 생성한 용어는 on-demand 서버 렌더"* 가 **누구에 의해 유발되는지**를 이 ADR은 정하지 않았다. 0013의 답: **SSR 폴백은 D1 조회 전용이며 생성을 트리거하지 않는다**(크롤러 과금 차단), 색인 자격은 품질 게이트가 연다. 라우트를 짓기 전에 0013을 함께 읽을 것.
- 관련 ADR: [ADR-0010](0010-web-abuse-prevention.md)(웹 남용 방지) · [ADR-0011](0011-prompt-ownership-transfer.md)(프롬프트 소유권) · [ADR-0006](0006-server-cache-boundary.md)(서버 캐시 경계) · [ADR-0001](0001-cross-platform-framework.md)(앱 프레임워크 선택)
- 진행 상태: [`../../ROADMAP.md`](../../ROADMAP.md) W 트랙

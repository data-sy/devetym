# 🤖 웹 트랙 W — 자율주행 착수 핸드오프 (W0 → W1a → W1b)

> **성격: 콜드 세션 착수용 실행 문서.** 이 문서를 읽는 세션은 **이전 대화 맥락이 없다고 가정**한다.
> §1로 착수 가능 여부를 판정하고, §4를 실행한 뒤 §5 순서대로 구현한다. **설계를 다시 논의하지 않는다.**
>
> **작성**: 2026-08-05 (설계 보강 세션 · 브랜치 `docs/web-track-autonomy-prep`).
> **진행 상태 정본은 이 문서가 아니라 [`../../ROADMAP.md`](../../ROADMAP.md) W 트랙** — 충돌하면 ROADMAP을 신뢰한다.
> **설계 정본은 [`../design/web-transition-design.md`](../design/web-transition-design.md)** — 판정 근거는 전부 거기 있다.

---

## 0. 한 줄 요약

`~/devetym/web/`에 **Astro 6 + React 아일랜드**로 정적 사이트 650페이지를 짓고, `devetym-proxy`를 하드닝해 **웹 AI 폴백을 앱과 격리된 예산으로 개방**한다. 목적은 단 하나 — **검색 유입**이다.

**앱은 유지된다.** 이건 마이그레이션이 아니라 **채널 확장**이다(웹 = 유입·증명 표면, 앱 = 무제한 표면).

⚠️ **설계 결정은 전부 끝났다.** 결정 D1~D10 · 열린 질문 Q1·Q3·Q5 · 설계 공백 X1~X4가 근거와 함께 닫혀 있다. **재론하지 말 것.** 구현 중 설계가 틀렸다고 판단되면 **고치지 말고 §6 규율대로 사람에게 보고**한다.

---

## 1. 착수 전제 — H배치 검증. **하나라도 없으면 멈추고 사람에게 보고한다**

이 트랙은 사람 게이트를 앞으로 몰아 일괄 처리한 뒤 무인 관통하도록 설계됐다(설계서 §9-1). **자격증명이 자리에 있는지 먼저 스스로 확인한다.** 추측으로 진행하지 않는다.

| # | 확인 대상 | 확인 방법 |
|---|---|---|
| H1 | `devetym.com`이 CF 계정 DNS 아래 | `npx wrangler whoami` + CF 대시보드/API로 zone 존재 확인 |
| H2 | `CLOUDFLARE_API_TOKEN` 환경변수 | `wrangler deploy --dry-run`이 인증 통과 |
| H3 | Search Console 소유권 확인됨 | **사람 보고에 의존** — 자동 확인 불가. 없어도 W0-b·W1은 진행 가능(K1 측정만 지연) |
| H4 | Turnstile site key(공개) + `TURNSTILE_SECRET`(프록시 시크릿) | `wrangler secret list` |
| H5 | `ANTHROPIC_API_KEY_WEB`(웹 전용 워크스페이스 키) | `wrangler secret list`. **기존 `ANTHROPIC_API_KEY`는 앱용 — 건드리지 않는다** |
| H6 | CF rate limiting rule 1개 활성 | 대시보드/API 확인 |
| H7 | **ADR-0009·0010·0011이 `Accepted`** | `docs/adr/` 3파일의 Status 행 |
| H8 | Q4(한도 도달 화면 문구) 답변이 설계서에 기입됨 | 설계서 §7 Q4 |

**H7이 가장 놓치기 쉽다.** 계정 작업이 아니라서 눈에 안 띄지만, `Proposed` 상태의 ADR을 구현하는 것은 이 프로젝트의 거버넌스 규율 위반이다(ROADMAP: *"비준은 사람 승인 후"*). **`Proposed`면 W1a에 착수하지 않는다.**

**부분 착수 규칙**: H1·H2만 있으면 **W0까지는 진행 가능**하다. W1a는 H4~H7 전부, W1b는 H8까지 필요하다. 없는 게 있으면 **가능한 데까지 하고 멈춰 보고**한다 — 추측으로 메우지 않는다.

---

## 2. 정본 (재론 금지 · 이 문서에 중복하지 않는다)

| 대상 | 정본 |
|---|---|
| 설계 판정 전체 | [`docs/design/web-transition-design.md`](../design/web-transition-design.md) — 특히 **§9(자율주행 실행 계약)** |
| 진행 상태 | [`ROADMAP.md`](../../ROADMAP.md) W 트랙 |
| 웹 결정 3건 | `docs/adr/0009` · `0010` · `0011` |
| 불변식 INV-1~13 | [`docs/cache-delivery-milestones.md`](../cache-delivery-milestones.md) |
| 서버 계약 | [ADR-0006](../adr/0006-server-cache-boundary.md) (**0004는 Superseded**) |
| 콘텐츠 | `shared/src/commonMain/composeResources/files/terms.json` — **650항목. 웹이 이 파일 하나를 본다(D9, 수정 지점 1)** |
| 앱 명세(이식 대상) | 설계서 §3 판정표 + **§3-1의 숨은 규칙 10개** |
| 프록시 운영 절차 | `~/devetym-proxy/README.md` |
| 문구 정본 | `shared/.../DetailMessages.kt` (`ErrorKind` 6종 한글 메시지) |

---

## 3. 확정된 결정 (근거는 설계서에 · 여기선 결론만)

1. **전면 SSG.** 650페이지는 빌드타임 생성. **AI 생성분은 클라이언트 렌더 후 주기 재빌드로 정적 승격** — on-demand SSR을 쓰지 않는다(D1 개정). **따라서 CF 어댑터가 필요 없다** — `astro build` 산출물 `dist`를 Workers 정적 자산으로 올리는 것이 전부.
2. **Astro 6 + React 아일랜드.** Next.js·Remix·Vite SPA 기각. **Kotlin/Wasm(CMP 웹) 기각 — 재론 금지**(캔버스라 DOM이 없어 SEO 목적에 정면으로 반한다).
3. **호스팅 = Cloudflare Workers 정적 자산.** 코드 배치는 `~/devetym/web/`(같은 repo).
4. **AI 폴백을 W1에 개방하되 웹은 3건/일.** 3층 = 브라우저 3 / IP 15 / 웹 전역 30. **앱은 현행 유지**(기기 10 / 전역 200).
5. **표면 판정축은 `Origin` 헤더 단독**(설계서 §9-4). allowlist 안이면 웹 캡+웹 키, **헤더 부재면 앱 캡+앱 키**, allowlist 밖이면 차단. **웹 키는 fallback이 아니라 명시적 조건이다.**
6. **자동완성은 웹만 `keyword + aliases`**(D7). 앱은 keyword only — **의도적 분기이며 버그가 아니다.** 한글 별칭 1,097개가 유입 자산 전부이므로 앱 명세를 그대로 옮기면 한글 입력에 자동완성이 반응하지 않는다.
7. **프롬프트·도구 스키마를 `commonMain` → Worker로 이전**(Q5 해결, ADR-0011). 앱은 계속 자기 것을 보내고 **서버가 무시하고 덮는다** — `model`·`max_tokens`·`thinking`에 이미 있는 강제 덮어쓰기와 같은 패턴이라 **INV-1(앱 무변경) 유지**.
8. **개인화(히스토리·북마크·계정) 웹 v1 제외**(D10). 로컬 저장이 없으므로 단일-writer 직렬화(DR-2)도 불필요.
9. **오라클 = `web/scripts/verify-deployment.mjs`의 3검사**(§9-5). **로컬 빌드 성공은 오라클이 아니다.**
10. **러너 = vitest · 축 = W축1~4**(§9-6). `normalizeTermKey`는 프록시 구현을 **vendoring + 파일 동일성 단언**.

### 3-1. 가장 잘 깨지는 숨은 규칙 3개 (나머지는 설계서 §3-1)

- **`normalizeKeyword`는 키잉 전용이다.** AI에 보내는 질의에는 **원본 대소문자를 보존**한다. lowercase하면 `NaN`·`Go`·`REST`·`C`의 의미가 뭉개져 **어원이 조용히 오답**이 된다. 실패가 크래시가 아니라 *틀린 콘텐츠*로 나타난다.
- **category clamp는 AI 경로에만** 적용한다. 번들·캐시 경로는 clamp하지 않는다.
- **`TermResult`는 3분기다** — `Found` · `NotDevTerm` · **`PossibleTypo(suggestion)`**. 오타 제안 분기를 UI가 빠뜨리기 쉽다. 그리고 **뒤 둘은 저장하지 않는다**(lazy) — D1 오염 방지에 직결.

---

## 4. 환경 정합 — 먼저 이것부터 실행한다

```bash
# ── devetym: 설계 브랜치 위에 구현 브랜치를 스택 ─────────────────────
#    (설계서 §9와 개정된 D1이 main에 없고 이 브랜치에만 있다.
#     스택해야 설계서를 작업트리에서 읽으면서 구현을 얹을 수 있다.)
cd ~/devetym
git fetch origin
git checkout docs/web-track-autonomy-prep
git checkout -b feat/w-web-track

# ── proxy: main 최신화 후 W1a 브랜치 분기 ───────────────────────────
cd ~/devetym-proxy
git checkout main && git pull
git checkout -b feat/w1a-web-hardening
nvm use          # .nvmrc = 22. 안 하면 wrangler가 실행을 거부한다
npm ci
```

**검증 4줄** — 넷 다 통과해야 착수한다:

```bash
node -v                                   # v22.x
cd ~/devetym-proxy && npx wrangler deploy --dry-run   # 인증·바인딩 통과 (H2 확인)
npx wrangler secret list                  # ANTHROPIC_API_KEY / ANTHROPIC_API_KEY_WEB / TURNSTILE_SECRET
grep -c '"keyword"' ~/devetym/shared/src/commonMain/composeResources/files/terms.json   # 650
```

---

## 5. 실행 순서

### ⚠️ W1a가 W1b보다 반드시 먼저다

A안(AI를 W1에 개방)은 **방어와 개방을 동시에 공개하는 결정**이지, 방어 없이 먼저 여는 결정이 아니다. **W1a DoD를 통과하지 않은 채 W1b의 AI 폴백을 배포하지 않는다.**

### Phase W0 · 터 닦기 (repo: `devetym`)

1. **`web/` 스캐폴드** — Astro 6 + React 통합. `terms.json`을 **경로 참조로만** 읽는다(복사 금지 — D9의 "수정 지점 1"이 구조적으로 깨진다).
2. **디자인 토큰 추출** — `shared/.../ui/theme/AppColors.kt`(라이트/다크 각 10색)·`AppTypography.kt`·`AppDimens.kt` → CSS 커스텀 프로퍼티. **`docs/design`에 토큰 파일은 없다. 정본은 코드다.**
3. **폰트 인라인** — `composeResources/font`의 DM 7파일(OFL)을 `@font-face`로. 라이선스 동일하므로 그대로 사용.
4. **빈 사이트 배포** → `devetym.com`이 200 응답.

**W0 DoD**: **W0-b(배포·200 확인)** 만 이 세션 소관. W0-a(Search Console 소유권)는 **사람(H3)** — 묶지 않는다(설계서 §9-2).

### Phase W1a · 프록시 하드닝 (repo: `devetym-proxy`) ★ 먼저

1. **CORS** — 고정 allowlist. **동적 Origin 에코 금지**(T4).
2. **표면 판정** — §9-4 케이스 표 5건 전부 구현. `Origin` 부재 = 앱.
3. **Turnstile 검증** — 웹 *생성* 요청에만. **캐시 히트·정적 열람에는 요구하지 않는다**(유입 마찰 0).
4. **웹 3층 캡** — 브라우저 3 / IP(`CF-Connecting-IP`, 위조 불가) 15 / 웹 전역 30. 검사 순서는 **사용자 대면 → IP → 전역**(오류 문구가 달라지므로).
5. **2키 선택** — 웹은 `ANTHROPIC_API_KEY_WEB`, 앱은 기존 키.
6. **프롬프트·도구 스키마 이전**(Q5) — `ClaudePrompt.kt`(177 LOC)의 `SYSTEM_PROMPT`·`TOOLS`를 Worker로. 클라가 보낸 `system`·`tools`는 무시하고 덮는다. **`prompt_version` 해시 입력이 서버 것으로 바뀌어 상수가 된다** — 의도된 변화이니 주석으로 남긴다. `Category.CANONICAL` 손복제(`index.js:60`)도 이때 함께 정리.
7. **usage 로그에 표면 태그 추가** — F5(앱 429/402 발생률 상승) 관측 장치. **현재 이걸 보는 수단이 없다.**

**W1a DoD**: ① **기존 앱에서 정상 생성 성공**(앱 무영향 실측) ② 브라우저에서 Turnstile 토큰 없이 호출 시 차단 ③ 캐시 히트는 토큰·캡 없이 통과 ④ 프록시 vitest 전건 green.

### Phase W1b · 문 열기 (repo: `devetym`)

1. **빌드타임 인덱스** — keyword + **모든 aliases** 정규화(총 1,966키). 교차 충돌 3건(`집계`·`분기`·`샤딩`)은 검색창만 first-wins, **페이지는 가려진 엔트리도 각자 URL을 갖는다**(설계서 §3-1 3번 — 웹은 이 결함을 구조적으로 고친다).
2. **650 정적 페이지** — `/term/{slug}`. **한글 별칭을 `title`·`h1`·`alternateName`에 1급으로.** 예: `뮤텍스(mutex) — 왜 이렇게 부르는가`.
3. **검색 섬(React)** — 디바운스 300ms · trim 후 빈이면 조회 안 함 · 자동완성은 **keyword + aliases**.
4. **AI 폴백 섬(React)** — 3건 한도 · `ErrorKind` 6종 한글 매핑(**`else` 없이 전수**) · `TermResult` 3분기 · **한도 도달 시 Q4 문구 + 앱 유도**.
5. **사이트맵 · robots · 구조화 데이터** — `DefinedTerm`+`DefinedTermSet`, `/search`는 `noindex`.
6. **승격 잡** — D1의 AI 생성분을 당겨 다음 빌드에 정적 페이지로 편입.
7. **W축1~4 지그 작성** — 특히 `web/scripts/verify-deployment.mjs`(§9-5).

**W1b DoD**: **§9-5 지그 3검사(A 도달성 · B 색인 가능성 · C 한글 별칭 전수) 실패 0건** + AI 생성 왕복 1회 성공.

---

## 6. 안전 규율 — 이 세션이 **하지 않을 것**

- **push·PR·머지 금지** — 커밋까지만. GitHub은 외부 대면이라 사람이 한다. **브랜치 삭제 금지**(병합 후에도 보존).
- **배포는 허용, 단 범위 한정** — 웹 정적 자산과 프록시 배포는 이 트랙의 DoD라 허용된다(H2 토큰이 그 위임이다). **프록시 배포는 2단 롤아웃**을 따른다(README §8-2). 롤백은 코드가 아니라 설정 한 줄 + 재배포.
- **Anthropic 실호출은 왕복 검증 최대 3회까지**(약 $0.09). 그 이상은 사람 승인.
- **D1 `--remote` 마이그레이션 금지** — `--local`만. `--remote`는 되돌리기 어려우므로 **사람 승인 후에만**.
- **앱 코드 수정 금지 (INV-1)** — `shared/`·`androidApp/`·`iosApp/`은 이 트랙에서 건드리지 않는다. 프롬프트 이전은 **서버가 무시하는 방식**으로 하지 앱을 고쳐서 하지 않는다.
- **`Constants.kt`의 `privacyPolicyUrl` 교체 금지** — 순서 게이트가 있다(설계서 §5-1). ① 도메인 → ② 새 도메인에 정책 페이지 실제 게시·200 확인 → ③ 상수 교체 → ④ 스토어 라벨. **역순 금지** — 게시된 앱이 죽은 URL을 가리키게 된다. ③④는 **사람 소관**.
- **앱 한도 상수 수정 금지** — `DAILY_LIMIT`(10)·`GLOBAL_DAILY_LIMIT`(200)은 현행 유지. 웹 캡은 **별도로 추가**한다.
- **ADR·INV·설계서 수정 금지** — 이 트랙은 제약을 **소비**하지 생성하지 않는다. 결함이 보이면 **고치지 말고 보고**.
- **`~/dev/agent-harnesses/` 수정 금지** — 격리 세션 소관.
- **`site/`(Jekyll) 정책 페이지 이관 금지** — 위 순서 게이트에 묶여 있다.

---

## 7. 완료 조건

1. **W0-b** — `devetym.com` 200 응답
2. **W1a DoD 4건** 전부 통과 (특히 ① **앱 무영향 실측**)
3. **W1b DoD** — §9-5 지그 3검사 실패 0건 + AI 왕복 1회 성공
4. **W축1~4 green**
5. 두 repo 각각 커밋 완료 (**푸시 안 함**)
6. `ROADMAP.md` W 트랙에 진행 상태 갱신 — **진행 상태 정본은 디스크 로드맵**
7. 사람에게 남길 것 정리: ① PR 생성 여부 ② `--remote` 마이그레이션 승인 ③ `Constants.kt` 순서 게이트 ③④단계 ④ **Q2**(AI 생성분 색인 — W2 사안) ⑤ 구현 중 발견한 설계 결함 목록

**⚠️ 실패를 정직하게 보고한다.** 지그가 빨간데 green이라고 말하지 않는다. 부분 완료면 **무엇이 남았는지 명시**한다 — 이 프로젝트가 마일스톤마다 겪은 *"빌드는 되는데 실기동은 깨진다"* 의 웹 대응물이 *"빌드는 되는데 색인은 안 된다"*(F6)이고, 그 유일한 방어가 이 규율이다.

---

## 8. 착수 전 체크

- [ ] **§1 H배치 8건 검증** — 없는 게 있으면 어디까지 가능한지 판정하고 보고
- [ ] §4 명령 실행 + 검증 4줄 통과
- [ ] 설계서 통독 — 특히 **§2 결정표 · §3-1 숨은 규칙 10개 · §4-5 캡 3층 · §9 전체**
- [ ] `devetym-proxy/src/index.js` 581줄 통독 — 어디에 무엇을 끼워 넣는지 파악
- [ ] `TermRepository.orchestrate()` 확인 — 웹이 2계층으로 축약할 원본 분기 순서
- [ ] §6 안전 규율 숙지 — 특히 **push · 앱 코드 무변경 · `privacyPolicyUrl` 순서 게이트**

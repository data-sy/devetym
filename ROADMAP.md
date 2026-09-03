# DevEtym Roadmap (Compose Multiplatform)

DevEtym(개발 어원 사전) CMP 앱의 중장기 작업 계획이자 **진행 상태 정본**. 세부 실행 지시는 [`docs/specs/spec.md`](docs/specs/spec.md)·[`docs/architecture.md`](docs/architecture.md)·각 [ADR](docs/adr/)를 참조.

구축 원칙: **위험이 낮은 코어부터, UI는 마지막.** 거의 전부 `commonMain`, 플랫폼 조각(엔진·드라이버·셸)만 각자.

**서버 캐시·딜리버리는 별도 트랙이 아니라 M1~M8에 빌트인이다.** 3계층 read-through(`로컬/번들 → 서버 D1 캐시 → Claude API`)·local-first pinning을 처음부터 각 마일스톤 범위에 녹인다 — 나중 마이그레이션·리팩토링·"출시 후" 없음. 확정 불변식 INV-1~13과 마일스톤별 상세 스펙은 [`docs/cache-delivery-milestones.md`](docs/cache-delivery-milestones.md), 서버 코드는 별도 repo **`devetym-proxy`**(read-through 캐시로 확장), 계약 결정은 [ADR-0006](docs/adr/0006-server-cache-boundary.md).

---

## Now — 진행 중

> ## ▶ 재개 지점 — "이제 뭐 하면 돼?"에 대한 답 (2026-09-02 기준)
>
> **아래 Now 목록에는 이미 닫힌 트랙이 섞여 있다**(M0~M8·M9 iOS·코드 갭·#19 — 완료 이력 보존용). **살아 있는 것은 둘뿐이다.**
>
> | 순서 | 할 일 | 상태 | 다음 한 걸음 |
> |---|---|---|---|
> | **1** | ~~**서류 정돈**~~ | **✅ 종결 (2026-08-25)** — repo·`~/Downloads/devetym-release` 양쪽 완료 | 없음. 살아 있는 트랙은 아래 2번뿐이다 |
> | **2** | **웹 트랙 W — 「크게」** | **W0a·W0b·W0c·W1a·W1b·W1c·W2 ✅ 프로덕션 종결** (W2 2026-09-03) — 갈래 허브 6장(`/category/…`) · 빵부스러기 + `BreadcrumbList` · `DefinedTerm`이 갈래 허브를 가리킴 · 본문 언급 링크(갈래를 넘는 다리 229건). 배포 워커 **`887d5dc5`** · **실 URL 672 전수 200** · 사이트맵 **668** · 구조 검사 JSON-LD 1,335블록 문제 0 | **▶ 다음 한 걸음 = W3**(8주 실측 리뷰 — K1 색인률·K2 유입·K3 세션당 신규 검색). 사람 몫 **⑥ 사이트맵 제출은 2026-09-03 완료** — K1 계측이 돌기 시작했다. ⚠️ **8주 전에는 「유입이 안 늘었다」고 말할 수 없다**(색인 지연). 정본 = [`🤖-26-08-25-web-large-track-handoff.md`](🤖-26-08-25-web-large-track-handoff.md) |
> | **3** | 🔴 **[사람] 프록시→Anthropic 간헐 403** | **미해결 · 라이브 앱도 영향** — 같은 요청이 1·2회차 403(`{"error":{"type":"forbidden","message":"Request not allowed"}}`) 3회차 200. Anthropic 자체 오류 shape이 아니라 그 **앞단** 형식이고, **앱 키·웹 키 양쪽에서 같이** 난다 | 웹은 「다시 시도」로 완화했으나 **앱에는 그 완화가 없다** — 사용자에겐 "결과를 불러오지 못했어요"로 끝난다. 사람이 Anthropic Console(키 상태·워크스페이스 제한)을 확인할 것 |
>
> **완료된 W 트랙 절차는 [Done](#done--완료)으로 내렸다.** 브랜치 전용 SSOT(`w0c-sandbox-roadmap.md`)는 흡수 커밋에서 삭제했고, 살아남을 값은 핸드오프 §4「인용해도 되는 실측값」·§5「함정」으로 옮겼다. 세부 SQL·리허설은 원격 브랜치 `sandbox/w0c-d1-seeding`(양쪽 repo)·`feat/w1a-proxy-hardening` 커밋 이력에 있다.
>
> **1번 주의**: 그 폴더는 repo 밖이라 *"git 이력이 보존하므로 삭제는 손실이 아니다"* 전제가 **절반만 성립**한다(이관 후 생성분은 git에 없다). 그래서 삭제 전에 스냅샷을 떴다 — 그 tar.gz가 유일한 복구 경로다. 헌장은 2026-08-25 정돈 실행으로 소진돼 삭제했다(`git show 0d4c57e:docs/handoff/26-07-14-doc-pruning-backlog.md`). 실행 결과는 아래 「서류 정돈 트랙」.
>
> **2번 — 무엇이 정해졌나 (2026-08-25)**: 사람이 **「크게」**(650장 + 웹 AI·3~4주)를 골랐다. 근거는 검색 수요 실측이 아니라 **구조** — *"어차피 웹에서도 AI가 돌게 하는 게 궁극적으로 원하는 형태"*. 그러면 「중간」은 지나갈 중간역일 뿐이고 승급 비용만 든다. **→ 「검색 수요 선실측」(반나절 런북)은 취소**한다. 그 실측은 「중간 vs 작게」를 가르는 장치였고 이 결정이 그 위를 지나갔다(런북은 `docs/handoff/archive/26-08-24-search-demand-spike-runbook.html`로 내려 보존).
>
> ### 🙋 사람이 해야 하는 것 (2026-09-02 갱신)
>
> **AI가 대신 못 하는 것만 남겼다.** 순서는 위가 급한 순.
>
> **✅ 소진된 항목 — 완료 이력만 남긴다** (절차는 다 썼으므로 본문에서 털었다)
>
> | | 무엇 | 완료 |
> |---|---|---|
> | ⓪ | `wrangler.toml` 좌표 복원 + W0c 원격 적용 ①~⑥ + 양쪽 repo 푸시(`main`·W1a·샌드박스 브랜치) | 2026-09-01 · 09-02 |
> | ① | ADR-0012·0013 비준 → 둘 다 `Accepted`. 함께 정해진 것 = **승격 잡을 W1c로 W 트랙 안에서 닫는다** | 2026-08-25 |
> | ② | Apple Developer Program 갱신일 확인 → 만료 `2027-07-08` | 2026-08-25 |
> | ③ | Search Console 소유권 확인 → 도메인 속성 등록, **계측 가동 중** | 2026-08-25 |
> | ⑦ | W1a 켜기(Turnstile 위젯 · Anthropic `DevEtym-Web` 워크스페이스+$20 · 시크릿 3개 · 배포 · **실기기 생성 1건**) | 2026-09-02 |
> | ⑧⑨ | W1c 원격 반영 — D1 시딩(authored 650→**660**) + 웹 재배포 `16ac69f8`. ⚠️ **1회차가 조용히 실패**했고 판정은 명령 출력이 아니라 D1 조회였다(함정 27) | 2026-09-02 |
> | ⑥ | **Search Console 사이트맵 제출**(`sitemap-index.xml` · 668 URL) — **K1 측정 배선이 닫혔다.** W3가 이제 성립한다 | 2026-09-03 |
>
> **⚠️ ②의 잔여**: **2027-06-08**(만료 한 달 전) 캘린더 리마인더 등록 · 자동 갱신 켜져 있는지 Membership details에서 확인 — 꺼져 있으면 리마인더가 유일한 방어선이다.
> **덤(선택)**: **네이버 서치어드바이저**(<https://searchadvisor.naver.com>)도 등록할 값이 있다. 한글 별칭 1,097개가 이 트랙의 핵심 자산이라 네이버 색인이 구글만큼 중요하다.
>
> ---
>
> **아래 둘이 살아 있는 전부다.** 급한 순은 ④(중복 콘텐츠) → ⑤(선택).
>
> **④ `www` → apex 301 Redirect Rule** — *지금 www가 200으로 같은 내용을 서빙해 중복 콘텐츠다(canonical로만 완화 중).* **2026-09-02 재확인**: `www.devetym.com/`·`www.devetym.com/term/mutex` 모두 200이고 canonical만 apex를 가리킨다. 페이지가 **666장**(용어 660 + 갈래 허브 6)이므로 중복 대상도 그만큼이다 — 그리고 사이트맵을 제출한 지금은 **색인이 실제로 도는 구간**이라 우선순위가 한 번 더 올라갔다.
> Cloudflare 대시보드 → `devetym.com` → **Rules** → **Redirect Rules** → **Create rule**
> - 이름: `www to apex`
> - 조건(Custom filter expression): `Hostname` `equals` `www.devetym.com`
> - 동작: **Dynamic** → 식 `concat("https://devetym.com", http.request.uri.path)`
> - 상태 코드: **301** · **Preserve query string** 체크
> - 저장 후 확인: `curl -sI https://www.devetym.com/` 가 `301`이면 성공.
> ⚠️ **Astro 미들웨어로는 못 고친다**(실측 — `_routes.json`이 prerender 경로를 Worker에서 제외). 무료 플랜 Redirect Rule 10개 중 1개를 쓴다.
>
> **⑤ (선택) 앱 방침 URL 이전** — 순서를 지켜야 한다. ① `devetym.com`에 정책 페이지 **실게시·200 확인** → ② `Constants.kt`의 `privacyPolicyUrl` 교체 → ③ App Store 라벨 갱신. **역순이면 게시된 iOS 앱이 죽은 URL을 가리킨다.** 현행 라이브(`data-sy.github.io/devetym/privacy-policy`)는 살아 있으므로 급하지 않다. ①은 Claude가 할 수 있다 — 지시하면 된다.
>
> **✅ 결정 이력 (소진 — 정본은 ADR)**: AI 크롤러 허용 여부는 Cloudflare 기본값이 이미
> 「학습 수집기 차단 / 인용·검색 크롤러 허용」로 갈라 놓아 **손댈 것 없음**. 구조 질문 2건은
> [ADR-0012](docs/adr/0012-content-canon-d1.md)(D1 = 콘텐츠 정본)·[ADR-0013](docs/adr/0013-web-route-contract.md)(SSG + 조회 전용 SSR)로 닫혔고,
> 스택·남용 방지·프롬프트 소유권은 [ADR-0009](docs/adr/0009-web-framework-rendering.md)·[ADR-0010](docs/adr/0010-web-abuse-prevention.md)·[ADR-0011](docs/adr/0011-prompt-ownership-transfer.md)로 닫혔다(전부 `Accepted`).
> 눈 뜨고 수용한 대가와 대안 기각 사유는 각 ADR 안에 있다 — 여기 복제하지 않는다.
> 도메인은 2026-08-25 구매·결선 완료(`SITE_URL` 단일 지점 덕에 교체는 환경변수 한 줄, 코드 수정 0).
>
> **정직하게 남는 것**: 한국어 어원 검색 수요는 **여전히 미검증**이며, 이제 그 위험을 **눈 뜨고 수용**한 상태다 — 사전 측정으로 닫지 않고 **W3 실측(색인률·유입)으로 사후 확인**한다. 다만 웹의 값이 검색 단독에 걸려 있지는 않다: **씨딩 착지면**과 **웹 AI 자체**는 검색 수요와 무관하게 성립한다. 대신 **한글 별칭 1,097개를 title·h1·구조화 데이터에 1급으로 올리는 작업은 필수**다 — 안 하면 650장은 한국어 검색에 사실상 존재하지 않는다.
>
> 〔2026-08-21 전달 형식 교훈: 사람에게 **「감사 0/26」·「L0-1」·ADR 상태 같은 과정 어휘로 설명하면 오히려 헷갈려 한다.** 결정과 트레이드오프 언어로 말하고, 참고 자료는 **한 번에 한 탭만** 띄운다. 과정 어휘는 핸드오프·원장 안에만 둔다.〕
>
> **그 외는 전부 대기열이다** — 백로그(`Later`)의 `[Docs·규범]`은 해소됨, `[Ops]`·`[Build]`·이슈 [#15](https://github.com/data-sy/devetym/issues/15)·[#16](https://github.com/data-sy/devetym/issues/16)·WU-7·F Android는 착수 신호가 오기 전엔 잠들어 있다.


- **🌐 웹(React) 이행 트랙 W — Now에 남은 것은 W3뿐이다**(W2까지 프로덕션 종결 2026-09-03). 유입(acquisition) 목적의 채널 확장이고 **앱은 유지**한다(마이그레이션 아님). 규모는 「크게」(650장 정적 + 웹 AI)로 2026-08-05 사람 확정 — **재론 금지**.
  - **완료분은 [Done](#done--완료)으로 내렸다** — W0a 기반 · W0b 도메인 결선 · W0c 650 D1 시딩 · W1a 프록시 하드닝 · W1b 웹 본체. 실측값·함정·설계 전제는 **[핸드오프](🤖-26-08-25-web-large-track-handoff.md)가 정본**이다(§4 인용해도 되는 실측값 · §5 다시 밟지 말 것 · §3 하지 말 것).
  - **착수 전 반드시 아는 것 3가지** (전부 핸드오프에 근거 있음):
    - **한글 별칭 1,097개가 이 트랙의 핵심 자산이다.** 표제어 650개는 전부 영어이고 한글 keyword는 **0개**다. 한국인은 `뮤텍스 어원`으로 검색하므로 별칭을 title·h1·구조화 데이터에 **1급으로** 올리지 않으면 650장은 한국어 검색에 존재하지 않는다.
    - **오라클 규율(설계서 F6)**: 이 프로젝트가 마일스톤마다 겪은 *"빌드는 되는데 실기동은 깨진다"*의 웹 대응물은 **"빌드는 되는데 색인은 안 된다"**이다. **로컬 빌드 성공은 오라클이 아니다** — 배포된 실 URL 650개 전수 200 + Search Console 색인 확인이 오라클이다.
    - **씨딩이 이 트랙에 종속돼 있다**(2026-08-19 사람 결정). 긱뉴스·OKKY·인프런 착지면을 `devetym.com`으로 잡는다 — **발사는 W1b 완료 후**다. 복붙본은 `~/Downloads/devetym-release/`에 있고 **폐기 금지**.
  - **채택 스택**: Astro + React 아일랜드 / SSG 650페이지 / Cloudflare Workers. Next.js·Remix·Vite SPA·Kotlin/Wasm 기각([ADR-0009](docs/adr/0009-web-framework-rendering.md)). 코드 배치 `~/devetym/web/`(같은 repo — `terms.json` 경로 참조만으로 수정 지점 1).
  - **마일스톤**: ~~W0(도메인·스캐폴드·토큰 추출)~~ ✅ → ~~W0c(650 D1 시딩)~~ ✅ → ~~W1a(프록시 하드닝)~~ ✅ → ~~W1b 정적 650 + 검색 + AI 폴백~~ ✅ 2026-09-02 → ~~**W1c** 승격 잡~~ ✅ 2026-09-02(= 캐시 M5. 라운드 001 · 생성분 10건 승격 · 자격 규범 = [ADR-0014](docs/adr/0014-promotion-eligibility.md)) → ~~**W2** 허브·구조화 데이터·얇은 콘텐츠~~ ✅ 2026-09-03(갈래 허브 6 · 빵부스러기 · 갈래를 넘는 언급 링크 · 설계서 §5-2의 3수단 전부 가동 · 배포 `887d5dc5`) → **W3** ← 지금 8주 실측 리뷰.
  - **⚠️ 정직하게 남는 위험**: 한국어 어원 검색 수요는 **여전히 미검증**이고, 사전 측정 없이 **눈 뜨고 수용**한 상태다 — W3 실측(색인률·유입)으로 **사후** 판정한다. 다만 웹의 값이 검색 단독에 걸려 있지는 않다: 씨딩 착지면과 웹 AI 자체는 검색 수요와 무관하게 성립한다.

- **📂 서류 정돈 트랙 — ✅ 종결 (2026-08-25 실행).** 헌장(과거 트래킹도 YAGNI · git 이력이 보존하므로 삭제는 손실이 아니다 · 남기는 문서의 기준 = cold-start 자기완결성)에 따라 실행 완료. 결과(repo): 문서 **86 → 46**(핸드오프·완료 원장·db-expand 실행기록·역할 종료 프롬프트 삭제, `docs/handoff/` 폴더 소멸), ROADMAP **142KB → 83KB**(닫힌 트랙 서사를 Done 한 줄씩으로 압축), **깨진 링크 0 검증**. 규범 문서(specs·ADR·architecture·INV·prd·design)는 손대지 않았고, 링크 수리가 불가피한 2건(ADR-0007 참조 2곳·설계서 산출 근거 1줄)은 **결정 문면은 그대로 두고 링크만** 걷어냈다. 결과(repo 밖): `~/Downloads/devetym-release` **30 → 6건**(72K) — 씨딩 복붙본 2·그로스 플랜·스토어 메타·핫픽스 런북·README만 남겼다. 삭제분 24건은 **스냅샷 `~/Downloads/devetym-release-snapshot-2026-08-25.tar.gz`**(2.4M·32항목·gzip 무결성 확인)에 통째로 보존.

- **⏸️ [외부][사람] F · Android 첫 배포 — 보류 (2026-08-05 사용자 결정. 폐기 아님·우선순위 강등).** Play Console·AAB·keystore. ⚠️ **개인 개발자 계정(2023-11 이후 생성)은 프로덕션 전 폐쇄 테스트 필수** — 최소 20명 테스터 × 14일 연속.
  - **보류 사유**: 웹(W 트랙)으로 안드로이드 유저를 URL로 커버한다. Play가 더 주는 건 스토어 검색 노출·설치형 신뢰감 둘뿐이고, 폐쇄 테스트 게이트는 TWA로 감싸도 동일 적용이라 웹앱화가 우회해주지 않는다.
  - **재개 트리거**: 웹 유입이 붙은 뒤 "안드로이드 앱은 없나요"류 반응이 나오면 재개.
  - **잔여 블로커(보류 상태로 보존)**: 스크린샷 미촬영 · `androidApp/build.gradle.kts` release `signingConfig` 미배선·keystore 미생성 · Play Console 계정/과금 미확인 · 베타(TestFlight/내부)·초기 리뷰 확보 전략·단계적 출시 비율 전부 미수립.
  - **실무 자료 10건**(서명·프리플라이트·스샷·스모크·a11y·아이콘·메타 프롬프트)은 2026-08-25 정돈에서 삭제했다. 재개 시 꺼내는 법: `tar -xzf ~/Downloads/devetym-release-snapshot-2026-08-25.tar.gz -C ~/Downloads devetym-release/보류-android` (또는 `git show b4d6b3e^:docs/release/<옛이름>`). **꺼내도 재검증부터** — Play 콘솔 UI·정책·폐쇄 테스트 요건이 바뀌었을 수 있다. 다시 살릴 땐 재개 시점 기준으로 검증부터 할 것(콘솔 UI·정책이 바뀐다).
  - CI(GitHub Actions) 빌드 자동화 검토도 함께 보류.

- **🟡 [사람] WU-7 · 원본 repo `~/dev-etymology` 폐기 — 마지막 잔여 이관 항목.** 자기완결화 트랙(2026-07-10 착수)의 WU-1~6·8~10은 전건 완료됐고 **의존 0이 실측으로 확증**됐다(런타임·빌드·CI·자산). 남은 것은 폐기 실행 하나이며 **사람 최종 확인** 몫이다.
  - ⚠️ **선행 게이트 — 구직 서류 repo 링크 갱신(2026-07-17 등재)**: 사람인·원티드 실물 텍스트·PDF·career-ops SSOT가 `github.com/data-sy/dev-etymology`를 참조 중이라, `data-sy/devetym`으로 교체 완료 전에 GitHub repo를 지우면 **라이브 서류 링크가 404**. 절차 정본 = job-apply-artifacts ROADMAP 백로그 「서류 AI 파트 repo 링크 갱신」.
  - ⚠️ 원본에만 있는 자산(iOS appiconset PNG·icon-dark·LaunchLogo·typography-review·design-followup·`.claude` 원본)을 뽑아둘지 먼저 결정. **devetym 브랜치 보존 규율과는 별개 건**이다(그건 이 repo 얘기).

- **🖥️ 검증 환경 디스크 산출물 — 지우지 말 것 (git 밖, 2026-08-19 실측).** CoreSimulator 3.5G · `~/.gradle` 3.8G · `devetym.avd` · **iOS 26.5 시뮬 런타임 8.52G**(2026-08-15 #19 실기기 검증용으로 받음). 이 런타임이 있어야 iOS 시뮬 축이 다시 선다 — 용량 회수 목적으로 지우면 다음 검증에서 다시 8.5G를 받아야 한다.

- **⚙️ 정책 (2026-07-05) — 구현 전 사람 비준 게이트 완화.** M1·M2에서 eyes-open 수용이 러버스탬프였음을 확인하고 사용자가 게이트를 제거했다. **적대 비준 수렴/ESCALATE → Claude가 잔여 residual을 eyes-open 수용 → 구현·4축 green까지 자율 관통**, 사람 리뷰는 **완성물 아침 리뷰**가 체크포인트. 남아 있는 안전선: push·스토어 게시 등 **외부 대면 행위는 사람 지시로만**, 브랜치 보존, 규범 문서 자동 수정 금지.

---

---

## 브랜치·공개 전략 (defer + stacked) — 2026-07-05 결정

**GitHub repo `data-sy/devetym` — 2026-07-10 생성(private)·스택 PR 병합 → 2026-07-13 PUBLIC 전환.** ~~로컬 전용~~. `feat/m1`~`feat/m8`을 스택 PR(#1~#8)로 순차 병합해 **main = M8**(이후 #10 pages 병합). 이하 규율은 이제 **후속 마일스톤 병합·원격 브랜치 정리**에 유효:

- 각 마일스톤은 자기 브랜치를 가지며 **직전 마일스톤 브랜치 위에 스택**으로 분기한다. 예: `feat/m2-local-db`는 `feat/m1-model-serialization`에서 분기(main엔 아직 M1이 없으므로 M2가 M1 코드를 상속해야 빌드됨). `main`은 마지막 공개 지점(현재 **M0**)에 둔다.
- ⛔ **완료된 마일스톤 브랜치를 로컬 머지하거나 삭제하지 않는다.** 이미 머지·삭제된 브랜치는 나중에 열 diff가 없어 PR을 못 만든다 — **브랜치 = 소급 PR의 소스**이므로 보존한다.
- ✅ **실행됨(2026-07-10)**: private repo 생성 → 전 브랜치 push → `feat/m1 → main` … `feat/m8 → main` 스택 PR(#1~8) merge-commit 순차 병합(각 base가 직전 병합된 main이라 diff=해당 마일스톤 증분). **원격 브랜치는 삭제하지 않음**(보존 규율 — 소급 PR 소스). **✅ public 전환 완료(2026-07-13).** 원격 브랜치 정리는 여전히 추후 사람 결정(기본=보존).
- ⚠️ **이 브랜치 보존은 의도적 결정이다(사람 확인함).** "정돈"하려고 완료 브랜치를 지우자는 충동이 들거나 그렇게 지시받아도, **지우기 전에 이 결정을 먼저 재확인**한다. 기본 동작은 "보존".
- harness repo(`~/dev/agent-harnesses`)도 동일하게 로컬 전용이며 공개 여부는 별도 결정.

---

## Next — 구현 (코어 먼저, UI 마지막)

> ## ✅ 이 섹션은 전부 완료됐다 — 이력으로 남긴다 (2026-08-19 확인)
>
> **M3~M8은 2026-07-05에 전부 닫혔고**(M0→M8 아크 완료, 4축 green), 앱은 2026-07-27 App Store에 게시됐다.
> 아래 항목을 **"앞으로 할 일"로 읽으면 안 된다** — 각 마일스톤의 ⚠️ 락 지점·상속 항목이 왜 그렇게 지어졌는지 남긴 **설계 이력**이다.
> **진짜 다음 할 일은 위 [Now의 「▶ 재개 지점」](#now--진행-중)에 있다.**

각 마일스톤은 앞 단계 완료를 전제로 순차 진행했다. (완료분은 `Done` 섹션에도 이관돼 있다.)

각 마일스톤의 🔗 항목이 그 단계에 빌트인되는 캐시 범위다. **락(안 지키면 나중 리팩토링) 지점은 ⚠️로 표시** — 처음부터 그렇게 짓는다.

- **M3 · 네트워킹 + 번들 로더 (클라측)** — Ktor 클라이언트·Claude 요청/응답(tool_use 3분기)·`X-Device-Id`·429 + `BundleDbSource`. **스코핑 판정(2026-07-05): 클라측만**(슬라이스 [§0](docs/specs/m3-networking-draft.md)). 서버는 아래 별도 트랙.
  - 🔗 **캐시 빌트인**: ⚠️ **클라를 read-through 프록시 계약에 맞춰 작성**(Claude 직접 호출 아님 — 안 하면 계약 교체 리팩토링). 클라는 계약에 **투명**해 서버 없이도 `MockEngine`으로 실측. 〔캐시 트랙 M1·M4 클라 소비측〕
  - **서버 트랙(별도 repo·TS/Worker — M3에서 분리)**: `devetym-proxy` 신규 구축 — D1 스키마·Worker read-through(D1→API·write-back·first-write-wins)·single-flight(DO)·validator write-게이트·rate-limit/남용/무효화·**INV-13 정규화-후-캐시쓰기**. 클라 M3와 병렬/후속, 자체 green 오라클. 〔캐시 트랙 M0서버·M1·M2·M3write·M7〕
  - ⚠️ 계약 변경: 프록시 → read-through 캐시. [ADR-0006](docs/adr/0006-server-cache-boundary.md)(ADR-0004 대체). 참조: spec 2-1·2-2.
  - ⚠️ **INV-A wire측 로더 실측 상속(M1 DR-1 바인딩)**: M1이 실제 `terms.json` 디코드로 wire 키 계약을 fixture 실측했으나(슬라이스 §6), **번들 로더(`BundleDbSource`)의 실제 로드 경로**가 aliases 내용을 보존하는지는 **M3 DoD에서 회귀 가드로 테스트**한다 — 실제 배포 `terms.json`을 로더로 로드해 알려진 term의 aliases *내용*을 단언(성공 디코드는 무효 오라클). 근거: M1 슬라이스 §7-4·§8, DR-1 eyes-open 수용.
  - ⚠️ **서버 read-through category 소유(M1 DR-2 바인딩) — 서버 트랙으로 이관**: 클라 M3 스코핑 분리(2026-07-05)로 이 항목은 **서버 트랙 DoD**로 옮겨졌다. 서버가 정규화 이전 원응답을 캐시-히트로 되돌려 클라 정규화를 우회하지 않도록 **정규화-후-캐시쓰기 순서**를 고정한다(집합 밖 category clamp 후 write-back). 클라측 상보 방어(수신 category 정규화)는 **M4**. 정본 불변식: [cache-delivery-milestones](docs/cache-delivery-milestones.md) **INV-13**. 근거: M1 슬라이스 §7-2, DR-2 eyes-open 수용, M3 슬라이스 §4·§7-6.
- **M4 · Repository 오케스트레이터** — `fetch` 3단 흐름·upsert·북마크·히스토리·Analytics. Fake 협력자 테스트.
  - 🔗 **캐시 빌트인**: ⚠️ **3계층 read-through를 처음부터**(로컬/번들 → 네트워크 → 서버 D1 캐시 → API, 2계층으로 짓고 확장 금지). **local-first pinning + 명시적 새로고침** 경로 내장. 〔INV-1·INV-2·INV-6·캐시 트랙 M1소비·M4행위〕 참조: spec 2-3·2-4.
  - ⚠️ **upsert 보존 목록 상속(M2 DR-M2-2)**: `INSERT OR REPLACE`=DELETE+INSERT라 refresh 시 `createdAt`을 `isBookmarked`/`source`와 **함께 보존**해야 `bookmarked`(`createdAt DESC`)가 새로고침마다 조용히 재정렬되지 않는다. pinned(`seenAt`) 로우는 `fetch`가 덮지 않고 `refresh`만 갱신. `toEntity`는 4 DB전용 필드가 필수인자라 read-modify-write 재주입 누락이 **컴파일 에러**(M2가 강제). 근거: M2 슬라이스 §3-2·§3-4·Open Questions.
  - ⚠️ **schemaVersion Int 범위 보장 상속(M2 DR-M2-3)**: `Term.toDto()`의 `Long?→Int?`는 Int 범위에서만 무손실. 서버 배달 경로가 `Int.MAX_VALUE` 초과 `schemaVersion`을 기록하면 silent 절단 → M4/캐시 트랙이 Int 범위 보장(또는 `toDto` 범위 가드). 근거: M2 슬라이스 §4(INV-9)·Open Questions.
- **M5 · ViewModel + StateFlow** — 화면 상태를 sealed로 노출.
  - 🔗 pinned/refresh 상태 노출. 참조: architecture §4.5.
- **M6 · Compose UI** — 검색/상세/북마크/히스토리/온보딩/설정. **반응형 `Flow`로 갱신(수동 재조회 없음, [ADR-0002](docs/adr/0002-code-idiom-principle.md))**.
  - 🔗 **명시적 "새로고침" 어포던스**(INV-6, 본 항목 불변 + 사용자 트리거 갱신)·pinned 표시. 참조: spec 3-x.
  - ~~선행: **디자인 토큰 확정**(`docs/design/`, 작성 예정)~~ → **무효**(2026-08-05 실측·2026-08-19 재확인). 토큰은 `docs/design/`에 있었던 적이 없고 **정본은 코드**다 — `ui/theme/AppColors.kt`·`AppTypography.kt`·`AppDimens.kt`. 웹 트랙 W0의 「토큰 추출」은 이 코드에서 CSS로 뽑는 작업을 뜻한다.
- **M7 · 배선·셸** — Koin 조립 마무리, 셸별 권한·진입점.
  - 🔗 서버 배포 배선(`devetym-proxy` wrangler). 참조: architecture §3·§4.7.
- **M8 · 통합·마무리** — 오류 처리 통합·접근성·번들 DB 650(iOS 자산 재사용)·앱 아이콘(Android adaptive + iOS)·스플래시.
  - ℹ️ **번들은 이미 완성돼 있다**(저술 불필요, *재사용*만): `~/dev-etymology/DevEtym/DevEtym/Resources/terms.json` — **650개**, M1 `TermEntry`와 스키마 정합(6필드 + 버전 필드는 없음 → INV-B null default 경로). 카테고리 6집합 분포 균등. 배치는 **M1 구현 착수 시** `commonMain/composeResources`(spec 1-5)로.
  - 🔗 **캐시 빌트인**: **seed 승격 잡**(critic 배치, D1 hot 항목 → 번들 승격 플라이휠)·**콘텐츠 팩 백그라운드 동기화**(버전드 팩·delta/cursor 증분·로컬 병합) 메커니즘 내장 → **출시 1일차부터 가동**(데이터는 릴리즈마다 축적, 리팩토링 아님). 〔INV-11·INV-12·캐시 트랙 M5·M6·M3critic〕 참조: spec 4-x.
  - ⚠️ **`NativeSqliteDriver` 실행 정확성 실측 상속(M2 DR-1 잔여 절반, B1)**: M2 §6-B DB 왕복은 JVM(JDBC) 전용이라 네이티브 DB 실행(스키마 create·`INSERT OR REPLACE`·`ORDER BY`/`LIMIT`·nullable INTEGER 바인드·TEXT 정렬 로케일)은 무측정. B2(네이티브 드라이버 크로스타깃 테스트) 미채택 → **통합/실기기 실행에서 실측**한다. 근거: M2 슬라이스 §5·Open Questions(사람 게이트 추적).

---

## Later — 출시 후 백로그

출시 게이트가 아니라 **출시 이후** 착수하는 항목(미착수/검토). 〔2026-08-19: 종전 머리말이 가리키던 '진행 중 트랙'은 **코드 갭=완료 · 이관=WU-7만 잔여 · M9 iOS=종결**이다. 살아 있는 것은 위 **Now**의 「▶ 재개 지점」 둘뿐.〕 정리: 구 #1·#3·#4·#12 → 이관 트랙 · #2 → M9 [외부] · #9·#10·#11 → 코드 갭 트랙.

### W1b에서 이월된 항목 (2026-09-02)

- **[P1 · 앱] 오류 화면에 재시도가 없다.** `DetailScreen.kt:136`의 `DetailUiState.Error`는
  `MessageBody(…, "돌아가기", onBack)` 하나뿐이다 — **재시도 버튼이 없다**(실측 확인).
  프록시→Anthropic 간헐 403(위 Now 3번)이 살아 있는 동안, 앱 사용자는 **한 번 실패하면 막다른 길**이다.
  웹은 W1b에서 「다시 시도」를 붙여 완화했으므로 지금 **두 채널의 회복력이 비대칭**이다.
  ⚠️ 실패한 호출은 한도에 가산되지 않으므로(프록시가 `upstream.ok`일 때만 카운터를 올린다)
  재시도가 사용자 몫을 깎지 않는다 — 이 사실이 이 항목의 전제다.
  ⚠️ **앱 릴리스가 필요하다** — 웹처럼 1분에 못 고친다. 간헐 403의 원인이 먼저 닫히면
  우선순위가 내려갈 수 있으니, Now 3번 조사 결과를 보고 착수 여부를 정한다.

- **[P2 · 웹] 웹 화면을 앱과 같은 디자인으로 맞춘다.** 지금 **공유되는 것은 디자인 토큰뿐**이다 —
  색 11개·치수·타이포 21종은 앱 Kotlin 정본에서 빌드마다 자동 추출되므로 어긋날 수 없다
  (`web/scripts/extract-tokens.mjs`가 개수를 단언해 드리프트 시 빌드가 깨진다).
  **어긋나 있는 것은 화면 구성**이다 — 웹은 검색 홈·상세·전체 색인이라는 웹 고유 구조로 짰고,
  앱의 탭 구조·카드 레이아웃·상세 화면 배치를 따르지 않는다. 같은 물건으로 보이게 하려면
  컴포넌트 단위(용어 카드·상세 헤더·배지·액션)를 앱 쪽에 맞춰야 한다.
  ⚠️ **`src/styles/tokens.css`를 손으로 고치는 방식은 안 된다** — 생성물이라 다음 빌드에 덮인다.
  ⚠️ **`web/README.md` 「웹이 앱과 의도적으로 다른 곳」 표의 5건은 맞추면 안 된다**
  (본문 폰트·DM Sans 미탑재·자동완성 별칭 포함·교차 충돌 둘 다 노출·정규화 이식본).
  "앱이랑 맞추자"로 이걸 되돌리면 한국어 검색이 조용히 죽는다.

- **[P2 · 웹] 정적 페이지 캐시 헤더가 `max-age=0, must-revalidate`다.** Cloudflare Assets 기본값이며
  ETag 재검증으로 동작하므로 틀리지는 않지만, 650장이 매 방문마다 왕복을 한 번 한다.
  `_headers`나 `[assets] headers`로 해시 없는 HTML에 짧은 TTL을 주는 게 나은지 판정할 것.

- **[P3 · 웹] `_routes.json`이 무의미하게 생성된다.** Astro Cloudflare 어댑터가 Pages용
  `_routes.json`을 굽는데, 우리는 Workers(main + assets) 배포라 **이 파일이 쓰이지 않는다**
  (실측: `/term/*`가 exclude에 있는데도 미보유 용어가 워커에 도달했다). 지금은 무해하나,
  이 파일을 근거로 라우팅을 추론하면 틀린 결론이 나온다 — 그 함정만 기억할 것.

### W0c에서 이월된 미해결 항목 (2026-09-01 · 샌드박스 SSOT 소멸에 따라 이관)

- **[W1c 착수 전 판정 · 규범 변경 가능] 승격 잡의 입력 필터에 `branch`가 필요하다.**
  [ADR-0013](docs/adr/0013-web-route-contract.md)은 "생성분도 페이지가 될 자격이 있다"를 전제하는데,
  프로덕션 generated **21행 중 14행은 페이지가 되면 안 된다**(`not_dev_term` 13 · `possible_typo` 1).
  `origin='authored'`만으로 색인 자격을 가르면 승격 시 이 행들이 섞인다. 규범 변경이면 새 ADR.
- **[W1a에서 재검토] 계정 분리 또는 DB 스코프 API 토큰.** `d1 execute`가 DB 이름을 설정이 아니라
  **계정 전체**에서 해석하는 구멍은 이 둘로만 닫힌다. W0c 규모엔 과하다고 판정했다.
- **[P2] `entry_versions`를 아무도 안 치운다 — 🔴 이제 가설이 아니다.** W1c 승격 시딩(2026-09-02)이
  첫 write를 냈고 **한 번에 660행**이 쌓였다(승격으로 밀려난 generated 10 + 센티널이 바뀌어
  재기록된 authored 650). 스냅샷 태깅이라 **승격 라운드 1회 = 번들 전량 아카이브**다.
  번들 개정 1회당 660행이 쌓이고 프루닝 주체가 없다.
  스냅샷 단위 태깅의 알려진 대가다(엔트리별 해시로 가면 줄지만 "스냅샷 X 전체 무효화"를 잃는다).
- **[P2 · W1a 필요] Anthropic 로컬 스텁.** `wrangler dev --local`의 캐시 **미스**는 진짜 API로 나간다
  (자리표시자 키라 401로 거절되지만 요청 자체는 나간다). W1a에서 웹 경로를 실측할 때 필요해진다.
- **[P3 · 관찰] 번들 경로와 캐시 경로가 같은 용어에 다른 `promptVersion`을 준다.**
  번들에서 읽으면 `null`, 캐시에서 받으면 `authored:efa8f264dc67`. 앱은 이 값으로 분기하지 않고
  저장만 하므로(실측) 지금은 무해하다. "버전 다르면 갱신" 류 로직을 넣는 날 첫 지뢰가 된다.
- **[판정 대기] `devetym-cache-dev`를 유지할 것인가.** W0c §3-7 리허설용으로 만든 프로덕션 클론
  (무료 3/10). 이번에 배포 순서 문제를 잡아낸 값이 있었다. 방치하면 실제와 갈라져 "리허설했다"는
  착각의 근거가 된다 → **유지하되 W1a 착수 시 프로덕션에서 다시 클론**하는 쪽을 권한다(클론 2분).

### [P1·최우선] 번들 위 원격 오버라이드 오버레이 계층

문제: 조회가 번들(terms.json) 최상위(오프라인·즉시) 구조라, 번들 용어에
오류가 있으면 서버로 못 고치고 새 빌드+심사가 필요함(daemon/bug가 그 사례).
목표: 번들을 유지한 채 리빌드 없이 개별 용어를 서버에서 교정. "구워진
기본값 위 원격 오버레이" 패턴.

MVP 스펙:
1. 매니페스트 소스: 기존 프록시(Cloudflare Worker)에 GET /overrides 추가.
   응답 JSON = { version:int, updatedAt:iso,
   overrides: { "<termKey>": { <terms.json 항목 동일 스키마>, overrideVersion:int } } }.
   저장은 기존 D1(또는 KV), 교정된 용어만 담아 작게 유지.
2. 클라이언트 페치: 앱 시작 시 non-blocking으로 fetch → 로컬 캐시 + TTL(24h)
   + version/ETag 조건부 갱신. UI 막지 않음. 오프라인이면 마지막 캐시,
   없으면 오버라이드 없이 진행.
3. orchestrate() 분기(TermRepository.kt:76): 번들 조회 시 해당 termKey의
   오버라이드가 있으면 그걸 반환, 없으면 번들. 로컬 캐시·프록시·API 경로 불변.
4. 우선순위: override > 번들. 프록시/D1발 용어는 이미 서버측 교정 가능 → 대상 아님.
5. 교정 게시 런북: 번들 용어 교정 → 오버라이드 스토어에 항목 추가/수정 →
   매니페스트 version 증가 → 클라이언트가 TTL 내 자동 반영.

비목표(오버엔지니어링 금지): 관리자 UI 없음(D1 직접 편집/간단 스크립트),
유저별 타깃팅·부분 필드 머지·실시간 푸시 없음(항목 전체 교체 + TTL 폴링).

착수 조건: v0.1.0 출시 후. 지금은 기록만.

- **[Data] 번들 DB 추가 확장** — 검색 빈도 데이터를 우선순위 입력으로(승격 잡의 hot 선정 입력, M8 플라이휠과 연동).
- **[Arch] AI 스트리밍 도입 검토** — 현재 단발 응답. 토큰 스트리밍(`Flow<String>`)은 이후 선택지(architecture §4.3).
- **[Arch] 프롬프트 서버 이전 검토** — 현재 클라이언트(`commonMain`) 소유. 프롬프트 핫픽스 필요성 커지면 재검토([ADR-0006](docs/adr/0006-server-cache-boundary.md) 유보 항목).
- **[UI] 디자인 후속** — 다크/라이트 폴리시·대비·플랫폼별 미세 조정.
- ~~**[Server] 프록시 토큰 usage 기록(M9 실기기 검증 파생, 2026-07-13)**~~ — ✅ **구현·배포·D1 활성화 완료(2026-07-14)**. `usage_log` 적재 코드·마이그레이션·`USAGE_DB` 바인딩이 프로덕션 라이브(devetym-proxy 커밋 `c7218db`, 배포 버전 `c5cd809f`, 무과금 스모크 405/400/413 통과). 잔여(선택): usage 실적재 눈확인은 실검색 1회(~$0.03) 필요 — 402 수집 트랙 착수 시 겸사 확인.
- ~~**[Arch] 크래시 리포팅 commonMain 단일 KMP 배선으로 통합 (WU-4 후속)**~~ — ✅ **완료(WU-4B, 2026-07-10)**. Approach A(Kotlin Cocoapods)는 `pod` CLI 부재로 스킵 → **Approach B(Sentry.xcframework 벤더링 + linkerOpts + Swift 백호환 라이브러리 경로)** 채택. commonMain 단일 `sentry-kotlin-multiplatform` 0.27.0(iOS Cocoa 8.58.2 정적 xcframework, 비커밋 gradle 다운로드) 배선 + **5축 green** + **Xcode 시뮬 빌드 SUCCEEDED**(iOS도 Sentry 실링크 — WU-11 SPM 절차 대체). seam 이원화 해소. 상세 = `git show 0d4c57e:docs/handoff/26-07-10-wu4-crash-reporting-ledger.md` §5(2026-08-25 정돈에서 삭제).
- **[Ops] Admin 키 회전 — 2026-10 초 (만료 90일, 리마인드 ~10/5)** — `admin-cost-logging-rot-2026-10` 만료 전 무중단 회전: 새 키 발급 → 루트 `.env` 교체(gitignore 등록됨) → `report.py` 확인 → 구 키 폐기. 정책·절차 정본 = [결정 문서 §5 체크리스트](docs/cost/cost-management-decision.md). 캘린더 리마인더는 사람이 별도 등록.
- **[Server] 서비스 소진(402) 시 놓친 검색어 수집 (2026-07-14 발의)** — 크레딧/월 한도 소진으로 생성 못 한 검색어를 프록시가 D1에 적재(키워드 + 시각 + device prefix). 의미: **수요가 실증된 미보유 용어 목록** → 나중에 Batch API(50% 할인) 일괄 생성으로 캐시/번들 선탑재, 승격 플라이휠(INV-12)의 hot 선정 입력과도 연동. 앱 변경 불요(프록시만). **의존 해소(2026-07-14)**: D1 활성화 완료 — 착수 가능. 확장하면 402뿐 아니라 실패 전반(타임아웃 등)의 키워드 수집도 같은 테이블로 가능 — 설계는 착수 세션에서. *새 세션에서 진행 예정(사람 확정).*
- **[A11y] 접근성 후속 2건 (VoiceOver 감사 파생, 2026-07-14)** — ① 탭 전환 시 VoiceOver 포커스를 새 화면 콘텐츠로 이동(focus management — 현재는 표준 동작 범위로 판정, 순방향 스와이프가 다음 탭으로 감) ② 외관 3모드 선택 상태 `selectable(selected=…)`·데이터 수집 Switch 행 `toggleable` 라벨 연결. v0.1.x 대상. 근거 = [감사 대본 §D #3·#4](docs/release/README.md).
- **[Feature] 하루 한 단어 알림 (Word of the Day, 2026-07-14)** — 매일 1회 단어를 로컬 알림으로 푸시. 서버 푸시 불요(번들 DB 650개에서 로컬 선정 — 랜덤 또는 승격 잡 hot 우선순위 연동 가능, 위 [Data] 항목과 시너지). 구현 좌표: 공통 선정 로직은 `commonMain`, 알림 스케줄링은 플랫폼 seam(expect/actual) — iOS `UNUserNotificationCenter` 반복 로컬 알림 + Android `AlarmManager`/`WorkManager`+`NotificationManager`. ⚠️ iOS 26 신규 **AlarmKit**(무음·집중모드 관통, 전체화면 알람)은 알람·타이머용이라 데일리 학습 알림엔 과함 — 표준 로컬 알림이 적합, 단 iOS 26+ 위젯/Live Activity 노출은 후속 검토. 알림 권한 요청 UX(온보딩 or 설정 토글)·방침 영향(수집 0 유지, 로컬 온리) 함께 설계.
- **[Feature] 앱 내 평점 요청 배선 (in-app review prompt, 2026-07-16 발의)** — 사용자가 만족했을 확률이 높은 순간에 시스템 리뷰 프롬프트를 능동 노출해 초반 평점을 확보한다(설정 화면의 수동 "앱 평가" 버튼과 별개 — 그건 사용자 주도, 이건 앱 주도). 트리거 설계(발의 시 확정 방향): ① 누적 어원 조회 N회 이상(5~7회, 앱 가치를 충분히 경험) ② 결과를 실제로 읽은 신호(상세 끝까지 스크롤 또는 수 초 체류) 충족 직후 ③ 검색 실패·에러·빈 결과 직후엔 절대 금지. OS가 노출을 연 3회로 제한하므로 아무 때나 호출하지 않고 아껴 쓴다. 구현 좌표: 조회 카운팅·트리거 판정은 `commonMain` 순수 로직 + 기존 스토어 seam 패턴으로 영속(외관·동의와 동일), 표출은 **별도 seam이 필요하다**. ⚠️ **좌표 갱신(2026-08-08, [#19](https://github.com/data-sy/devetym/issues/19))** — 종전 계획은 *"`iosReviewPresenter` 훅이 이미 배선돼 있어 호출 지점만 추가"* 였으나 **그 훅은 제거됐다**: 설정 버튼이 프롬프트 API를 떠나 App Store 딥링크가 되면서 `AppActions.requestReview()`는 이제 **딥링크 전용**이다. 따라서 이 기능은 ① `iOSApp.swift`의 StoreKit 2 주입(의존 역전)을 **되살리고** ② 딥링크와 구분되는 **새 seam**(예: `promptReview()`)으로 붙여야 한다 — 기존 seam 재사용은 설정 버튼까지 프롬프트로 되돌려 #19를 회귀시킨다. Android는 현 Play URL `ACTION_VIEW` 외에 Play In-App Review(`ReviewManager`) 도입 검토. v0.1.x 대상.
- ✅ **[Docs] 구 `~/devetym-shots` 경로 참조 — 해소 (2026-08-25)** — 참조하던 문서 7건 중 6건이 서류 정돈에서 삭제됐고, ROADMAP에 남은 1건도 현행 `shots/`로 정정했다.
- **[Feature] 어원 일러스트 — 용어당 그림 한 컷 (2026-07-18 발의)** — 어원 이야기를 텍스트 대신/병행으로 그림 한 컷으로 전달(예: canary-deployment = 갱도에 새를 풀며 "가서 확인해랏!" 외치는 광부). 히어로 용어 큐레이션 작업 중 "결과물을 그림으로 보면 더 쉽다"는 관찰에서 나옴 — 스토어 스크린샷·씨딩 글 비주얼과도 시너지. **다음 스텝에서 고민할 것**: 제작 방식(생성·외주·직접), 적용 범위(히어로 몇 개만 vs 전체 650), 과금 연계(일러스트는 유료 사용자 전용 등 수익화 실험 후보). 미확정 아이디어 단계 — 착수 전 범위·비용 설계 필요.
- ✅ **[Docs·규범] 서버 캐시 완료 사실 미반영 2건 — 해소 완료 (2026-07-28 발의 → 2026-08-19 사람 승인 후 반영, `eee5493`)** — ① 〔해소〕 `docs/architecture.md:250`이 **미래형**이었다(*"서버 신규 구축은 `devetym-proxy`"*) — S1은 2026-07-28 가동 완료. ② 〔해소〕 `docs/cache-delivery-milestones.md` §M0·§M1이 **구현 완료인데 완료 마커가 없어** "안 지은 것"으로 읽힌다 → 다음 세션 중복 착수 위험. ⚠️ **둘 다 규범 문서라 자동 수정 금지** — 제안문은 S1 잔여 정리 핸드오프(3건 전부 소진 — 2026-08-25 정돈에서 삭제) §A에 그대로 있다. ⚠️ **INV-1~13 자체는 건드리지 말 것**(특히 INV-8 temperature는 S1에서 의도적 미달성 — "달성"으로 바꾸면 안 된다).
- ✅ **[Ops] 씨딩 3건 실행 여부 — 해소 (2026-08-19 사람 답변)**: **실행하지 않았고, 의도적으로 연기했다.** 씨딩은 **웹 트랙 W 완성 후 웹과 함께** 나간다 — App Store 착지는 다운로드 마찰 때문에 커뮤니티 참여도가 낮다는 판단. 복붙본 2건은 보존·재사용. 상세 = M9 「📆 게시·씨딩 일정」 항목.
- ✅ **[Ops] 서류 정돈 — 완료 (2026-08-25)** — 문서 86 → 46건, `docs/handoff/` 폴더 소멸, ROADMAP 142KB → 83KB(닫힌 트랙 서사를 Done 한 줄씩으로 압축). 댕글링 링크 0 검증 완료. `~/Downloads/devetym-release`는 30 → 6건(씨딩 복붙본 2·그로스 플랜·스토어 메타·핫픽스 런북·README). 삭제분은 스냅샷 tar.gz에 보존.
- **[Ops] v0.1.1 프로모션 텍스트 입력 여부 확인 (2026-08-16)** — 새 버전 레코드에서 프로모션 텍스트가 **비어 있는 것을 사람이 발견**했다. v0.1.0 때 미입력이었는지 새 버전으로 승계가 안 된 것인지 **원인 미확인**. D5 확정본(공감형 87자)은 `~/Downloads/devetym-release/store-metadata.md` §2에 있다. **프로모션 텍스트는 심사 없이 언제든 교체 가능**하므로 게시 후에도 채울 수 있다 — 다음 릴리스 때 "승계되는 필드/안 되는 필드"를 한 번 정리해 둘 것.
- **[Build] `pbxproj` ↔ `project.yml` 드리프트 정리 (2026-08-16 발의)** — Xcode 26이 v0.1.1 아카이브 중 `Sentry.xcframework` 참조에 `expectedSignature = "AppleDeveloperProgram:97JCY7859U:GetSentry LLC"`(프레임워크 서명 검증 핀)를 **자동 추가**했고, 사람 결정으로 그대로 커밋했다. 이 저장소는 `project.yml` → xcodegen → `pbxproj`가 정본이라 **다음 `xcodegen generate` 때 이 줄이 조용히 사라진다.** 해소 방향 둘: ① 설치된 xcodegen이 `expectedSignature`를 지원하면 `project.yml`에 명시 ② 미지원이면 재생성 후 이 속성을 다시 붙이는 절차를 문서화(또는 Xcode가 재추가하도록 두고 무해한 노이즈로 수용). ⚠️ **재생성 전에 이 항목을 먼저 볼 것** — 모르고 돌리면 서명 핀이 빠진 채 커밋된다.
- **[Android] `AndroidSeams.openUrl` `ActivityNotFoundException` 미처리 — 크래시 경로 (2026-08-08 발의, [#19](https://github.com/data-sy/devetym/issues/19) 곁가지)** — 같은 파일 `sendMail`은 잡는데 `openUrl`(`AndroidSeams.kt:50`)은 안 잡아, 브라우저·스토어 부재 기기에서 **앱이 죽는다**. `requestReview`(Play URL)·`privacyPolicyUrl`이 전부 이 함수를 탄다. **이번 수정(#19)에 끼워 넣지 않았다** — Android는 미출시(F 트랙 2026-08-05 보류)라 실사용자 0명이고, `requestReview`가 여는 Play URL은 **지금 죽은 링크**(미게시)라 급하지 않으며, iOS 수정에 섞으면 스코프가 오염된다. **F 트랙 재개 때 묶어 처리**하는 것이 기본 방향(수정 자체는 `sendMail`과 동형 — `try`/`catch` + Toast 안내 한 겹).
- **[Data] 번들 스냅샷 재생성 — 콘텐츠 정본 D1 이전 완료 후 (2026-08-25 사람 발의)** — 위 「콘텐츠 정본 D1 승격」이 끝나면 `terms.json`은 **손으로 고치는 정본이 아니라 D1에서 뽑아낸 생성물(스냅샷)** 이 된다. 그 시점에 ① D1 → `shared/src/commonMain/composeResources/files/terms.json` 익스포트 잡을 돌려 번들을 최신화하고 ② 파일 상단에 「generated — 직접 편집 금지」 마커를 박고 ③ 다음 앱 릴리스에 실어 보낸다. **이것이 INV-12 승격 플라이휠의 실제 구현체**가 된다(별도 머지 잡이 아니라 스냅샷 익스포트 1회). ⚠️ 앱 오프라인 보장(INV-11 후단)은 이 스냅샷이 계속 번들에 실려야 성립한다 — 「D1이 정본이니 번들은 빼자」로 미끄러지지 말 것. ⚠️ 웹 SSG 빌드도 같은 익스포트를 입력으로 쓰면 D1 장애 시에도 빌드가 재현 가능하다.
- **[Ops] Apple Developer Program 갱신일 확인 — 미확인 (2026-08-25)** — **놓치면 앱이 App Store에서 내려간다.** 현재 유일한 실사용자 채널이 iOS 단독이라 이게 곧 서비스 정지다. 결제 자체는 완료돼 있으나(WU-12a) **갱신일이 어디에도 기록돼 있지 않다**. 확인처 = [Apple Developer > Membership](https://developer.apple.com/account) → 확인 후 [`docs/cost/running-costs.md`](docs/cost/running-costs.md) §1에 채우고 캘린더 리마인더 등록(Admin 키 회전과 같은 방식). 곁가지: Sentry 플랜 확인 · `devetym.com` 자동 갱신 토글 확인(둘 다 같은 문서 §5).
- ~~**[Ops] 도메인 등록기관 Cloudflare 이전**~~ — ✅ **안 하기로 결정 (2026-08-25 사람).** AWS 계정을 **도메인을 모아 두는 계정**으로 쓰므로 거기 두는 편이 관리가 낫다. **추가로 나가는 돈이 없다** — Route 53 호스팅 영역을 지워 $0.50/월은 이미 회피했고, 남는 것은 등록비 $16/yr뿐이며 그건 어디서 사도 낸다(Cloudflare가 연 $5쯤 싸지만 계정 분산 비용이 그보다 크다는 판단). 기술적으로도 이전이 **불필요하다** — Worker 커스텀 도메인이 요구하는 것은 *네임서버 위임*이고 그건 이미 끝났다. 자동 갱신 ✅. 원장 = [`docs/cost/running-costs.md`](docs/cost/running-costs.md) §6.
- **[Web] `www` → apex 301 Redirect Rule (2026-08-25 발의 · W0b 파생)** — 지금 `www.devetym.com`이 **301이 아니라 200으로 같은 내용을 서빙**한다. 검색 유입이 목적인 트랙에서 중복 콘텐츠는 스스로 신호를 깎는다. ⚠️ **Astro 미들웨어로는 못 고친다(실측)** — 어댑터가 생성하는 `_routes.json`이 prerender 경로를 Worker 호출에서 제외해 미들웨어가 아예 안 탄다(`assets.run_worker_first`로도 안 뒤집힘). → **Cloudflare Redirect Rule**(존 레벨·Worker 미경유·무료 플랜 10개)로 건다: `Rules > Redirect Rules > Create` · 조건 `hostname eq "www.devetym.com"` · 동작 `Dynamic → concat("https://devetym.com", http.request.uri.path)` · **301**. 대시보드 작업이라 사람 몫. **현재 완화**: www가 서빙하는 HTML의 canonical이 apex를 가리켜 대개는 정리되나, 301이 신호가 훨씬 강하고 링크 자산도 합쳐진다.
- **[Web] 웹 방문자 피드백 수집 (2026-08-25 사람 발의)** — *"웹으로 열면 어떠셨나요"* 를 묻고 싶다. **모달은 채택하지 않는다** — 검색 유입 방문자는 착지 페이지 하나 보고 15초 안에 이탈하는 비중이 높아, 뜨는 창은 응답률은 낮고 이탈은 올린다. 게다가 3건 한도 화면은 이미 **앱 유도 전환점**으로 설계돼 있어(ADR-0010·Q4) 거기에 피드백을 겹치면 유일하게 측정 가능한 전환 경로를 갉아먹는다. **채택 방향**: ① 용어 상세 **본문 끝**에 인라인 한 줄 — `이 설명이 도움이 됐나요? 👍 👎`, 클릭 즉시 기록하고 그 자리에서 *"고마워요"* 로 바뀐다(페이지 이동 없음) ② 👎를 누른 경우에만 **한 줄 자유 입력**을 펼친다(선택) ③ **AI 생성이 성공한 직후**에도 같은 한 줄을 붙인다 — 사용자가 방금 값을 받은 순간이라 응답률이 가장 높다. **공짜가 아니다**: 저장소(D1 테이블) + **Turnstile**(자유 입력은 스팸 표면) + 방침에 한 줄 + 「개인정보 입력 금지」 안내가 따라온다 → **W1a 하드닝과 같은 슬라이스로 묶는 것이 맞다**(별도로 하면 방어 없는 쓰기 경로가 하나 더 열린다). ⚠️ 👎 신호는 **[ADR-0013](docs/adr/0013-web-route-contract.md)의 색인 게이트 입력으로도 쓸 수 있다** — 사람이 틀렸다고 표시한 생성분을 승격에서 빼는 값싼 신호. 목적지 = D1(버그성 제보는 GitHub Issues로 승격, [ADR-0008](docs/adr/0008-issue-tracking.md)).
- **[Feature] 탐색 유도 — 첫 진입 인기 용어 · 검색 후 연관 용어 (2026-09-02 사람 발의)** — 지금 앱 첫 화면은 **빈 검색창**이라 "뭘 쳐야 하지"에서 이탈한다. 둘을 붙인다: ① **첫 진입에 많이 찾는 용어 몇 개**를 보여 주고 ② **한 용어를 본 직후 연관 용어**를 눌러 볼 수 있게 한다. 값: 세션당 조회 수가 늘고(= 캐시 히트·앱 체류), 「하루 한 단어」·평점 프롬프트 트리거(누적 조회 N회)와 곧장 시너지가 난다.
  - **인기 순위의 원천이 이미 있다** — D1 `entries.hit_count`가 요청 횟수를 세고 있고(시딩이 이 값을 보존한다), 프록시 `usage_log`도 있다. 즉 **새 계측을 짓지 않고 서버가 가진 수로 시작할 수 있다.** ⚠️ 다만 `hit_count`는 **캐시 미스로 서버까지 온 요청만** 센다 — 번들에서 로컬로 끝난 조회는 안 잡힌다. 그래서 이 수는 「인기」가 아니라 「서버에 도달한 수요」다. 이 편향을 모르고 쓰면 이미 번들에 있는 인기 용어가 순위에서 빠진다.
  - **연관 용어는 지금 데이터로 계산할 수 있다** — `category`(6종) + 별칭·본문의 어휘 겹침. 새 필드나 임베딩 없이 시작하고, 부족하면 그때 키운다. **웹 쪽 「관련 용어」는 W2 범위와 겹치므로**(카테고리 허브·관련 용어) 같은 산출을 앱·웹이 나눠 쓰는 형태로 설계할 것 — 두 번 짓지 않는다.
  - **미확정**: 순위를 서버가 주나(엔드포인트 신설·D1 읽기) 번들에 구워 보내나(오프라인 유지·신선도 손해). 후자는 INV-11 오프라인 보장과 정합하고 전자는 실시간성이 있다. 착수 세션에서 가른다.
- **[UX·Content] 「650개」라는 고정 수를 사이트·앱 정체성에서 뺀다 (2026-09-02 사람 발의)** — 승격 잡(W1c)이 돌기 시작하면 **개수는 매 라운드 늘어난다.** 그런데 지금은 그 수가 여러 곳에 정체성처럼 박혀 있다: 사이트 설명(`web/src/config/site.ts`)·한도 화면 문구(`web/src/lib/messages.ts`)·엔트리 가드(`web/src/lib/terms.ts`)·시딩 오라클(`test_seed_d1.py` 확정값)·스토어 메타·씨딩 복붙본. W1c 승격 1회에 이 여섯 곳을 손으로 맞췄다 — **그 노동이 매 라운드 반복되고, 한 곳이라도 빠지면 사이트가 스스로 틀린 수를 말한다.**
  - **관점 전환**: 「650개 사전」이 아니라 「계속 자라는 사전」으로 말한다. 사용자 문구에서 수를 빼거나(*"개발 용어의 어원을 찾아봅니다"*), 굳이 쓸 거면 **빌드 시점에 번들에서 파생**해 손으로 안 맞춰도 되게 한다.
  - ⚠️ **깨지는 검사는 남겨야 한다.** `terms.ts`의 엔트리 수 가드와 시딩 확정값은 "빌드는 성공하는데 데이터가 조용히 증발"을 잡는 장치다(INV-A). 여기서 수를 빼면 그 오라클이 사라진다 — **사용자에게 보이는 문구의 수**와 **오라클로서의 수**를 갈라서 판정할 것. 전자는 파생, 후자는 유지가 기본 방향.
  - **2026-09-03 갱신(W2)**: 박힌 곳이 하나 늘어 **일곱**이 됐다 — `web/scripts/test-structure.mjs`가 용어 660·허브 6을 단언한다. 다만 이건 위 분류상 **「오라클로서의 수」라 유지가 기본**이다(페이지가 통째로 사라져도 나머지 검사는 전부 통과하므로 규모 단언이 없으면 못 잡는다). 착수할 때 **사용자에게 보이는 수**(site.ts·messages.ts·스토어 메타·씨딩 복붙본)만 파생으로 바꾸고 오라클 3곳(terms.ts 가드·시딩 확정값·구조 검사)은 남기는 것으로 가른다.
  - 착수 시 함께 볼 것: 스토어 메타(`~/Downloads/devetym-release/store-metadata.md`)·씨딩 복붙본 2건에도 같은 수가 박혀 있다.
- (아이디어 추가 시 여기로)

---

## Done — 완료

- **W1b · 웹 본체 — ✅ 2026-09-02 프로덕션.** 착지 페이지 한 장이던 `devetym.com`이 **650장짜리 사이트**가 됐다. 워커 `devetym-web` `eeba288a`.
  - **완료 오라클 통과**: 배포된 실 URL **656개 전수 200**(용어 650 + `/`·`/terms`·`/search`·`/search-index.json`·`robots.txt`·`sitemap-index.xml`) + **웹 표면 AI 생성 왕복 성공**(`dark launch` — Turnstile → same-site `/api/term` → service binding → 프록시 → 생성 → D1 적재 → 자기 URL 200). 사이트맵 652 URL.
  - **3층 한도가 실재함이 실측으로 확인됐다**: 생성 1건에 `rl:web:c:*`(쿠키)·`rl:web:ip:*`·`rl:web:global`이 **전부** 증가했고, IP 버킷이 **실 클라이언트 IP의 해시와 정확히 일치**했다 — service binding이 `CF-Connecting-IP`를 보존한다는 뜻이다. 보존되지 않았다면 IP 15건/일 층이 전 웹 사용자 공용 버킷 하나로 붕괴했을 것이다(15건 뒤 전원 429). same-site 쿠키도 왕복한다(2회차 재발급 0).
  - **한글 별칭 1급 승격 완료** — title·h1·`DefinedTerm.alternateName`. `뮤텍스(mutex) — 왜 이렇게 부르는가`.
  - **앱과 의도적으로 갈라진 곳 2개를 테스트가 고정한다**: 자동완성이 별칭을 본다(앱 명세면 한글 입력에 0건) · 정규화 충돌 3건(집계·분기·샤딩)을 둘 다 보여준다(앱은 first-wins로 가림).
  - **실측으로 잡은 함정 4건은 핸드오프 §5에 있다** — `build.format:"file"`이 `/term/index`를 307로 만든 것 · 404 라우트가 status 200을 못 뒤집는 것 · `innerHTML` 노드에 스코프 CSS가 안 붙는 것 · `[assets]` 섹션이 뒤따르는 최상위 키를 삼킨 것.
  - **정규화 네 번째 구현(웹)** 이 생겼고 그 대가를 교차 실행으로 갚았다 — `test_term_key.py`가 웹 이식본까지 실행해 3,398건 바이트 비교(불일치 0).

- **🌐 웹 트랙 W1a · 프록시 하드닝** — ✅ **2026-09-02 프로덕션 적용·실측 종결**. 워커 `078e1a3a`(롤백 대상 `d0504c73`) · `devetym-proxy` `feat/w1a-proxy-hardening` → `main` 병합. 결정 = [ADR-0010](docs/adr/0010-web-abuse-prevention.md)(표면 분리·3층 한도·CORS) · [ADR-0011](docs/adr/0011-prompt-ownership-transfer.md)(프롬프트 소유권 이전).
  - **닫은 것**: CORS 고정 allowlist(밖은 **서버가 403** — 헤더만으론 방어가 아니다) · 표면 분리 캡(웹 쿠키 3/IP 15/전역 30 · **앱 10/200 불변**) · HMAC 서명 HttpOnly 쿠키 식별 · Turnstile(웹 생성 요청에만·fail-closed) · 워크스페이스 키 분기(`DevEtym-Web` · spend limit $20) · **프롬프트 정본 앱→워커 이전** · usage_log `surface`·`outcome`.
  - **라이브 실측**: 제3자 Origin 403 · preflight 204 · 양 표면 캐시 히트 200 · 토큰 없는 웹 생성 403(**과금 0**) · usage에 `app/ok`·`web/cache_hit`·`web/turnstile_failed` 착지.
  - **프롬프트 이전 무손실 증거**: 실기기 생성 `bikeshedding` → 저장 행의 `prompt_version = v2-pathA:956ba44a7c48` = **이전 전 generated 행과 같은 태그**. 한 글자만 달랐어도 신·구가 두 태그로 갈려 INV-9 선택적 무효화가 불가능해졌을 자리다.
  - **관측 구멍 하나를 닫았다**: 종전엔 **성공 호출만** 적재해 설계서 F5(앱 요청의 429/402 발생률)의 **분자가 없었다**. 이제 차단 요청도 행으로 남는다.
  - 테스트 83 → **131**. 백업 `~/devetym-usage-backup-20260902.sql`.

- **🌐 웹 트랙 W0c · 650 D1 시딩** — ✅ **2026-09-01 프로덕션 적용**. 결정 = [ADR-0012](docs/adr/0012-content-canon-d1.md)(D1 = 콘텐츠 정본, 앱 번들 = 스냅샷). 브랜치 `sandbox/w0c-d1-seeding`(양쪽 repo 원격 보존).
  - **7/7**: `normalizeTermKey` N1 확정(구분자 삭제) · `entries.origin` · authored 센티널(번들 내용 해시 `authored:efa8f264dc67`) · 650 시딩 · **authored > generated 충돌 규칙**(+ INV-5 첫 write) · 익스포트 왕복 **바이트 동일** · 원격 적용.
  - **프로덕션 실측**: entries **671** · authored **650** · generated **21** · aliases **1,304** · 키 1,975개 **전부 N1**. 실 앱 경로에서 `aatree`·`AA 트리`·`추상 팩토리`가 전부 정본 행에 도달한다.
  - **🔑 순서 교훈**: 정규화 정의를 바꾸면 **워커 배포가 데이터 시딩보다 먼저다.** 반대로 하면 keyword 286개·별칭 784개가 현 워커가 조회하지 않는 키로 앉아 사용자가 찾을 때마다 과금 + 중복 행이 쌓인다. 백업 `~/devetym-d1-backup-20260901-020554.sql`.

- **🌐 웹 트랙 W0a·W0b · 기반과 도메인 결선** — ✅ **2026-08-25**. <https://devetym.com> 라이브(200 · `noindex` 해제 · 사이트맵 200). Astro 5.18 + `@astrojs/cloudflare` 12.6 · **`SITE_URL` 단일 지점**(도메인 교체가 환경변수 한 줄, 코드 수정 0) · 토큰 자동 추출 · 폰트 woff2 5종 291KB→121KB · **클라이언트 JS 0바이트**. 빌드 750ms · Worker 기동 24ms. 결정 = [ADR-0009](docs/adr/0009-web-framework-rendering.md)·[ADR-0013](docs/adr/0013-web-route-contract.md).
  - **도메인**: Amazon Registrar 등록($16/yr·자동 갱신·만료 2027-08-25), **네임서버만 Cloudflare 위임**(소유·결제는 Amazon 유지). Route 53 호스팅 영역 삭제로 $0.50/월 회피.
  - **실측으로 잡은 함정 2건**: ① 실 도메인 결선 뒤에도 `workers.dev`가 살아 있으면 같은 내용을 색인 허용 상태로 서빙해 **중복 콘텐츠로 경쟁**한다 → `workers_dev = false`로 껐다. ② `robots.txt`가 선언한 사이트맵이 **실제로 404**였다 → `@astrojs/sitemap`으로 해소. **⚠️ `www` 301은 Astro 미들웨어로 못 고친다**(어댑터 `_routes.json`이 prerender 경로를 Worker에서 제외) → Cloudflare Redirect Rule 필요(Now 「사람이 해야 하는 것」 ④).

- **📂 서류 정돈 트랙** — ✅ 2026-08-25. 위 Now 항목 참조(스냅샷 경로 포함).
- **#19 · 설정 「앱 평가하기」 무반응 수정** — ✅ 완전 종결 2026-08-19 ([#19](https://github.com/data-sy/devetym/issues/19), 2026-08-08 외부 유저 제보). 성격 = 버그 재현이 아니라 **API 오용 수정**(iOS `AppStore.requestReview` 대신 App Store 리뷰 딥링크). 브랜치 `fix/settings-review-deeplink`. **오라클은 실기기 단독** — 시뮬엔 App Store 앱이 없어 Safari가 열리는 것까지만 증명한다. 실기기 PASS(iPhone 13 mini·iOS 26.5.2) → **v0.1.1 App Store 게시(2026-08-18)** → 게시본 재확인 → 제보자 회신까지 완료.
- **M9 · 출시 준비·실기기 검증 — iOS 종결** — ✅ **2026-07-27 App Store 게시·라이브** ([개발 어원 사전](https://apps.apple.com/kr/app/id6790429958) · Apple ID `6790429958` · 무료·한국 단독 · 현재 v0.1.1). 검증 구간은 PR #9로 main 병합(2026-07-13), 제출 준비·수정분은 PR #11·#12, Sentry 실 DSN 배선은 PR #14. 실기기(아이폰 13 mini) 스모크·UX 사인오프·VoiceOver 사인오프·접근성 감사 종결, iOS 시뮬 입력 주입(CGEvent 자작 도구) 완주, 스토어 스크린샷 5컷×2사이즈 확정. **Android(F)만 잔여 → Now의 F 항목.**
- **캐시 트랙 S1 · `devetym-proxy` read-through 캐시** — ✅ **2026-07-28 프로덕션 가동 중**(2단 배포 `b87b77f1`, `CACHE_DISABLED="0"`). 스펙 = [`docs/specs/server-m0-m1-cache-read-through-draft.md`](docs/specs/server-m0-m1-cache-read-through-draft.md) · 계약 = [ADR-0006](docs/adr/0006-server-cache-boundary.md). 한 사용자가 생성시킨 항목을 다른 사용자가 재사용해 재과금을 없앤다(비용이 *사용자 수*가 아니라 *새 용어 수*에 비례). PR [#17](https://github.com/data-sy/devetym/pull/17)·[#18](https://github.com/data-sy/devetym/pull/18)·[devetym-proxy#3](https://github.com/data-sy/devetym-proxy/pull/3).
  - **라이브 실측(과금 1회 $0.0230)**: 요청 `멱등성` → `term_key="idempotency"`(AI 정본 키워드) 저장 · `prompt_version = v2-pathA:956ba44a7c48`가 앱 `ClaudePrompt.kt` sha256과 일치 · 영문/공백패딩/타 기기 전부 히트(0.08~0.2s, Anthropic 0회) · 한도 소진 상태에서 **히트 200 / 미스 429**.
  - **롤백 = `CACHE_DISABLED="1"` 한 줄 + 재배포.** 직전 프로덕션 `c5cd809f`(2026-07-14)가 기준점.
  - **관측 지점**: `usage_log`의 `cache_hit` 비율이 이 슬라이스의 성과 지표. 부정 분기 재생성 빈도를 보고 `NEGATIVE_TTL_DAYS`(30) 조정 판단.
  - ⚠️ **운영 교훈**: `wrangler` 4.114·`miniflare` 4가 **Node ≥22 강제**(`.nvmrc` 추가됨) · **KV 조작 후엔 반드시 `kv key get`으로 되읽어 확인**(`--expiration-ttl` 미지원 플래그가 조용히 실패해 전역 카운터가 소진 상태로 남을 뻔했다).
- **이관·자기완결화 트랙 (WU-1~6·8~10)** — ✅ 2026-07-10~13. `~/dev-etymology` → `devetym` 자기완결화: Pages 배포·방침 URL 라이브(PR #10) · Scripts·db-expand 이관(`315ea55`) · ai-quality → [ADR-0007](docs/adr/0007-ai-prompt-quality.md) 신설 · 크래시 리포팅 Sentry(초기 seam 분리 → **WU-4B 단일 KMP 통합**으로 iOS까지 실배선; Kotlin Cocoapods는 `pod` CLI 부재로 스킵, `Sentry.xcframework` 벤더링 채택) · 네이티브 iOS 전수 스윕으로 **의존 0 확증**. **잔여 = WU-7(폐기·사람) → Now 항목.**
- **코드 갭 수정 트랙 (WU-8·9·10)** — ✅ 2026-07-10. devetym 내부 결함 3건: 클립보드 복사 UI 배선(dead seam 활성화) · Android 스플래시 배선 · 셸 배선 회귀 가드. 전건 green·커밋.
- **M0→M8 마일스톤 아크 (코드 레벨)** — ✅ 2026-07-05. 상세는 아래 마일스톤별 항목.

- **비용 관리·모니터링 체계 (운영 트랙)** — ✅ 2026-07-14 (devetym PR #13 + devetym-proxy PR #1 머지·**프로덕션 배포**, 브랜치 `feat/cost-management`·`feat/cost-hardening` 보존). 컨설팅([결정 문서](docs/cost/cost-management-decision.md)) → 구현: **가시성**(워크스페이스 분리 + `Scripts/cost/report.py` Admin API 리포트 + 프록시 usage D1 기록 코드) · **상한**(Console 월 $30 + 알림 10/20/25 + 크레딧 자동리로드 $5→$15 + 프록시 과금 파라미터 서버 강제·본문 32KB 캡·전역 200회/일 캡 + probe 실행 전 비용 게이트) · **오류 계약**(402=서비스측 소진 → `ServiceExhausted` → "AI 생성에 문제가 있어요" — ADR-0006 Decision 7). 검증: JVM+네이티브 green, 라이브 프록시 무과금 스모크 4경로. Console 설정 스냅샷 = [설정 로그](docs/cost/console-settings-log.md). **잔여 2건 해소(2026-07-14)**: D1 활성화 완료(배포 `c5cd809f`) + Admin 키 발급·루트 `.env` 보관·`report.py` 첫 리포트 성공($0.20, 캐시 적중 54%).
- **M9-후속 · 실기기 피드백 UX 3건** — ✅ 2026-07-13 (브랜치 `feat/m9-release-verification`, 커밋 `3f1ce6a`·`35874bf`·`720f5d4` — 각각 독립 커밋·미푸시). M9 아이폰 13 mini 실기기 테스트 피드백 3건 전부 구현, 각 건 **5축 green + iPhone 16 Pro 시뮬 실주행 스크린샷 대조**로 닫음.
  - **[UX-1] 상세 액션 톤 알약 버튼(목업 A안)** — `ActionText` → `TonalPillButton` 아톰(Capsule accent 15% 틴트 + 글리프·라벨, 신규 의존성 0). 복사(WU-8 seam 유지)·북마크·공유 3개 + 오류 제보 회색 톤 분리. **전경 다크=accent(≈11:1)·라이트=brand(≈6:1)** — 라이트 accent가 틴트 위 AA 미달(≈4.1)이라 `tonalActionColor`로 분기, `AccessibilityContrastTest` 합성 쌍 게이트가 근거를 락. 시뮬: 다크/라이트 렌더·복사→`pbpaste` 회수·북마크 ☆↔★ 반응형.
  - **[UX-2] 스와이프 네비게이션** — 뎁스0 탭 4개 `HorizontalPager`(탭 상태 정본=pagerState, 탭바 클릭=animateScrollToPage, 재탭 pop 유지) + 뎁스1 엣지 스와이프-백(24dp 엣지·80dp 임계, `isEdgeSwipeBack` 순수 판정+테스트 5건). 상세 중 `userScrollEnabled=false`(제스처 충돌 관리). 〔구현 로어: `detectHorizontalDragGestures`의 onDragStart는 **터치 슬롭 통과 지점**이라 엣지 판정이 밀림 — 시뮬 실주행으로 적발, `awaitFirstDown` 실제 다운 지점 기준으로 교체〕 시뮬: 4탭 양방향 스와이프·클릭 점프·상세 중 본문 스와이프 무효·엣지 백·기존 탈출구 회귀 0.
  - **[UX-3] 로딩 문구 2개 크로스페이드** — "AI가 어원을 찾고 있어요" ↔ "잠시만 기다려 주세요" ~3초 교차(600ms tween, `loadingPhrase` 순수 헬퍼+순환 테스트). 시뮬: AI 미스 로딩에서 t<3s 1문구·t≈4.6s 2문구 실측.
- **M8 · 통합·자산·마감 (최종 *구현* 마일스톤 — 이후 M9는 검증·출시)** — 2026-07-05 (브랜치 `feat/m8-integration-assets`, 로컬 커밋 `ed26f51`·미푸시). M7 스텁을 **seam actual**로 대체: androidMain(`AndroidAppActions`·`PrefsAppearanceStore`·`PrefsOnboardingStore`·`PrefsDeviceIdProvider`·`AndroidDeviceInfo`)·iosMain(`IosAppActions`·`UserDefaults*` 3종·`IosDeviceInfo`), 플랫폼 모듈 5종 바인딩 교체. **외관 배선**(`AppRoot`가 `appearance.mode`→`AppTheme(dark)` 소비, `darkMode=true` inert 제거)·**온보딩 영속**(`OnboardingStore` seam)·**in-app OFL 라이선스**(`LicensesScreen`+`Res.readBytes`, `showLicenses` 오버레이)·**Android 런처 아이콘**(`v2/icon.svg`→rsvg 15 PNG+adaptive+colors+manifest, 커밋 PNG). **green 4축**: `:shared:testDebugUnitTest`(97, KoinGraph 온보딩 포함) · `:androidApp:assembleDebug`(**APK ic_launcher 17엔트리 패키징 실증**) · `:shared:linkDebugFrameworkIosSimulatorArm64`(iOS seams UIKit/Foundation 링크) · **`:shared:iosSimulatorArm64Test`(83, 회귀 0)**. 신규 좌표 0(플랫폼 API만). 참조: [M8 슬라이스](docs/specs/m8-integration-assets-draft.md).
  - **비준 RATIFIED(4R 수렴)**. §7 판정: iOS share=최소 스텁·평가=스토어 url·아이콘=커밋 PNG·Firebase=null 유지·VM수명주기=범위 밖·라이선스=in-app. **정정 반영**: iOS `NSUserDefaults` objectForKey null 체크로 외관 부재시 다크(2) 보장(integerForKey 0 반환 함정)·`UIPasteboard.string` 세터·전 actual 5종 실 모듈 바인딩(그래프 마스킹 방어)·DR-2 carry-forward(라이선스 네비 슬롯) `showLicenses` 오버레이로 마감.
  - **⚠️ 검증 천장(최대)**: seam 런타임 동작·아이콘 시각 충실도·iOS appiconset(Xcode)·접근성·Firebase·실 플랫폼 Koin 그래프 완전성·실기기 시각/상호작용·코드서명·심사는 4축이 보증 안 함 → 「코드 완료·실기기 검증 필요」(Now 아침 체크리스트).
  - 🔗 캐시: seam actual이 로컬-first 상태를 플랫폼 저장소(SharedPreferences/NSUserDefaults)에 영속. 서버·프록시는 별도 트랙. 〔캐시 트랙 M8 행위〕
- **M7 · Koin 배선 + 앱 셸 통합** — 2026-07-05 (브랜치 `feat/m7-koin-wiring`, 로컬 커밋·미푸시). 전 계층을 Koin 그래프로 조립: `di/AppModule`(`appModule(readyBundle)` 팩토리·`suspend initKoin(platformModule)`·`TermRepository`=`single`)·플랫폼 팩토리(`androidPlatformModule(context)`/`iosPlatformModule()`)·`KoinAppDependencies`·`DeviceIdProvider`·`epochMillis` expect/actual·`appWriteScope`. **앱 셸이 처음으로 `AppRoot`를 그린다**(M0 `App()` 삭제): `MainActivity`·`MainViewController`·`DevEtymApp`(`runBlocking { initKoin(...) }`). **green 4축**: `:shared:testDebugUnitTest`(97, KoinGraph 2+M7Concurrency 3) · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64` · **`:shared:iosSimulatorArm64Test`(83, M7Concurrency 3 네이티브)**. iosMain `AppModule.kt` facade 병합(`AppModuleKt.doInitKoin()` Swift 무편집)·`iosPlatformModule`·`epochMillis` ios actual 링크 실증. 참조: [M7 슬라이스](docs/specs/m7-koin-wiring-draft.md).
  - **⚠️ 검증 천장(최대)**: green = 컴파일·조립·링크 + 그래프 해석(공통+테스트-플랫폼 스텁, eager touch) + DR-2/DR5-2 순수 실행. **실 androidMain/iosMain 플랫폼 Koin 바인딩 완전성·Xcode 빌드·seam actual·런타임 시각/상호작용은 보증 안 함 → 실기기 이월**. 비준 ESCALATE(6R) 잔여 Blocker DR-1(실 플랫폼 그래프 완전성 4축 결착 불가)=천장 정직 수용.
  - **M4/M5 이월 처리**: **DR-2 단일-writer**=`TermRepositoryImpl` 정규화 키 Mutex(맵-가드 coroutines Mutex·비재진입 데드락 부재) + `single` 배선 **구조 담보**(진짜 병렬 강제는 실기기 이월·자칭 안 함). **DR5-2**=`DetailViewModel` 선택적 `writeScope` **취소 내성 하드닝**('닫음' 철회 — 실 셸 plain `remember` VM leak·DR5-2 실 창은 M8 ViewModelStore 이월). seam·deviceId·온보딩=스텁/in-memory(actual M8).
  - 🔗 캐시 배선 완결: 3계층 read-through·pinning·단일-writer 오케스트레이터가 Koin single로 전 화면 공유. 서버·프록시는 별도 트랙. 〔캐시 트랙 M7 행위〕
- **M6 · Compose UI (디자인시스템+6화면+네비)** — 2026-07-05 (브랜치 `feat/m6-compose-ui`, 로컬 커밋·미푸시). `commonMain/ui/`에 **디자인 시스템**(`theme/`: AppColors 11토큰 라이트/다크·AppFonts 하이브리드[한글=시스템·영문=DM Mono·헤더=DM Serif]·AppTypography 21종·AppDimens·AppTheme+AppScheme, 다크 기본)·**재사용 원자**(`components/`: CategoryBadge·AiBadge·FlowChip·PulsingDots·EmptyState)·**6화면**(`screens/`: Search·Detail·Bookmark·History·Settings·Onboarding, `XxxScreen`(VM 구독 래퍼)+`XxxContent`(순수) 2겹)·**네비**(`AppRoot`: 의존성-0 상태기반 back stack, 4탭+상세 push+온보딩 게이트)·**플랫폼 seam**(`platform/`: AppActions·AppearanceStore·DeviceInfo + no-op 스텁). 폰트 7종 `composeResources/font/`. **green 4축**: `:shared:testDebugUnitTest`(92, DesignSystem 7) · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64` · **`:shared:iosSimulatorArm64Test`(80, DesignSystem 7 네이티브)**. `lifecycle-runtime-compose 2.9.6`+폰트 iosSimulatorArm64 링크 실증. 참조: [M6 슬라이스](docs/specs/m6-compose-ui-draft.md).
  - **⚠️ 검증 천장(정직 경계)**: green = **컴파일·조립·링크 + 순수 헬퍼 네이티브 실행**까지. **화면 시각 충실도(간격·폰트 렌더·픽셀·다크 실제색·탭/스와이프/애니메이션)는 보증 안 함 → 「코드 완료·실기기 시각 검증 필요」**. 거짓 green 아님(구조·상태분기·배선·컴파일만 보증). 순수 헬퍼(색/타이포/에러메시지/상대시간/상태표시매핑/카테고리색/isBookmarkedFor) 7종만 네이티브 실측.
  - **비준 RATIFIED(6R 수렴)**. §7 판정: 네비=의존성-0(navigation-compose 링크 리스크 회피), 타이포 21종(정본 Theme.swift), 색상 hex=colorset 정본, RelativeTime=경과 diff 기반(N시간 전 포함). **M5 이월**: DR-4(상세 북마크 상태) `isBookmarkedFor`(`normalizeKeyword` 교차조회 — 저장 키 정본 매치)로 마감, DR5-3(history limit) 전량 유지. **DR5-2(쓰기 유실창)은 정직 이월**(M7 — 파생 읽기⟂쓰기 내구성).
  - 🔗 캐시 소비 UI: 반응형 목록·북마크 별표가 로컬-first 상태를 화면에 표면화. 서버·프록시 불변. 〔캐시 트랙 M6 행위〕
- **M5 · ViewModel + StateFlow** — 2026-07-05 (브랜치 `feat/m5-viewmodel`, 로컬 커밋·미푸시). `commonMain/ui/`에 sealed `DetailUiState`(Loading/Result/Error)·`ErrorKind`+`toErrorKind`(sealed-when canary)·ViewModel 4종(`Detail`·`Search`·`Bookmark`·`History`). `TermRepository`만 주입(architecture §4.5). Detail은 load/refresh 단일 취소 슬롯 공유(refresh가 in-flight load 취소)·취소≠Error·toggleBookmark Found-only guard. Search는 300ms 디바운스·반응형 recent. 목록은 전부 `Flow`→`stateIn`(수동 재조회 없음, ADR-0002), History는 전량 노출. **green 4축**: `:shared:testDebugUnitTest`(ui 20) · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64` · **`:shared:iosSimulatorArm64Test`(ui 20 네이티브 실행)**. `lifecycle-viewmodel 2.9.6` iosSimulatorArm64 klib 소비 + `setMain` 네이티브 실행 실측(OQ-1 확정). 참조: [M5 슬라이스](docs/specs/m5-viewmodel-draft.md), spec 3-0·3-2·3-3.
  - **비준 ESCALATE(6R) → 게이트 완화 하 eyes-open**. DR6-2(단일 취소 슬롯) 구현 해소, DR5-1(guard)·DR5-4(취소≠Error) discriminating 테스트 방어. **이월**: OQ-3/DR-2 단일-writer Mutex 강제(M7 single 배선 게이트·다중스레드 실측 — M5는 코드 미착지·제안만), DR-4/DR5-2 상세 북마크 상태 소스+쓰기 유실창(M6), DR5-3 history limit 구체값(사람 게이트), AD-1 `TermRepository.kt` KDoc 과장 정정.
  - 🔗 캐시 소비측: 반응형 `stateIn` 목록이 DB 변경을 자동 반영(로컬-first pinning 표면화). 서버·프록시 계층 불변. 〔캐시 트랙 M5 행위〕
- **M4 · Repository 오케스트레이터** — 2026-07-05 (브랜치 `feat/m4-repository`, 로컬 커밋·미푸시). `commonMain/repository/`에 `TermRepository`(유일 인터페이스) + `TermRepositoryImpl`(3계층 read-through fetch: 정규화→번들→로컬 AI 캐시→네트워크, refresh는 캐시 우회·pinned `seenAt` 갱신, toggleBookmark, 반응형 `Flow`). `LocalTermStore`+`SqlDelightTermStore`(M2 쿼리 위임), `AnalyticsService`+Placeholder, `TermGenerator` 인터페이스(`ClaudeApi` 구현). **green 4축**: `:shared:testDebugUnitTest`(65) · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64` · **`:shared:iosSimulatorArm64Test`(53)**. 오케스트레이션(fetch 분기·pinning·clamp·createdAt 보존)이 네이티브 실행으로 실측(Fake 협력자 22건). 참조: [M4 슬라이스](docs/specs/m4-repository-draft.md), spec 2-3·2-4.
  - **⚠️ 상속 폐쇄**: createdAt 보존(M2 DR-M2-2 — §6-A refresh 후 정렬 안정성 실측)·schemaVersion Int범위(DR-M2-3 — 모든 쓰기가 `TermEntry.schemaVersion:Int?` 출처라 구성으로 보장)·AI category clamp(M3 §7-4 — 집합 밖→`기타`). **저장 keyword 정본화(AD-1)**: 모든 저장 경로가 `entry.copy(keyword=normalizeKeyword(input))`로 고정 — 대소문자 유의미 용어(`React`/`REST`)의 3단 캐시 영구 miss·중복 로우·재정렬 차단(§6-A 실측).
  - **비준 ESCALATE(6R, Blocker 1=AD-2) → 게이트 완화 하 eyes-open**. AD-2(M2 매퍼 주석 랜드마인) 주석 경로별 정정으로 해소, AD-3(Fake seam) `TermGenerator` 인터페이스로 해소, AD-1(keyword 소문자화) 번들 keyword 소문자 일관성으로 수용. DR-2 RMW 원자성(**단일-writer 계약을 인터페이스 전제조건으로 M5에 전파**)·DR-3 번들 category 게이트(데이터 트랙)·DR-4 크로스버전 승격은 이월.
  - 🔗 캐시 빌트인: 3계층 read-through·local-first pinning(INV-1·2·6·11) 클라 소비측 완성. 서버 D1 계층은 `ClaudeApi.generate` 안에서 서버가 처리(클라 투명). 〔캐시 트랙 M4 행위〕
- **M3 · 네트워킹 + 번들 로더 (클라측)** — 2026-07-05 (브랜치 `feat/m3-networking`, 로컬 커밋·미푸시). `commonMain`에 `BundleDbSource`(번들 `terms.json` 650 로드·정규화 인덱스 first-wins·keyword/alias 완전매칭·prefix autocomplete) + `ClaudeApi`(Ktor read-through 프록시 호출·`tool_use` 3분기→`TermResult`·`X-Device-Id`·429→DailyLimitExceeded) + 프롬프트/3도구(iOS 검증본 계승) + HttpClient 엔진 `expect`/`actual`(OkHttp/Darwin) + 공유 `AppJson`·`normalizeKeyword`. **green 4축 실측**: `:shared:testDebugUnitTest`(39) · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64` · **`:shared:iosSimulatorArm64Test`(31)**. **Ktor 3.5.1 × Kotlin 2.3.21 × serialization 1.9.0 klib 소비를 네이티브 링크·테스트로 실측**(§5 load-bearing). 참조: [M3 슬라이스](docs/specs/m3-networking-draft.md), spec 2-1·2-2, [ADR-0006](docs/adr/0006-server-cache-boundary.md).
  - **⚠️ INV-A 로더측 실측 = 폐쇄(M1 DR-1 바인딩)**: §6-B가 실 배포 `terms.json`을 `InMemoryBundleDbSource` 파서·인덱스에 태워 `aa-tree` aliases 내용·category + **alias 검색 성립**(`search("Arne Andersson tree")`→`aa-tree`)을 단언(성공 디코드는 무효 오라클). M1 fixture 대비 증분 폐쇄점. **네이티브 실행 갭 선제 폐쇄**: §6-A(BundleDbSource 매칭 9 + ClaudeApi×MockEngine 11)가 `:iosSimulatorArm64Test`로 네이티브 실행 — Native Ktor 파이프라인+Anthropic 응답 shape(thinking/text/tool_use) 직렬화 디코드 실측(M1·M2 비준 blocker였던 갭을 M3는 선제 폐쇄).
  - **비준 결과 = ESCALATE(6R, Blocker 1=AD-1) → 게이트 완화 하 eyes-open, AD-1은 구현으로 해소**. 6라운드가 draft를 강화(정규화 seam 제거·키잉vs프롬프트 분리로 대소문자 유의미 용어 어원오염 차단·에러처리 status선검사·flat DTO로 thinking블록 관용). 잔여 Blocker AD-1(2xx 비JSON/빈바디 `NoTransformationFoundException` 미포착)은 **수용이 아니라 구현에서 닫음**(catch 넓혀 InvalidResponse 봉인 + canned 2건). 상세: M3 슬라이스 Open Questions.
  - 🔗 캐시 빌트인: 클라를 read-through 계약(ADR-0006)에 투명하게 작성(리팩토링-0). **서버 트랙(devetym-proxy·INV-13)은 별도 이관**(§0). 클라측 category 정규화·fetch 3단은 M4. 〔캐시 트랙 M1·M4 클라 소비측〕
- **M2 · 로컬 DB** — 2026-07-05 (브랜치 `feat/m2-local-db`, 로컬 커밋·미푸시). SQLDelight 2.3.2([ADR-0003](docs/adr/0003-local-storage.md)): `.sq` 스키마(`term`·`searchHistory`, **pinning `seenAt` + 버전 `schemaVersion`/`promptVersion` 컬럼 처음부터** — INV-6·INV-9·INV-12, 마이그레이션 회피)·반응형 라벨 쿼리(`bookmarked`/`recent`, `.asFlow()` 대상)·드라이버 `expect`/`actual`(`AndroidSqliteDriver`/`NativeSqliteDriver`)·DTO↔엔티티 매퍼(`TermEntry.toEntity()`/`Term.toDto()`, aliases/source는 매퍼 변환). **green 4축 실측**: `:shared:testDebugUnitTest`(17) · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64` · **`:shared:iosSimulatorArm64Test`(11, B1 신규 축)**. **Kotlin 2.3.21 × SQLDelight 2.3.2 klib 소비를 네이티브 링크·테스트로 실측**(§5 load-bearing). 참조: [M2 슬라이스](docs/specs/m2-local-db-draft.md), spec 1-2.
  - **⚠️ INV-A 매핑측 실측 = 폐쇄(M1 DR-1 바인딩)**: 매퍼 `toEntity`/`toDto`의 `aliases`(순서)·`category` 무손실 보존을 §6-A 순수 commonTest로 실측(DoD 필수). aliases/source 변환을 매퍼에 둬(컬럼 어댑터 아님) 드라이버 없는 순수 왕복으로 성립. **B1 결착**: §6-A가 `:iosSimulatorArm64Test`로 **네이티브 실행**돼 Native `kotlinx.serialization` 왕복도 실측.
  - **비준 결과 = ESCALATE → 사람 eyes-open + B1 부분 폐쇄**(재비준 안 함). 6라운드가 draft를 강화(INV-9 무손실 M2경로 한정·`toEntity` 4필드 필수인자화로 M4 재주입누락 컴파일에러화·§6-B raw컬럼 canary). 잔존 Blocker(네이티브 실행 갭)를 **B1**(네이티브 실행 축 추가)로 직렬화 절반 폐쇄, DB 실행 절반은 M8 이월. 상세: M2 슬라이스 §5·§8·Open Questions.
  - 🔗 캐시 빌트인: pinning/버전 컬럼 = 로컬 head 계층 저장측(INV-6·INV-9·INV-12). 값 쓰기는 M4. 〔캐시 트랙 M4 저장측〕
- **M1 · 모델·직렬화** — 2026-07-05 (브랜치 `feat/m1-model-serialization`, 로컬 커밋·미푸시). `commonMain/model/`에 `TermEntry`(@Serializable, 버전 필드 옵셔널·INV-9)·`Source` enum·`TermResult` sealed interface·`Category` 정본 6어휘(pass-through, 강제 안 함). kotlinx.serialization JSON 왕복. 번들 `terms.json`(650) → `commonMain/composeResources/files/`(compose-resources 배선). `commonTest` §6 5종 + `androidUnitTest` fixture 1종(실제 번들 aliases 내용 단언). green 3축 실측: `:shared:testDebugUnitTest`(6 pass) · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64`. **serialization 1.9.0 ↔ Kotlin 2.3.21 호환 빌드로 실측**(§5 load-bearing). 참조: [M1 슬라이스](docs/specs/m1-model-serialization-draft.md), spec 1-1.
  - **비준 결과 = ESCALATE → 사람 eyes-open 수용**(재비준 안 함). Blocker 3 결착: **DR-3**(sealed `when` else 금지) 슬라이스 §6에서 닫음 · **DR-1**(INV-A 실측 범위) M1 fixture로 wire측 부분 폐쇄 + 매핑측(M2)·로더 회귀(M3) 바인딩 상속(위 M2·M3 ⚠️ 항목) · **DR-2**(서버 캐시-히트 정규화 우회) [cache-delivery-milestones](docs/cache-delivery-milestones.md) **INV-13**(정규화-후-캐시쓰기)로 이관. 상세는 슬라이스 §8·Open Questions.
  - 🔗 캐시 빌트인: entry 계약 = read-through 응답 shape. INV-9 버전 태깅 반영(`schemaVersion`/`promptVersion` 옵셔널). 〔캐시 트랙 M0-클라측〕
- **M0 · KMP 골격** — 2026-07-04 (`feat/m0-kmp-scaffold` → `main`, no-ff 병합). Android APK + iOS 시뮬레이터 실제 실행 확인. `shared + androidApp + iosApp`, Koin `startKoin` 배선, 공유 `Greeting`을 양 플랫폼 Compose 화면에 표시. green 루프 3축: `:shared:testDebugUnitTest` · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64`(SKIE 포함). 참조: architecture §3·§5, spec 1-6.
  - ✅ iOS interop 결정: **SKIE**([ADR-0005](docs/adr/0005-ios-interop.md)). 골격 버전(**빌드 실측 확정**): **Kotlin 2.3.21 · CMP 1.11.1 · AGP 8.13.0 · Gradle 8.13 · SKIE 0.10.12**. ⚠️ **SKIE 0.10.12는 Kotlin 최대 2.3.21**(2.4.0 거부, 실측) — SKIE가 새 Kotlin 지원 전엔 앞질러 올리지 말 것.
- **프로젝트 문서 세트 수립** — 2026-07-04
  - **그린필드 CMP 설계로** README·[PRD](docs/product/prd.md)·[아키텍처 설계서](docs/architecture.md)·[ADR 0001~0004](docs/adr/)·[Spec](docs/specs/spec.md) 작성.
  - 동일 제품의 iOS(`dev-etymology`, SwiftUI) 구현에서 **검증된 데이터 흐름·설계 불변식을 계승**(fetch 3단·lazy 저장·upsert·aliases 보존·tool_use 3분기·프록시 계약), 관용구는 **코틀린으로**([ADR-0002](docs/adr/0002-code-idiom-principle.md): 리터럴 포팅 금지, 우회 패턴은 삭제).
  - 결정: CMP(UI까지 공유, [ADR-0001](docs/adr/0001-cross-platform-framework.md)) / 로컬 DB SQLDelight 우선·미확정([ADR-0003](docs/adr/0003-local-storage.md)) / 프록시 계약 계승([ADR-0004](docs/adr/0004-backend-proxy-boundary.md)).
- **repo 개설** — 2026-07-04. `devetym`(git init, 계정 `data-sy` 예약).

---

## 작업 단위 분할 원칙

작은 단일 앱이라 가벼운 구조를 쓴다.
- **Roadmap** — 모든 작업의 단일 인덱스이자 진행 상태 정본 (이 문서).
- **Architecture** — [`docs/architecture.md`](docs/architecture.md), 기술 설계 정본.
- **Spec** — [`docs/specs/spec.md`](docs/specs/spec.md), 구현 명세(Phase 1~4).
- **ADR** — 돌이킬 수 없는 결정 ([`docs/adr/`](docs/adr/)).

## 갱신 규칙

- 마일스톤 착수 시 Now로 이동, 브랜치명 함께 기록.
- 완료 시 Done으로 이동, 완료일·PR 번호 기록. 의사결정이 있었다면 ADR 번호도 함께.
- 새 아이디어는 Later에 먼저 추가하고, 우선순위가 오르면 Next로 승격.
- 보류 작업은 Next에 두고 "보류 사유" 명시.

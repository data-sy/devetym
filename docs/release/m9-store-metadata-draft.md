# M9 스토어 제출 메타데이터 · 개인정보 라벨 초안 (2026-07-05)

> **성격: `[AI→사람]` 지그.** Play Store·App Store 제출 필드 초안 + 데이터 안전/개인정보 라벨.
> **개인정보 라벨 단일 정본 = [`site/privacy-policy.md`](../../site/privacy-policy.md)**(§7 Q4 고정 — 불일치 방지).
> 아래 라벨은 방침 §1·§5에서 **파생**했다. 방침이 바뀌면 이 문서가 아니라 방침을 고치고 여기 재파생.
> 사람은 검토·수정·붙여넣기. `[사람]` 잔여 = 실제 콘솔 입력·심사 제출.

## 1. 공통 앱 정보
| 필드 | 값(초안) |
|---|---|
| 앱 이름 | 개발 어원 사전 (DevEtym) |
| 개인정보 처리방침 URL | https://data-sy.github.io/devetym/privacy-policy (약관: https://data-sy.github.io/devetym/terms-of-service) — 2026-07-13 Pages 배포·라이브 |
| 지원 이메일 | oddmuffinstudio@gmail.com (방침 §8) |
| 카테고리 | 교육 / 개발자 도구 (Education / Developer Tools) |
| 대상 연령 | 만 14세 이상 (방침 §6 — 아동 대상 아님) |

## 2. 스토어 설명 초안 (검토·수정 대상)
**짧은 설명(80자 이내):**
> 개발 용어의 어원과 명명 이유를 찾아주는 사전. 오프라인 번들 + AI 생성.

**전체 설명 — D5 확정(2026-07-13): 후크형(부록 A) 채택.** 정본 텍스트는 [부록 A "후크형 전체 설명"](#부록-a-대안-aso-카피-옵션-dev-etymology-초안-계승--wu-5) — 콘솔 입력 시 그대로 사용.

> ~~구 현행안(기능 나열형)은 폐기~~ — "익명 이용 데이터는 동의한 경우에만 수집" 문구가 방침 "현재 미수집"과 불일치(정합 결함)했고, 후크형은 수집 문구 없이 정합. 결정 로그 D5 참조.

**프로모션 텍스트 — D5 확정(2026-07-13):** 부록 A 초안(107자) 채택 — `'버그'는 왜 버그일까? 개발 용어의 어원과 작명 이유를 한국어로 풀어주는 사전. 650개 용어를 오프라인에서 즉시 찾고, 없는 용어는 AI가 설명합니다. 면접·학습용 북마크와 검색 기록까지.`

**키워드(App Store) — D4 확정(2026-07-13), 95자:**
> `코딩,컴퓨터공학,CS,IT,개발자,기술면접,취업,자료구조,알고리즘,주니어,비전공,용어집,영어단어,학습,깃허브,오픈소스,뜻,의미,신입,부트캠프,컴공,개발상식,전산,바이브코딩`

(구 기본 8개안은 D3 확정 이름·부제와 중복이라 폐기 — 결정 로그 D4 참조)

## 3. 데이터 안전 / 개인정보 라벨 (방침에서 파생 — 정본은 방침)

> **정합 갱신(2026-07-06):** 방침을 "현재 미수집"으로 정합(블로커 #1 결정 — [LAUNCH-CHECKLIST](LAUNCH-CHECKLIST.md)). Firebase/애널리틱스 미도입(`instanceId()=null`, no-op `PlaceholderAnalyticsService`) 상태를 라벨에 반영. **실제로 기기를 떠나는 데이터는 AI 폴백(번들 미스) 시의 검색어 + 한도용 기기 식별자뿐**(방침 §2-1).
> **정합 갱신(2026-07-10 · WU-4):** 크래시 리포팅(Sentry) 도입 → **크래시 진단 데이터(성능/진단)**가 새로 전송 카테고리에 편입(방침 §2-2). PII 미부착(`sendDefaultPii=false`)·오류 수정 목적·비추적. 애널리틱스는 여전히 미수집.

### 3-1. 수집·전송 데이터 (방침 §1~§3)
| 데이터 유형 | 전송/수집? | 목적 | 사용자-링크 | 비고 |
|---|---|---|---|---|
| 검색 키워드 (번들 미스 시에만) | ✅ 전송 (서버 처리) | **앱 기능**(AI 정의 생성) | ❌ 비연결 | 방침 §2. 통계·프로파일링 아님 |
| 기기 식별자 `X-Device-Id` (임의 값, 재설치 시 리셋) | ✅ 전송 | **앱 기능**(일일 한도 계수) | ❌ 실명·광고ID 무연결 | 방침 §2-1 |
| 크래시 진단(스택트레이스·기기/OS·앱 버전) | ✅ 전송 (크래시 시에만) | **앱 안정성**(오류 진단·수정) | ❌ 비연결(PII off) | 방침 §2-2. 수탁자=Sentry. 통계·추적 아님 |
| 사용 통계(애널리틱스) | ❌ 미수집 | — | — | Firebase/GA **미통합**(방침 §3·§4) |
| 이름·이메일·전화 | ❌ | — | — | 미수집(방침 §3) |
| 광고 식별자(IDFA/AAID) | ❌ | — | — | 미수집(방침 §3) |
| 위치 | ❌ | — | — | 미수집(방침 §3) |
| 북마크·검색 히스토리·열람 내용 | ❌ 전송 안 함 | — | — | **기기 내에만 저장**(방침 §1) |

### 3-2. Play Console 「데이터 보안」 매핑
- **수집/공유 항목**: 앱 활동/검색(검색 키워드 — 번들 미스 시 앱 기능 목적), 기기/기타 ID(임의 `X-Device-Id` — 앱 기능/한도 목적), **앱 활동 → 진단(크래시 로그) — 앱 안정성 목적(방침 §2-2)**. **애널리틱스 없음.**
- 목적: **앱 기능(App functionality)** + **크래시 로그(Crash logs, 진단)**. 분석·광고·개인화 아님.
- 크래시 로그: 「수집」 O, 「공유」 X, 사용자와 **비연결**, 수집 선택성=필수(오류 발생 시 자동). 수탁자=Sentry.
- 전송 암호화: ✅ HTTPS(방침 §2). 데이터를 서버에 영구 저장하지 않음(기능 처리용) — 크래시 로그는 진단 보관 기간 내 Sentry 보관.
- 검색 데이터 수집은 **AI 생성 검색 기능 사용 시에만** 발생(번들 검색만 하면 미전송), 크래시 로그는 **크래시 발생 시에만** 발생.

### 3-3. App Store 「App Privacy」 매핑
- **Data Not Linked to You**: User Content(검색 키워드 — App Functionality), Identifiers(임의 `X-Device-Id` — App Functionality), **Diagnostics → Crash Data(크래시 진단 — App Functionality/Analytics 아님, PII off)**.
- **Data Used to Track You**: 없음(광고 ID·크로스앱 추적·애널리틱스 미수집 — 방침 §3).
- Tracking(ATT) 프롬프트: **불필요**(추적 데이터 없음 — 크래시 진단은 추적 아님).

## 4. ⚠️ 실 제출 전 확인 (사람)
- ☑ **블로커 #1 해소(2026-07-06)**: 방침·라벨을 "현재 미수집(애널리틱스 없음)"으로 정합. Firebase는 도입 시 방침 §4 절차로 재갱신 후 라벨 갱신.
- ☑ **크래시 리포팅 도입(2026-07-10 · WU-4)**: 방침 §2-2 신설 + 라벨에 크래시 진단 반영. ☐ 실 제출 시 Play 「데이터 보안」에 **크래시 로그** 체크·App Store 「Diagnostics/Crash Data」 체크 누락 없나 확인. ☐ 배포 빌드에 실 DSN 주입(Android=`-PSENTRY_DSN`/CI 시크릿, iOS=Info.plist `SentryDsn` 빌드세팅/xcconfig·CI). ☐ iOS 크래시 배선은 WU-4B 단일 KMP로 완료(Xcode 빌드 검증) — 실 DSN 런타임 도달만 실기기 스모크서 확인 후 라벨 확정.
- ☑ **방침 배포 URL 확정(2026-07-13 · 블로커 #2 해소)**: GitHub Pages 라이브 → 위 §1 반영(privacy-policy·terms-of-service, 전부 200). ☐ 앱 내 링크(WU-6 잔여 인앱 방침 URL)와 최종 일치 확인.
- ☐ 스토어 라벨 입력 시 "검색 키워드 서버 전송"을 각 콘솔 문항에 맞게 분류(App Functionality 목적 강조)
- ☐ 스토어별 설명 글자수 제한 재확인
- ☐ 스크린샷·프로모 자산(별도 핸드오프 — [m9-screenshot-capture-handoff](m9-screenshot-capture-handoff.md))
- ☐ **Kids Category / Designed-for-Families 등록 금지** — 설명이 임의 입력에 대한 AI 생성 응답이라 아동 카테고리 부적합(§1 "만 14세 이상"과 정합). 콘텐츠 등급 자체는 폭력·성인물 없어 낮으나, 아동 전용 카테고리에는 넣지 않는다.

---

## 5. App Review 심사 노트 (D9 확정 — 2026-07-13) `[사람: 콘솔 App Review Information란에 붙여넣기]`

> 리젝 리스크 상위 3종(무검수 AI 생성 · 심사 환경 네트워크 실패 · 최소 기능성 오해)을 선제 해소하는 구조. 영어 단독(App Review 글로벌 팀 표준).

```
[App overview] "개발 어원 사전" (DevEtym) is a Korean-language dictionary that explains
the etymology and naming rationale of software development terms (e.g. why a "bug" is
called a bug). Korean UI, releasing in South Korea only.

[No account needed] No login, no registration. All features are available immediately
after install.

[How to demo — offline] 650+ terms are bundled offline. Search "mutex" or "daemon" to
see a full entry (etymology, naming rationale, category) with no network required.

[How to demo — AI fallback] Searching a term not in the bundle (e.g. "quicksort")
generates an entry via AI. Requests go through our proxy server — no API key is
embedded in the app. AI generation has a per-device daily limit; the bundled 650 terms
are unlimited and work offline, so the app remains fully functional if the network or
the daily limit blocks AI generation during review.

[AI-generated content] AI-generated entries are disclosed to users as AI-generated and
are not individually human-reviewed. Safeguards: generation is domain-limited to
explaining the searched programming term's etymology (not an open chatbot), per-device
daily limit, and an in-app report path for flagging incorrect content. This is also
reflected in our age-rating questionnaire answers.

[Privacy] No analytics SDK. Data leaving the device: the searched keyword + a random
per-install device ID (only on AI fallback, for the daily limit), and crash diagnostics
via Sentry (no PII). Bookmarks/history stay on device.
Policy: https://data-sy.github.io/devetym/privacy-policy

[Contact] oddmuffinstudio@gmail.com
```

---

## 부록 A. 대안 ASO 카피 옵션 (dev-etymology 초안 계승 — WU-5)

> **성격: 선택적 카피 대안 풀.** dev-etymology(iOS) App Store 초안의 ASO 카피를 계승. **§1·§3(연령·카테고리·개인정보)은 devetym 정본이 우선** — 아래 iOS 초안이 권했던 `4+`·Firebase 수집 전제는 **무효**(devetym은 만 14세 이상·미수집). 이름/설명 카피만 재사용 후보로 둔다.
> **`부제(subtitle)`·`프로모션 텍스트`는 App Store 전용 필드**(Play는 대응 필드 없음).

### 앱 이름 (≤30자) — **D3 확정(2026-07-13): A**
- **A (✅ 확정)**: `개발 어원 사전` — 브랜드 단순·정확 매칭. (§1 등록명)
- ~~B: `개발 어원 사전: 코딩 용어 뜻`~~ — 2.3.7 키워드 스터핑 인상 리스크로 기각.

### 부제 (App Store, ≤30자) — **D3 확정(2026-07-13): A**
- **A (✅ 확정)**: `프로그래밍 용어의 유래와 작명 이유` (19자) — 차별점형·포지셔닝 일관성 우선(사용자 확정).
- ~~B: `면접 준비 코딩 용어, 오프라인 즉답`~~ — 세션 추천이었으나 기각.
- ~~C: `650개 용어 오프라인 사전+AI 설명`~~ — 기각.

### 키워드 대안 (App Store, ≤100자·쉼표구분·공백없음)
이름/부제 단어는 자동 색인 → 중복 제외 후 100자 가깝게 충전 권장.
- `코딩,컴퓨터공학,CS,IT,개발자,기술면접,취업,자료구조,알고리즘,주니어,비전공,용어집,영어단어,학습,깃허브,오픈소스`

### 프로모션 텍스트 (App Store, ≤170자·심사없이 교체 가능)
- `'버그'는 왜 버그일까? 개발 용어의 어원과 작명 이유를 한국어로 풀어주는 사전. 650개 용어를 오프라인에서 즉시 찾고, 없는 용어는 AI가 설명합니다. 면접·학습용 북마크와 검색 기록까지.` (107자)

### 후크형 전체 설명 대안 (도입 3줄에 후크+정의)
> `'버그(bug)'는 왜 버그일까요? 진짜 나방 때문이었습니다.`
> 개발 어원 사전은 매일 쓰는 개발 용어가 왜 그런 이름을 갖게 됐는지, 그 어원과 작명의 맥락을 한국어로 풀어주는 사전입니다.
>
> 단어의 철자만 외우는 공부는 금방 잊힙니다. '왜 이 이름인가'를 알면 개념이 오래 남습니다. 면접을 앞둔 주니어 개발자, CS를 배우는 전공·비전공 학생, 용어의 진짜 뜻이 궁금한 모든 개발자를 위해 만들었습니다.
>
> **[주요 기능]** 어원·작명 관점 설명 / 650개 용어 오프라인 내장(즉시 검색·자동완성) / 없는 용어는 AI(Claude) 생성 / 북마크 / 검색 기록.
> **[알아두실 점]** 모든 설명은 AI가 생성하며 사람의 개별 검수를 거치지 않습니다(앱 내 제보로 개선). AI 생성은 기기당 하루 횟수 제한이 있고, 내장 650개는 제한 없이 오프라인. 로그인·계정 불필요.
>
> 문의: oddmuffinstudio@gmail.com

⚠️ 위 후크형 설명은 §2 현행 설명보다 마케팅이 강하나, **수집 관련 문구는 §3(미수집 정합)과 반드시 일치**시킬 것 — iOS 초안의 "opt-in 수집" 전제를 그대로 옮기지 말 것(위 대안엔 수집 문구 없음·정합).

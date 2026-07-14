# Claude API 비용 관리·모니터링 컨설팅 요청

> 이 프롬프트를 새 세션에 붙여넣어 컨설팅을 진행한다. 작업 디렉토리: `~/devetym`

## 내 상황

나는 **비개발자**이고, 이 앱(devetym — 개발 용어 어원 사전, Kotlin Multiplatform iOS 앱)을 바이브코딩으로 만들었다. 개발 과정에서도, 출시 후 운영에서도 Claude API를 쓰는데 **비용이 얼마나 나가는지, 앞으로 얼마나 나갈지 전혀 보이지 않아서 겁이 난다.** 비용을 관리하고 모니터링하는 체계를 컨설팅해 달라.

## 컨설팅 원칙 (중요)

1. **국소 패치보다 근본 해법을 우선한다.** 나는 이 프로젝트에서 iOS/Android를 각각 만들지 않고 KMP 하나로 통합하는 식의 선택을 해왔다. 비용 문제도 같은 태도로 접근해 달라 — 스크립트 하나에 로그 한 줄 심는 식이 아니라, 비용이 발생하는 모든 지점을 한 체계로 덮는 구조가 있으면 그쪽을 제안하라.
2. **추측 금지, 코드·문서 먼저 읽기.** 아래 "읽을 파일" 목록을 실제로 읽고 사실에 기반해 제안하라.
3. **비개발자가 이해할 언어로.** 옵션을 비교할 때는 트레이드오프(비용·구현 난이도·유지보수 부담)를 풀어서 설명하고, 추천 1개를 명시하라.
4. **조사 단계에서 실제 Claude API를 호출해 돈을 쓰지 마라.**
5. 결정에 필요한 정보(월 예산 상한, Admin API 키 보유 여부, 예상 사용자 규모 등)는 먼저 나에게 물어보라.

## 사전 조사로 이미 확인된 사실 (2026-07-14 기준)

비용이 발생하는 표면은 3곳이다:

1. **개발용 스크립트 (내 로컬에서 직접 호출, `ANTHROPIC_API_KEY` 사용)**
   - `Scripts/generate_db.py` — 번들 DB 배치 생성. `claude-sonnet-4-6`, `max_tokens: 8192`, 실시간 Messages API 직접 호출 (Batch API 아님 → 50% 할인 기회 미사용)
   - `Scripts/prompt-probe/probe_prompt.py` — 프롬프트 품질 프로브 (반복 호출 구조)
   - `Scripts/db-expand/` — DB 확장 라운드용. 향후 라운드마다 반복될 예정
2. **런타임 앱 (AI 폴백)** — 앱 → Cloudflare Worker 프록시(`devetym-proxy`, **별도 GitHub repo — 이 로컬에는 없음**) → Claude API. 키는 서버에만 있고, 기기당 일일 호출 한도가 있다. ADR-0006으로 서버 D1 read-through 캐시(용어당 정본 1회 생성 후 동결, write-once)가 확정되어 있다.
3. **향후 운영** — 사용자 증가 시 캐시 미스 트래픽, DB 확장 라운드, 프롬프트 버전업에 따른 재생성.

**현재 비용 가시성: 0.** 스크립트·앱·프록시 계약 어디에도 `response.usage`(토큰 수)를 기록하는 코드가 없다. 크레딧 잔액은 API로 조회 불가(Anthropic Console 전용)이고, 조직 단위 사용량/비용 조회는 Admin API(`cost_report` 등)로 가능하나 Admin 키(`sk-ant-admin...`)가 필요하다.

## 읽을 파일

- `Scripts/generate_db.py`, `Scripts/prompt-probe/probe_prompt.py`, `Scripts/db-expand/`
- `docs/adr/0006-server-cache-boundary.md` (현행 정본), `docs/adr/0004-backend-proxy-boundary.md` (superseded, 프록시 계약 배경)
- `docs/cache-delivery-milestones.md` (아키텍처 불변식 INV-1~13)
- `docs/db-expand/spec.md` (향후 API 호출이 반복될 트랙)
- `shared/src/commonMain/kotlin/com/robin/devetym/data/remote/` (앱 쪽 Claude 호출), `shared/src/commonMain/kotlin/com/robin/devetym/Constants.kt` (모델 상수)

## 컨설팅에서 답해줄 것

1. **비용 지도**: 위 3개 표면 각각에서 비용이 어떻게 발생하는지, 그리고 각각의 **최악 시나리오**(비용이 폭증할 수 있는 경로 — 예: 스크립트 무한 재시도, 프록시 한도 우회, 캐시 무효화 폭주)는 무엇인지.
2. **근본적 관리 체계 제안**: 예를 들어 다음 층위를 검토하되, 이 목록에 갇히지 말 것 —
   - **가시성 층**: 모든 호출 지점이 공유하는 usage/비용 로깅을 한 번에 세우는 방법 (스크립트 공통 모듈? 프록시에서 일괄 기록? Admin API cost_report 정기 조회?)
   - **상한 층**: Console spend limit·알림, 프록시 측 하드 캡, 스크립트 측 예산 가드
   - **절감 층**: 개발 파이프라인의 Batch API 전환(50% 할인), prompt caching, 모델 선택 재검토, 캐시 적중률을 높여 API를 진짜 최후수단으로 만드는 구조
3. **우선순위 로드맵**: "지금 당장 1시간짜리" → "다음 마일스톤에 넣을 것" → "사용자 늘면 할 것" 순으로.
4. **산출물**: 옵션 비교·추천·로드맵을 담은 결정 문서를 `docs/cost/` 아래에 작성하고, 내 승인 후 구현에 들어가라.

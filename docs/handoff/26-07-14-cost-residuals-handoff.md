# 핸드오프: 비용 관리 트랙 잔여 2건 + 서류 최신화 + D1 재배포 (2026-07-14)

> **다음 세션 시작점.** 비용 관리·모니터링 체계는 2026-07-14 구현·머지·배포 완료(ROADMAP Done "비용 관리·모니터링 체계" 항목). 이 문서는 그 **잔여 2건**을 닫는 세션의 정본이다.
> 배경 정본: [`docs/cost/cost-management-decision.md`](../cost/cost-management-decision.md)(결정·체크리스트) · [`docs/cost/console-settings-log.md`](../cost/console-settings-log.md)(Console 설정 스냅샷)

## 현재 상태 (이 문서 작성 시점)

- **완료·라이브**: devetym PR #13, devetym-proxy PR #1 머지. 프록시 하드닝 **프로덕션 배포됨**(버전 `cf5a7158`) — 과금 파라미터 서버 강제·기기 10/일·전역 200/일·402 서비스 소진 계약. Console: 조직·워크스페이스(Proxy/Experiments)·월 한도 $30·알림 10/20/25·크레딧 자동 리로드 $5→$15.
- **⚠️ devetym `main` 로컬 커밋 미푸시**: `f610422`(운영 문서 refresh) 및 이 핸드오프 커밋. 다른 세션(m9 스크린샷 트랙)도 main에 직접 커밋 중이니 **푸시 전 `git log origin/main..main`으로 합류분 확인**.
- **usage D1 기록만 미활성**: 코드·마이그레이션은 배포돼 있고 `USAGE_DB` 미바인딩이라 자동 스킵 중.

## 작업 1 — D1 활성화 + 재배포

**막힌 지점**: wrangler 토큰에 D1 스코프 없음 (`d1 create` → AuthenticationError code 10000. KV/Worker API는 정상 — 배포는 됐음).

1. **[사람]** Cloudflare 재로그인: 입력창에 `! cd ~/devetym-proxy && npx wrangler login` → 브라우저 로그인 → Allow.
   - 계정: **data.sy.2@gmail.com** ("Data.sy.2@gmail.com's Account"). 비번 모르면: https://dash.cloudflare.com/login 에서 **Sign in with Google 먼저 시도**, 안 되면 https://dash.cloudflare.com/forgot-password
2. **[AI]** `npx wrangler d1 create devetym-usage` → 출력된 `database_id` 확보
3. **[AI]** `~/devetym-proxy/wrangler.toml`의 `⑤` 블록 주석 해제 + id 기입 (절차 주석이 파일 안에 있음)
4. **[AI]** `npx wrangler d1 migrations apply devetym-usage --remote` (마이그레이션: `migrations/0001_usage_log.sql`)
5. **[AI]** `npx wrangler deploy` → 바인딩 목록에 `USAGE_DB` 표시 확인
6. **[AI]** 검증(무과금): 라이브 프록시에 GET(405)·비JSON(400)·40KB(413) 스모크. usage 실적재 확인은 실제 검색 1회가 필요하므로 앱/에뮬 검색 후 `npx wrangler d1 execute devetym-usage --remote --command "SELECT * FROM usage_log LIMIT 5"` — 비용 ~$0.03, 사람 컨펌 후.
7. **[AI]** devetym-proxy 변경 커밋·푸시(main 직접 또는 PR — 이전 관례는 PR·**브랜치 보존**)

## 작업 2 — Admin 키 → 첫 비용 리포트

**상태**: Admin 키 미발급 확인됨(로컬 env·.env.local 모두 없음, 2026-07-14 실측). 사용자가 Console에서 발급 중이었음 — **발급 완료 여부부터 확인**.

1. **[사람]** Console → 조직 Settings → **Admin keys**에서 발급 (`sk-ant-admin01-...` 접두사. 일반 키 `sk-ant-api03-...`와 다름 — "devetym-proxy-prod-2026-q3"는 프록시용 일반 키다)
2. **[사람]** 키 전달: `! echo 'ANTHROPIC_ADMIN_KEY=sk-ant-admin01-...' > ~/devetym/Scripts/cost/.env.local` (gitignore `Scripts/**/.env.local`로 커밋 불가 — 확인됨)
3. **[AI]** 실행: `set -a; source Scripts/cost/.env.local; set +a; python3 Scripts/cost/report.py` → 워크스페이스별 비용·캐시 적중률 표가 나오면 성공. 401/403이면 스크립트가 원인 안내(개인 계정/일반 키).
   - ⚠️ 첫 실행은 데이터가 거의 없어 `$0.20` 수준이 정상. 응답 스키마가 추정과 다르면(합계 0 등) `admin_get` 원본 JSON을 찍어 필드명 대조 후 `collect_costs`/`sum_tokens` 보정.

## 작업 3 — 서류 최신화 + 마무리

- [`cost-management-decision.md`](../cost/cost-management-decision.md) 체크리스트 잔여 2건([ ] Admin 키 / [ ] D1 활성화) 체크
- [`console-settings-log.md`](../cost/console-settings-log.md)에 새 날짜 항목 append: D1 활성화·Admin 키 발급(키 값은 절대 기록 금지, "발급됨"만)
- `ROADMAP.md` Later "[Server] 프록시 토큰 usage 기록" 항목의 잔여 문구 해소 표시 + Done 항목의 "잔여" 문구 갱신
- 이 핸드오프 상단에 완료 표시 추가
- devetym main 커밋·**푸시**(사람 승인 하에 — 위 미푸시 주의 참조)

## 범위 밖 (건드리지 말 것)

- **402 놓친 검색어 수집** — ROADMAP Later 백로그(2026-07-14 발의). 사용자가 **별도 새 세션**에서 진행 예정. D1 활성화(작업 1)가 선행 의존이니 이 세션이 끝나면 착수 가능해진다.
- m9 스크린샷/스토어 제출 트랙 — 병행 세션 소관. 작업 트리에 그쪽 미커밋 파일이 보여도 건드리지 않는다.

## 로어 (다음 세션이 밟지 말 것)

- wrangler 배포 직후 엣지 전파 ~수십 초 혼재 — 스모크가 구버전에 닿으면 20초 후 재시도.
- `gh pr merge` 뒤 로컬 main은 `git pull origin main` 명시(upstream 미설정 상태였음).
- probe·report 등 스크립트 검증 시 실 API 과금 금지 원칙 유지(가짜 키·무과금 경로 활용).

# 🤖 승격 라운드 001 — critic 심사 요청 (사람 실행)

> **일회용 문서다.** 이 라운드의 승격이 끝나면 `promote-round-001*.json`과 함께 지운다
> (결정·근거는 커밋 이력에 남는다). 항구적인 절차 정본은
> [`docs/db-expand/runbook-manual-round.md`](../../docs/db-expand/runbook-manual-round.md),
> 자격 규범은 [ADR-0014](../../docs/adr/0014-promotion-eligibility.md)다 — 여기 복제하지 않는다.

## 현재 = 2회차 (재생성 루프 3회 중 2회차)

**1회차 결과: 8/10 통과.** 걸린 2건을 `fix_direction`대로 고쳤고, 정량 게이트는 다시 10/10이다.

| keyword | 룰 | 고친 것 |
|---|---|---|
| `shedlock` | `RULE_ETYMOLOGY_FACT` | 제작자 이름(`Lukáš Rychtecký`)이 사실과 어긋난다는 지적. **인물 언급을 삭제**하고 Spring `@Scheduled` 연동·Maven 좌표라는 검증 가능한 사실로 대체했다 — 표기를 확증할 수 없는 다른 이름으로 바꾸면 같은 종류의 오류를 한 번 더 심는다 |
| `harness` | `RULE_ALIAS_STRICT` | 한정 수식어 변형 `test harness`·`테스트 하네스` 제거. alias는 `하네스` 하나 |

## 순서

1. **claude.ai에서 임시 채팅**을 새로 연다 (1회차 탭에 이어 붙이지 말 것 — 격리 지시가 그렇게 요구한다).
2. 시스템 지침 자리에 붙여넣기: [`prompts/critic-v2.paste.md`](prompts/critic-v2.paste.md) 전문.
3. 사용자 메시지로 붙여넣기: [`promote-round-001-r2.json`](promote-round-001-r2.json) — **고친 2건만** 들어 있다.
4. 출력 `{"passed": [...], "failed": [...]}`를 Claude Code에 그대로 주면 된다.

`failed: []`가 나오면 10건 전량이 승격 자격을 갖춘다 →
merge → seed_d1 → export_bundle → 카운트 갱신 → 웹 재배포.

3회차 안에 안 닫히면 entry 문제가 아니라 룰 문제 신호다(런북).

## 이번 라운드에서 알아 둘 것

- **`shedlock`의 인물 오류는 지금 라이브에 서 있다.** 원본 생성물에서 그대로 딸려온 값이라
  `devetym.com/term/shedlock`과 앱 캐시가 틀린 이름을 보여 주는 중이다. 승격이
  D1 행을 덮으면(authored > generated · 밀려난 본은 `entry_versions`) 그때 함께 고쳐진다.
- **`harness`의 별칭 제거는 반쪽만 적용된다.** D1 `aliases`에는 이미
  `testharness`·`테스트하네스`·`하네스` 세 행이 있고 시딩은 별칭을 지우지 않는다.
  즉 API·조회 경로에서는 「테스트 하네스」로 계속 찾아지고, **웹 정적 검색 인덱스에서만 빠진다.**
  한글 검색 자산이 줄어드는 쪽이라 별칭 정리 정책은 별건으로 봐야 한다.

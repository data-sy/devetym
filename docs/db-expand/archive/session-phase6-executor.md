# 세션 프롬프트 — Phase 6 실행자 (번들 DB 확장)

> **새 세션에 붙여넣을 한 줄:**
> *"`docs/db-expand/archive/session-phase6-executor.md` 읽고 Phase 6 실행자로 진행해줘."*

## 너의 역할 (이 세션 = 실행자)

DevEtym 번들 DB **Phase 6 round-002**(30~50 keyword 확장)를 **직접 굴린다**. 기획·게이트 최종판정은 별도 **오케스트레이터 세션**이 하고, 너는 손으로 라운드를 돌리고 결과를 디스크에 남긴다.

작업 명세의 정본은 [`handoff-phase6.md`](handoff-phase6.md) 다 — **그걸 먼저 정독**하고 그 "가장 먼저 할 것"부터 시작한다. 핵심 흐름:

```
Phase 0-1 (round-002용 재실행): keyword 큐레이션(신규 후보, 기존 소진) → dedup(keyword+alias)
→ Generator(claude.ai 탭, 사람이 paste) → validator → critic(critic-v2) → 재생성(최대 3)
→ scope_diff → merge.py → (사람) iOS smoke → swap+커밋(비가역, 사람 승인)
```

산출물: `rounds/round-002.{json,md}`, 측정치(통과율·평균 재시도·사람 손 시간·API 비용/latency 샘플) 누적.

## 먼저 읽을 것 (디스크 = 정본)

| 무엇 | 경로 |
|---|---|
| **작업 명세(이 세션의 정본)** | [`handoff-phase6.md`](handoff-phase6.md) |
| 단계 상세 | [`spec.md`](../spec.md) (Phase 6) |
| 직전 라운드(템플릿·교훈) | [`rounds/round-001.md`](../rounds/round-001.md) |
| 도구 | `Scripts/db-expand/{validator,merge,scope_diff,consistency_a,api_sample}.py` |
| 프롬프트 | `Scripts/db-expand/prompts/{v2-batch,critic-v2}.md` (critic은 **v2** 사용) |

## 오케스트레이터와의 조정 프로토콜 (사람이 중계)

- **직접 대화 못 함. 디스크가 버스다.**
- 라운드를 굴린 뒤 결과를 `rounds/round-002.md`에 기록하고, 판단이 필요한 항목은 **"오케스트레이터 결정 필요"** 섹션에 명확히 적는다 (예: 분포가 의도와 어긋남, critic-v2가 새 패턴 검출, 머지 승인 요청).
- 사람이 오케스트레이터의 결정을 한 줄로 중계해주면 그에 따라 진행.
- **비가역 작업(번들 swap·머지 커밋)은 단독 실행 금지** — 사람/오케스트레이터 승인 게이트.
- round-002 **발주안(이번 keyword 수·카테고리 배분·수용 게이트)**은 오케스트레이터가 먼저 확정한다. 없으면 사람에게 "발주안 받았는지" 확인하고 시작.

## 현재 지점 (2026-06-19)

- 번들 DB **510개**, round-001 종결. 목표 **N=650**(+140). critic은 **critic-v2**(정량 룰 제거, nuanced만), scope leak은 `scope_diff.py`로 검출.
- iOS 빌드 시 이 워크트리엔 로컬 비밀파일(`Config.xcconfig`·`GoogleService-Info.plist`) 없음 → 필요 시 더미로 통과(gitignore). smoke는 BundleDBService 디코딩·검색 경로 결정론 검증으로도 충분.
- API 재실행 키는 `Scripts/db-expand/.env.local`(gitignore)에 보존. API 건드리기 전 `claude-api` 스킬로 모델 id 확인(production=`claude-sonnet-4-6`).

## 가장 먼저 할 것

[`handoff-phase6.md`](handoff-phase6.md)를 정독하고, 오케스트레이터의 round-002 발주안(keyword 수·배분·게이트)이 디스크/사람으로 확정됐는지 확인한 뒤, **Phase 0-1(keyword 큐레이션 + dedup)**부터 착수한다. 발주안이 아직이면 그 확정을 기다린다(또는 사람에게 오케스트레이터 세션 결과를 요청).

## 규칙 (CLAUDE.md / 사용자)

- 커밋: Conventional, **scope 없이**, **Co-Authored-By 금지**, 작성자 본인.
- 진행 상태 정본은 디스크(ROADMAP·round 문서). **메모리에 status 쓰지 말 것.**
- 전문가 에이전트엔 해답 박지 말고 문제·제약만.

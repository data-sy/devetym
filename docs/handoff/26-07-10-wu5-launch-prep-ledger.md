# WU-5 완료 — launch-prep 대조·잔여 이관 원장 (2026-07-10)

> dev-etymology `docs/launch-prep/`(6종) + `docs/e2e-checklist.md`를 devetym `docs/release/`와 대조해 **미승계분만** 이관한 결정 기록.
> 계획 정본 = [`26-07-10-selfcontained-migration-plan.md`](26-07-10-selfcontained-migration-plan.md) WU-5. 상태 정본 = [ROADMAP](../../ROADMAP.md).

## 대전제 (이관 방향 결정)
- **dev-etymology = iOS 전용·Firebase Analytics 활성(opt-in 수집)**. **devetym = KMP(양 스토어)·"현재 미수집" 정합**(블로커 #1 해소 2026-07-06). → **개인정보 라벨·수집 전제 콘텐츠는 이관 금지**(옮기면 devetym 결정을 회귀).
- devetym `docs/release/`는 이미 KMP·미수집 기준으로 새로 작성돼 있어 **대부분 승계 완료**. 미승계 = **마케팅 카피 델타**뿐.

## 파일별 원장

| # | 소스 파일 | 승계 상태 | 결정 | 이관 대상/근거 |
|---|---|---|---|---|
| 1 | `appstore-metadata-draft.md` | PARTIAL | **부분 이관** | ASO 카피 옵션·후크형 설명 → `m9-store-metadata-draft.md` 부록 A. 개인정보 라벨부는 **폐기**(Firebase 전제) |
| 2 | `appstore-metadata-draft-reviewed.md` | =1+리뷰 | **부분 이관** | 스크린샷 캡션 5종 → `m9-screenshot-capture-handoff.md` §1b. Kids Category 금지 caution → `m9-store-metadata-draft.md` §4(각색). B1(검색기록 라벨)·B2(지원 URL)는 폐기(미수집 정합/이미 해소) |
| 3 | `appstore-metadata-prompt.md` | NOT | **폐기(선택 보류)** | 메타데이터 생성 라운드테이블 프롬프트. 재사용하려면 KMP·양 스토어·미수집으로 각색 필요. devetym은 이미 신규 초안 보유 → 현재 불필요 |
| 4 | `appstore-metadata-review-prompt.md` | NOT | **폐기(선택 보류)** | #3의 리뷰 짝. 동일 사유 |
| 5 | `appstore-metadata-review-result.md` | PARTIAL | **폐기** | 순수 신규 없음. 블로커(B1/B2)는 devetym 정합·자체 블로커 목록으로 이미 대체 |
| 6 | `launch-consult-prompt.md` | NOT | **폐기** | 출력물(done/blocker/human-gate 분류)이 이미 [`LAUNCH-CHECKLIST.md`](../release/LAUNCH-CHECKLIST.md)로 실체화됨 |
| 7 | `e2e-checklist.md` | PARTIAL | **폐기(선택 fold)** | [`m9-device-smoke-script.md`](../release/m9-device-smoke-script.md)+[`m9-accessibility-audit-script.md`](../release/m9-accessibility-audit-script.md)가 더 엄밀히 대체. 잔여 4개 미세 단언(300ms 디바운스·공유 문자열 포맷·상대시간·빈상태 카피)은 필요 시 스모크에 fold |

## 실이관 (미승계분)
1. **스크린샷 캡션 5종** → `m9-screenshot-capture-handoff.md` §1b(신규). 기존 shot list에 캡션 카피가 없던 갭.
2. **대안 ASO 카피**(이름 A/B·부제 A/B/C·키워드·프로모·후크형 설명 744자) → `m9-store-metadata-draft.md` 부록 A(신규). §1·§3(연령·카테고리·개인정보)은 devetym 정본 우선 명시, 부제/프로모는 App Store 전용 표기.
3. **Kids Category 등록 금지** → `m9-store-metadata-draft.md` §4(각색 — Firebase 충돌 사유 제거, "AI 임의입력 응답이라 아동 카테고리 부적합·14세이상 정합"으로).

## 폐기 대상 (이관 안 함)
- 개인정보/검색기록 라벨 콘텐츠(파일 1·2·5) — devetym 미수집 정합 회귀 방지.
- 파일 5(리뷰 결과)·6(런치 컨설트 프롬프트)·7(e2e 체크리스트) — 상위 대체본 존재.
- 라운드테이블 프롬프트(파일 3·4) — 재사용 시 KMP 각색 조건부, 현재 보류.

## 자기완결성
소스 7종 중 devetym에 **런타임·빌드로 참조되는 것 없음**(전부 문서). 미승계 마케팅 델타 이관 완료 → launch-prep 트랙 자기완결.

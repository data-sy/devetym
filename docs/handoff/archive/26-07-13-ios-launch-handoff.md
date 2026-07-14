# Handoff — iOS 출시 준비 (M9 잔여 · store-submission 트랙)

> **작성**: 2026-07-13 밤 (같은 날 D1~D9 결정·iOS 캡처 완료 반영). **다음 세션 진입점**: [E 제출 핸드오프](26-07-13-ios-submission-handoff.md) — 이 문서는 D 구간 이력 스냅샷으로 보존.
> **상태 정본**: [ROADMAP M9](../../ROADMAP.md) + [iOS 출시 대시보드](../release/ios-launch-dashboard.html)(체크박스 `checked` 속성이 정본). 이 핸드오프는 진입용 스냅샷 — 충돌 시 정본 우선.

> **⚠️ superseded (2026-07-14)**: 이 핸드오프의 목적(D1~D9 결정 세션 진입)은 완료됐다. 현행 진입점 = [E 제출 핸드오프](26-07-13-ios-submission-handoff.md) — 이하는 당시 스냅샷으로 보존(브랜치·스크린샷 서술은 이후 변경됨).

## 1. 현재 위치 (2026-07-13 병합 직후)

- **main = M9 검증 구간.** PR #9(merge-commit) 병합 완료 — 셸 재설계 종결·실기기 사인오프·5축 green까지 전부 main에 있음. `feat/m9-release-verification` 브랜치는 보존(삭제 금지 규율).
- **작업 브랜치 = `feat/m9-store-submission`** (main 위 스택, push됨). 잔여 출시 작업은 전부 여기서.
- 병합 시 `site/privacy-policy.md` 충돌을 m9 정본(§2-2 크래시 진단, 07-10)으로 해소 → Pages 자동 재배포로 **라이브 방침도 최신본**(확인됨).
- M9 DoD(스토어 게시)는 미완 — ~~결정·스크린샷~~(✅ 07-13 완료), 남은 게이트는 **콘솔 입력·빌드/서명·심사(사람/외부)** = E 트랙. 진입점 = [E 제출 핸드오프](26-07-13-ios-submission-handoff.md).

## 2. 다음 작업 (순서)

1. ~~**D1~D9 결정 세션**~~ — ✅ **완료(2026-07-13)**: 9건 전부 확정, [결정 로그](../release/ios-launch-decision-prompt.md#결정-로그) 기록·대시보드 체크 갱신 완료. 요지: D1 경량 캡션 밴드(HTML 캔버스 지그) · D2 한국 단독 · D3 이름 A+부제 A · D4 키워드 95자 · D5 후크형 설명+프로모션(정합 결함 문구 폐기) · D6 무료+수동 게시 · D7 등급 정직 응답(13+ 수용) · D8 프리뷰 스킵 · D9 심사 노트 영어 전문([메타 초안 §5](../release/m9-store-metadata-draft.md)).
2. ~~**D 스크린샷 캡처**~~ — ✅ **iOS 완료(2026-07-13)**: raw 11컷(다크 9·라이트 2, `~/devetym-shots/ios/`) + [캡션 밴드 지그](../release/m9-screenshot-caption-jig.html)로 프레임드 6컷(`framed/`). 상세컷=bug(후크 정합, 사용자 지시). **Android 캡처는 잔여**(별도 세션, 같은 레시피). 시뮬 환경 보존 유지.
3. **E 제출 준비** — **다음 진입점.** 대시보드 섹션 2(메타데이터)·4(빌드·서명)·5(심사·제출) 순. 콘솔 입력값(이름·부제·키워드·설명·심사 노트)과 스크린샷 자산 전부 준비됨 — 남은 건 대부분 `[사람]` 콘솔 작업 + 서명·아카이브([서명 가이드](../release/m9-signing-upload-guide.md)). 제출 아카이브는 **main 기준 빌드**, 게시 시점에 `v0.1.0` 태그(핫픽스 런북 전제).

## 3. 안전선 (요약 — 정본은 CLAUDE.md·ROADMAP)

- **심사 제출·게시·push는 사람 지시로만.** 브랜치 삭제 금지(보존 규율).
- 개인정보·수집 문구는 방침 "현재 미수집" 정합 유지 — ✅ 구 설명안의 "동의한 경우에만 수집" 정합 결함은 **D5에서 폐기 처리 완료**(후크형 설명 채택, 2026-07-13).
- 스토어 정책·요구 사이즈 등 외부 사실은 브리핑 전 웹 최신 확인.
- 완료 항목은 대시보드 HTML의 `checked` 갱신 + 커밋(브라우저 체크는 localStorage 임시).

## 4. 파일 지도

| 파일 | 역할 |
|---|---|
| [ios-launch-dashboard.html](../release/ios-launch-dashboard.html) | 실행 체크리스트 정본 (7섹션 38항목·소유자 태그·진행률) |
| [ios-launch-decision-prompt.md](../release/ios-launch-decision-prompt.md) | D1~D9 결정 세션 프롬프트 + 결정 로그 |
| [m9-screenshot-capture-handoff.md](../release/m9-screenshot-capture-handoff.md) | 스크린샷 캡처 레시피 (D1 확정 후) |
| [m9-store-metadata-draft.md](../release/m9-store-metadata-draft.md) | 이름·설명·키워드·라벨 초안 (D3~D5·D7 입력물) |
| [m9-signing-upload-guide.md](../release/m9-signing-upload-guide.md) | 서명·아카이브·업로드 절차 |
| [LAUNCH-CHECKLIST.md](../release/LAUNCH-CHECKLIST.md) | 6카테고리 배경·근거 (양 플랫폼) |

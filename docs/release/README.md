# 출시 자료는 이 repo 밖에 있다 (2026-07-26 이관)

`docs/release/`에 있던 출시 실무 자료 전부를 **로컬 `~/Downloads/devetym-release/`로 옮겼다.**
이유: 씨딩 글 초안·채널 전략 등 **공개 repo에 노출되면 곤란한 자료**가 섞여 있어서.

- **상태 정본은 [`ROADMAP.md`](../../ROADMAP.md) M9** — 이관 후에도 그대로다(게시 일정·잔여 결정 포함).
- 이 폴더를 가리키던 문서의 링크는 전부 이 README로 접었다. 실제 파일은 위 로컬 경로에서 열 것.
- **과거 이력은 이 repo에 남아 있다**: `git show HEAD:docs/release/<파일명>` (예: `git log -- docs/release/`).

## 옮긴 파일 (이름도 정리함)

| 이관 후 이름 | 옛 이름 | 역할 |
|---|---|---|
| `🤖-출시-그로스-플랜.md` | (repo 밖 신규) | 씨딩 실행 정본 — 채널별 글 초안·스테거링·리뷰 확보 |
| `hero-term-candidates.md` | 동일 | 히어로 용어 선정 정본(스토어=canary / 씨딩=happy-eyeballs) |
| `seeding-channel-comparison.md` | (repo 밖 신규) | 씨딩 채널 선택 근거 |
| `LAUNCH-CHECKLIST.md` | 동일 | 6카테고리 마스터 뷰(ROADMAP 종속) |
| `ios-launch-dashboard.html` | 동일 | iOS 게시 체크박스 실행판 |
| `🤖-launch-decision-prompt.md` | `ios-launch-decision-prompt.md` | D1~D9 결정 로그 |
| `store-metadata.md` | `m9-store-metadata-draft.md` | 스토어 필드·개인정보 라벨 정본 |
| `signing-upload-guide.md` | `m9-signing-upload-guide.md` | 서명·업로드 |
| `build-preflight-checklist.md` | `m9-build-preflight-checklist.md` | 빌드 전 점검 |
| `screenshot-capture.md` | `m9-screenshot-capture-handoff.md` | 스샷 캡처 레시피 |
| `screenshot-caption-jig.html` | `m9-screenshot-caption-jig.html` | 캡션 프레이밍 지그 |
| `🤖-screenshot-fresh-eyes-prompt.md` | `m9-screenshot-fresh-eyes-prompt.md` | 스샷 백지 검수 프롬프트 |
| `icon-render-sheet.html` | `m9-icon-render-sheet.html` | 아이콘 렌더 시트 |
| `accessibility-audit-script.md` | `m9-accessibility-audit-script.md` | 접근성 감사 대본 |
| `device-smoke-script.md` | `m9-device-smoke-script.md` | 실기기 스모크 대본 |
| `🤖-store-metadata-prompt.md` · `🤖-store-metadata-review-prompt.md` | `~/dev-etymology/docs/launch-prep/appstore-metadata-*.md` | 메타데이터 생성·리뷰 라운드테이블(다음 대상 = Play Console) |

**삭제한 것(1회성 검증 결과 — 결론은 ROADMAP·대본에 흡수됨, 원본은 git 이력)**:
`m9-ios-sim-smoke-report.md` · `m9-screenshot-fresh-eyes-result.md` · `m9-shell-redesign-device-checklist.md` · `m9-verification-teardown-ledger.md`

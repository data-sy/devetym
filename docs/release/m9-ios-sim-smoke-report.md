# M9 · iOS 시뮬레이터 스모크 리포트 (Tier 1 완주)

> **2026-07-13 · iPhone 16 시뮬레이터 (iOS 18.5) · AI 자율 주행.**
> [스모크 대본](m9-device-smoke-script.md) Tier 1의 "입력 주입 필요(탭/타이핑)" 잔여분을 전부 주행해 닫았다.
> 입력 주입 수단: cliclick은 mouse-up 이벤트 간헐 유실로 폐기 → **CGEvent 기반 자작 Swift tap/swipe 도구**
> (move→settle→down→hold→up 정석 시퀀스, 세션 스크래치패드에서 컴파일)로 신뢰 확보. 타이핑은 하드웨어
> 키보드 패스스루(System Events keystroke), 검증은 매 스텝 `simctl io screenshot` 육안 대조.

## 1. 이번 주행에서 새로 배선한 것 — 아이콘·런치스크린 (WU-6 NEEDS-MIGRATION 해소)

원본 `~/dev-etymology` Assets.xcassets에서 이관 + 배선 (이 커밋에 포함):

| 항목 | 내용 | 검증 |
|---|---|---|
| `AppIcon.appiconset` | 단일 1024 라이트(`icon.png`)+다크(`icon-dark.png`) | 홈스크린(2페이지) 라이트/다크 렌더 육안 ✅ |
| `LaunchLogo.imageset` | "개발어원 사전" 로고 @2x/@3x | 런치스크린 실렌더 ✅ |
| `Theme/brand.colorset` | `#2E5D3A` (Android adaptive 배경과 동일) | UILaunchScreen 배경 ✅ |
| `Info.plist` UILaunchScreen | 원본과 동일 dict(UIColorName=Theme/brand·UIImageName=LaunchLogo) | 그린 배경+로고 첫 프레임 캡처 ✅ |
| `project.yml` | `ASSETCATALOG_COMPILER_APPICON_NAME: AppIcon` | xcodebuild SUCCEEDED·Assets.car 구움 ✅ |

## 2. 주행 결과 매트릭스 (전부 PASS)

| # | 대본 항목 | 관찰 |
|---|---|---|
| 1 | 검색 타이핑 — 번들 히트 | `mutex` 타이핑→실시간 제안→Return→상세(배지·정의·어원·왜 이 이름인가) |
| 2 | 검색 — alias | `Arne Andersson tree` → **aa-tree** 상세 매핑 |
| 3 | 검색 — 미스→AI | `zzqxv` → 로딩(PulsingDots·"어원을 찾고 있어요") → **실 프록시 왕복** → NotDevTerm("개발 용어를 검색해주세요") |
| 4 | 상세→북마크 토글→탭 반영→재기동 유지 | ★↔☆ 양방향 반응형, 북마크 탭 반영, terminate→launch 후 잔존(실 디스크 SQLite) |
| 5 | 히스토리 탭·개별 삭제 | 실검색 항목 자동 추가 + X 삭제 즉시 반영 |
| 6 | 히스토리 타임스탬프 | 실검색 = "1분 전"/"9분 전" 정확. 〔이전 세션 주입분의 "20647일 전"은 **주입 데이터의 초/밀리초 단위 실수 — 앱 코드 무죄** 판정〕 |
| 7 | 설정 외관 토글 실조작 | 라이트↔다크 즉시 실렌더 전환 (시스템 모드는 2026-07-05 주입 확인분) |
| 8 | 라이선스 실스크롤 | OFL 전문 실로드(Res.readBytes)+스크롤+`← 뒤로` 동작 |
| 9 | 아이콘 스프링보드 렌더 | 홈 2페이지, 라이트/다크 어피어런스 모두 선명 |
| 10 | 클립보드 seam (보너스) | 상세 "어원 복사" → `simctl pbpaste`로 어원 전문 회수 — **iOS 클립보드 actual 실동작** (Android dead-seam과 대조) |

## 3. 🐛 발견 결함 — iOS Found 상세 탈출 불가 (✅ 2026-07-13 수정·시뮬 재검증 완료)

> **해소**: `DetailContent` 최상단 상시 "← 뒤로"(Found·Loading 포함 전 상태 — Loading도 탈출구가 없었다)
> + `AppRoot` 활성 탭 재탭 시 루트 pop(iOS 탭바 관례). 5축 green + iPhone 16 Pro 시뮬 실주행으로
> mutex Found 상세 → "← 뒤로" 복귀·재진입 → 활성 탭 재탭 복귀 모두 스크린샷 대조 확인. 이하는 발견 당시 원기록.

**정상(Found) 상세에 진입하면 그 탭에서 빠져나올 방법이 없다.** 앱 재시작만이 탈출구.

- 근거 3중: ① `DetailScreen.kt` onBack UI가 NotDevTerm("검색으로 돌아가기")·PossibleTypo("아니요, 돌아가기")·Error("돌아가기")에만 있고 **Found 상태엔 없음** ② iOS는 Android 시스템 백 제스처가 없음(자체 상태기반 네비 — 엣지 스와이프 미배선 실측) ③ `AppRoot.kt` 탭바 `onClick { tab = t }` — **활성 탭 재탭은 pop 없이 no-op**, 탭 전환해도 `detailKeys[tab]` 보존이라 복귀 시 상세 그대로
- 시뮬 실증: aa-tree 상세→엣지 스와이프 무효→타 탭 전환 후 검색 탭 복귀→여전히 상세
- Android는 시스템 백 있어 비차단. **iOS는 심사 리젝급 UX — 출시 전 수정 필수**
- 수정 후보: Found에 back 어포던스 추가, 또는 활성 탭 재탭 시 루트 pop (방향은 다음 세션에서 결정)

## 4. 마이너 노트 (비차단)

- **라이선스 화면 safe area 겹침** — `← 뒤로`·본문이 상태바(시계)와 겹침. 폴리시 수준
- **설정 "사용 데이터 수집 동의" 미영속** — `AppRoot.kt` `rememberSaveable` 메모리 전용(기본 true). WU-6 잔여 "온보딩 동의 거버넌스 얽힘"으로 이미 원장 등재된 알려진 갭
- 시뮬 자동화 로어: Simulator는 순간이동 클릭을 씹는다 — hover 선행+down/up 분리 필수. `Cmd+Shift+H` 키보드 홈은 미동작, Device>Home 메뉴 AX 클릭은 동작

## 5. 남은 것 (이 리포트로 대체되지 않음)

> **〔2026-07-13 추기 — 대부분 해소〕** 아래 항목 중 메일·클립보드·공유·앱평가·VoiceOver는 같은 날 **셸 재설계
> 스텝 1~3 + 실기기 라운드 1·2 사인오프**로 종결(공유 no-op·앱평가 죽은 URL은 실구현으로 대체). 정본 =
> [셸 재설계 체크리스트](m9-shell-redesign-device-checklist.md)·ROADMAP. 잔여 = TalkBack·Dynamic Type·실 DSN(선택/후속).

- **[사람] Tier 2 실기기**: 메일 실전송·앱간 클립보드 체감·실 DPI 아이콘·햅틱·TalkBack/VoiceOver 실감사([접근성 대본](m9-accessibility-audit-script.md)) — VoiceOver/Accessibility Inspector는 시뮬에서도 사람 상호작용 필요라 미주행
- **[사람] Sentry 실 DSN 런타임 도달** (WU-4B 잔여 — 실기기 스모크 시)
- iOS 공유 시트 no-op(알려진 백로그)·메일/앱평가는 시뮬 한계로 Tier 2 귀속

## 6. M9-후속 UX 3건 구현·재주행 (2026-07-13 오후, iPhone 16 Pro)

실기기 피드백 UX 3건(ROADMAP Done 참조) 구현 후 동일 CGEvent 도구로 재주행. 각 건 5축 green + 스크린샷 대조 PASS.

| # | 항목 | 관찰 |
|---|---|---|
| 1 | UX-1 톤 알약 렌더 (다크) | ❏ 복사·☆/★ 북마크·↗ 공유 accent 틴트 + 라임 전경, 오류 제보 회색 분리 — 글리프 tofu 없음 |
| 2 | UX-1 톤 알약 렌더 (라이트) | 페일 그린 틴트 + brand 딥그린 전경 (AA — `tonalActionColor` 분기 근거) |
| 3 | UX-1 기능 | 복사→`simctl pbpaste` 어원 전문 회수 · 북마크 ☆→★ 반응형 토글 |
| 4 | UX-3 로딩 문구 교차 | AI 미스 로딩 t<3s "AI가 어원을 찾고 있어요" → t≈4.6s "잠시만 기다려 주세요" (3초 교차 실측) |
| 5 | UX-2 뎁스0 페이저 | 4탭 좌우 스와이프 양방향 + 탭바 클릭 점프(히스토리→검색 2페이지) 전환·하이라이트 동기 |
| 6 | UX-2 제스처 충돌 | 상세 표시 중 본문 스와이프 무효(페이저 비활성·back 미발화) |
| 7 | UX-2 엣지 스와이프-백 | 왼쪽 엣지 시작 드래그 → 탭 루트 복귀. 기존 탈출구(상시 "← 뒤로"·활성 탭 재탭 pop) 회귀 0 |

🐛 **주행이 잡은 구현 결함 1건(수정 완료)**: 초판 엣지 스와이프-백이 `detectHorizontalDragGestures` 기반이라 **onDragStart가 터치 슬롭 통과 지점을 반환** → 엣지(24dp) 판정이 슬롭만큼 밀려 back 미발화. `awaitFirstDown` 실제 다운 지점 판정으로 교체 후 PASS — 시뮬 실주행 없이는 5축 green이 못 잡는 부류(제스처 런타임 의미론).

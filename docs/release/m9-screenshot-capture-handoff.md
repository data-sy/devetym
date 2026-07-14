# Handoff — Store Screenshot Capture (`[AI→H]`)

> **Why this is a separate session.** Capturing store screenshots means booting a live iOS simulator + Android emulator, installing the app, driving it to specific screens, injecting deterministic state, and capturing at each store's required device sizes — a long, environment-heavy task best done as its own focused run. This doc is the full recipe so a fresh session (or a person) can execute it end-to-end.
>
> **Blocks:** [LAUNCH-CHECKLIST](LAUNCH-CHECKLIST.md) §3 "Screenshots" (Blocker #4) and store-metadata submission.
> **Leans on:** [m9-device-smoke-script](m9-device-smoke-script.md) (boot/install/drive recipes already proven), [android-studio-cmp-runbook](../../android-studio-cmp-runbook.md). Verification env (AVD + iOS sim, ~8.2G) is **preserved** per the [teardown ledger](m9-verification-teardown-ledger.md) — reuse it, don't rebuild.

## 0. Goal

Produce a clean, consistent set of marketing screenshots for **both** stores, at each store's required device sizes, in **dark mode** (the app's default and strongest look) with a couple of light-mode alternates. Deterministic content (no half-typed text, no error states) unless the screen is meant to show that feature.

## 1. Screens to capture (the shot list)

Same order as the smoke script's flow. Aim for 5–8 shots (both stores cap at 8 for phone / 10 for iOS).

| # | Screen | State to inject / reach | Sells |
|---|---|---|---|
| 1 | Onboarding (intro) | Fresh install, first launch | What the app is |
| 2 | Search results — bundle hit | Search `mutex` (or `daemon`) | Instant offline dictionary |
| 3 | Term detail | Open a term with a rich etymology (e.g. `daemon`, `kernel`) | The core value: *why* the name |
| 4 | AI-generated result | Search a term **not** in bundle (e.g. `debounce` — ⚠️ 구 예시 `quicksort`는 이후 번들에 수록돼 폐기, 07-14 D9 추기와 동일) → AI fills it | AI fallback |
| 5 | Bookmarks | A few bookmarked terms | Personal library |
| 6 | Search history | A few recent searches | Convenience |
| 7 | Appearance / settings | Settings screen showing light/dark/system | Polish |
| 8 | Licenses (optional) | Licenses screen | Trust / OSS |

**Deterministic state injection** (avoid typing on camera): the smoke script already documents injecting SQLite state via `simctl` container `sqlite3` (iOS) and `adb` for Android. Pre-seed bookmarks/history rather than performing them live, so the screens are clean. For the AI-generated shot, either use a real proxy round-trip (network) or pre-seed the cached result row.

### 1b. 캡션 카피 (dev-etymology 초안 계승 — WU-5)

프레이밍(§4)에서 캡션을 얹을 때 쓸 한 줄 카피. 원본은 iOS App Store 초안이나 문구는 양 스토어 공통. **과장 금지**(스토어가 misleading 스크린샷 리젝) — 아래는 사실 기반.

2026-07-14 페르소나 라운드테이블([ASO]·[카피]·[사용자]·[심사]) + 사용자 검토로 전면 개정. 결정 근거:
- **앞 3장 = 광고 지면**(검색 결과 노출)이므로 "다운로드 이유"가 되는 주장만 배치: 후크 → AI 커버리지 → 타깃 유스케이스.
- **"650개" 숫자 캡션 폐기** — 숫자가 비인상적·갱신 부채·타깃에 역효과. 숫자는 description 본문 몫.
- **"인터넷 없어도"(오프라인 각) 폐기** — 한국 타깃에게 오프라인 장면은 약함. 속도는 기대치(table stakes)지 다운로드 이유가 아니므로 4번으로 강등.
- **"이름의 이유" 컷 삭제** — 후크 컷과 메시지·화면 중복(6컷→5컷).
- **캡션 자립 원칙**(QLT-App 계승): 각 캡션은 앞뒤 컷 없이 혼자 완결되게. "없는 용어는~" 같은 앞 컷 의존 지시어 금지.

2026-07-14 **2차 개정(D3 카피)** — 라운드테이블 2차 + 프레시아이 백지 설계([결과](m9-screenshot-fresh-eyes-result.md), 격리 세션 — 실행용 프롬프트는 사용 후 폐기) 대조 + 사용자 확정. 결정 근거:
- **"치는 순간, 결과가 바로" 컷 삭제(5컷→4컷)** — AI 경로의 실제 대기(로딩) 상태와 세트 차원의 자기모순·기대치 배신. 라운드테이블과 프레시아이가 **독립적으로 같은 결론**(속도 컷 불채택)에 도달해 확정.
- **f1 개정** — 구 "왜 버그일까? 어원부터 알려줘요"는 질문 직후 기능 설명으로 새는 데다 전제("버그=벌레"라는 어원 사실)가 생략됨. "에러를 왜 버그라고 부를까" 류는 표준 용어상 부정확(error=사람의 실수, bug=코드 결함, failure=드러난 장애 — IEEE 610 계보; 앱 내 정의 "프로그램의 결함"과도 어긋남)해 배제. 확정안은 용어 자체를 주어로 삼아 정확성 논쟁을 우회.
- **f2 조건절 제거(프레시아이 R1 이식)** — "모르는 용어도/사전에 없는 용어는" 구분은 소비자에게 무의미(내장 콘텐츠도 동일 프롬프트로 AI 사전 생성). "바로" 삭제로 즉답 뉘앙스 해소, 주장은 "어떤 용어든"으로 오히려 커짐.
- f3(면접·복습)·라이트 컷은 유지 — 프레시아이도 동일 슬롯 채택(독립 수렴).
- **효용 컷 추가(4→5컷, 같은 날 추기)** — "왜 어원인가"(철자 암기→망각, 이름의 이유→장기 기억) 주장이 프로모션 텍스트·전체 설명 리드에는 있으나 **검색 노출면(스크린샷 1~3컷)에는 부재**. f1 2행 병합안 대신 전용 컷(사용자 선택). 화면은 daemon 상세 — 수호령 비유 자체가 "이야기는 기억에 남는다"의 실물 증거.

| 컷 | 화면 (raw) | 캡션 | 판다 메시지 |
|---|---|---|---|
| f1 | 상세 다크 `bug` (`03-term-detail.png`) | **"버그는 왜 하필 *벌레*일까?"** | 후크 — 전제(버그=벌레) 내장 + 화면이 답변 |
| f2 | 상세 다크 `daemon` (`03b-term-detail-daemon.png`) | **"이름의 *이유*를 알면, 기억에 오래 남아요"** | 효용 — 왜 어원인가(설명 리드와 정합). "기억에"로 남는 대상 명시("기억이"는 관용 결 어긋나 기각) |
| f3 | AI 생성 결과 (`04-ai-generated.png`) | **"어떤 용어든 물어보세요 — *AI*가 어원까지 설명해줘요"** | 커버리지 보증, 막다른 골목 없음. "설명해줘요"로 청유("물어보세요")-응답 호흡 |
| f4 | 북마크 (`05-bookmarks.png`) | **"*면접·복습*용으로 모아두기"** | 타깃 유스케이스·재방문 가치 |
| f5 | 상세 라이트 `bug` (`10-detail-light.png`) | **"라이트/다크, 눈이 편한 쪽으로"** | 폴리시(외관 3모드 — 사실 기반) + 시각 환기 |

노출권(1~3컷) = 후크 → 효용 → AI. 다크 4 + 라이트 1로 마감. (구 D2 5컷 표는 git 히스토리 참조.)

## 2. iOS — required sizes & capture

Apple currently requires **one 6.9" (or 6.7") iPhone** set; it can auto-scale down to smaller iPhones. Provide iPad shots **only if the app supports iPad** (confirm target — this app is iPhone-first).

- **6.9" iPhone** (e.g. iPhone 16 Pro Max): 1320 × 2868 px
- **6.7" iPhone** (e.g. iPhone 15/16 Plus): 1290 × 2796 px — acceptable primary if 6.9" unavailable
- (Optional) **6.5"**: 1284 × 2778 px

> ⚠️ Store specs change — **verify current required sizes in App Store Connect at capture time** before finalizing.

Capture:

```bash
# list / boot the sim (iPhone 16 family already used in smoke)
xcrun simctl list devices | grep -i "iPhone 16"
xcrun simctl boot "iPhone 16 Pro Max"      # or the 6.9" device you have
# install the app build (see smoke script for the build/install step), drive to each screen, then:
xcrun simctl io "iPhone 16 Pro Max" screenshot ~/devetym-shots/ios/01-onboarding.png
# repeat per screen. Set dark mode:
xcrun simctl ui "iPhone 16 Pro Max" appearance dark
```

The native status bar can be cleaned up with `xcrun simctl status_bar <udid> override --time "9:41" --batteryState charged --batteryLevel 100 --cellularBars 4 --wifiBars 3`.

## 3. Android — required sizes & capture

Play Console requires **2–8 phone screenshots**, 16:9 or 9:16, each side 320–3840 px, PNG (24-bit, no alpha) or JPEG. Plus a **feature graphic 1024 × 500** and a **512 × 512 hi-res icon** (separate assets — see §5).

- Capture at the emulator's native resolution (the API 36 AVD from smoke is fine; a 1080 × 2400 phone profile gives a clean 9:16).

```bash
adb devices
adb shell settings put secure ui_night_mode 2     # force dark (2=dark, 1=light)
# drive to each screen (adb input tap/text per smoke script), then:
adb exec-out screencap -p > ~/devetym-shots/android/01-onboarding.png
# repeat per screen
```

## 4. Framing / polish (optional but recommended)

Raw device screenshots are acceptable. If you want marketing frames (device bezel + caption), do it as a **post step** on the raw PNGs — keep the raw captures too. Don't add text that overstates features (store review rejects misleading screenshots). Keep captions honest: "오프라인 사전 650+", "AI가 채우는 어원", etc.

## 5. Separate assets still needed (not screenshots)

- **Play feature graphic** 1024 × 500 (required for Play listing).
- **Play hi-res icon** 512 × 512 (derive from the adaptive icon; render sheet: [m9-icon-render-sheet.html](m9-icon-render-sheet.html)).
- **App preview video** — optional, skip for v1.

## 6. Output location & handback

- Save raw captures under a scratch/output dir (not committed unless you want them in-repo): `~/devetym-shots/{ios,android}/NN-name.png`.
- If delivering, the user's convention is the iCloud outputs folder (`claude-code-outputs/<project>/`) — copy there **only on explicit request**.
- When done: update [LAUNCH-CHECKLIST](LAUNCH-CHECKLIST.md) §3 "Screenshots" ⬜ → ✅/🟡 and note where the assets live.

## 7. Definition of done

- [x] iOS 6.9"/6.7" set (5–8 shots), dark primary + a light alternate or two, clean status bar. — **✅ 2026-07-13**: iPhone 16 Pro Max sim, 1320×2868, dark 9 + light 2 (`~/devetym-shots/ios/`), detail cut = `bug` (설명 후크와 정합).
- [x] Framing — **D2 프레임 + D3 카피 ✅ 2026-07-14** (D1 다크 캡션 밴드는 다크 앱과 경계 불가 판정·폐기, QLT-App 스샷 방식 계승): 밝은 라임 틴트 배경 + 검은 디바이스 카드(하단 크롭) via [caption jig](m9-screenshot-caption-jig.html). App Store Connect 슬롯별 **5컷** × 2사이즈 (§1b D3 개정: 속도 컷 삭제·f1/f3 문구 확정·효용 컷 f2 추가) — **6.9"** `~/devetym-shots/ios/framed-6.9/` (1320×2868), **6.5"** `~/devetym-shots/ios/framed-6.5/` (1242×2688). 구 `framed/`는 superseded(참고용 보존). 재생성 스크립트 패턴은 지그 파일 헤더 주석 참조.
- [ ] Android phone set (2–8 shots), 9:16, dark primary. — 별도 세션 잔여.
- [ ] Play feature graphic 1024×500 + hi-res icon 512×512. — Android 트랙 잔여.
- [x] Store specs re-verified against current console requirements at capture time. — 2026-07-13 웹 확인: 6.9" 필수(1320×2868 허용), 1~10장, iPad 미지원 시 불필요.
- [x] LAUNCH-CHECKLIST updated. (iOS ✅ / Android 🟡)

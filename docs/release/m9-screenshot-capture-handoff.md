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
| 4 | AI-generated result | Search a term **not** in bundle (e.g. `quicksort`) → AI fills it | AI fallback |
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

| 컷 | 화면 | 캡션 | 판다 메시지 |
|---|---|---|---|
| f1 | 상세(예: `bug`) | **"왜 *버그*일까? 어원부터 알려줘요"** | 후크·핵심 차별점 |
| f2 | AI 생성 결과 | **"모르는 용어도 *AI*가 바로 답해줘요"** | 커버리지 보증 (구: "없는 용어는 AI가 그 자리에서" — 자립형으로 교체) |
| f3 | 북마크 | **"면접·복습용으로 모아두기"** | 타깃 유스케이스 |
| f4 | 자동완성/검색창 | **"치는 순간, 결과가 *바로*"** | 즉답·사전다움 (구: 650개→인터넷 없어도 — 순차 폐기) |
| f5 | 상세(라이트) | **"라이트/다크, 눈이 편한 쪽으로"** | 폴리시(외관 3모드 — 사실 기반) |

다크 기본 + 라이트 1장 섞으면 완성도 인상↑. f4는 빼고 4컷으로 가도 무방(사람 판단).

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
- [x] Framing — **D2 ✅ 2026-07-14** (D1 다크 캡션 밴드는 다크 앱과 경계 불가 판정·폐기, QLT-App 스샷 방식 계승): 밝은 라임 틴트 배경 + 검은 디바이스 카드(하단 크롭) via [caption jig](m9-screenshot-caption-jig.html). App Store Connect 슬롯별 **5컷** × 2사이즈 (§1b 개정: 650 캡션 교체·f4 삭제) — **6.9"** `~/devetym-shots/ios/framed-6.9/` (1320×2868), **6.5"** `~/devetym-shots/ios/framed-6.5/` (1242×2688). 구 `framed/`는 superseded(참고용 보존). 재생성 스크립트 패턴은 지그 파일 헤더 주석 참조.
- [ ] Android phone set (2–8 shots), 9:16, dark primary. — 별도 세션 잔여.
- [ ] Play feature graphic 1024×500 + hi-res icon 512×512. — Android 트랙 잔여.
- [x] Store specs re-verified against current console requirements at capture time. — 2026-07-13 웹 확인: 6.9" 필수(1320×2868 허용), 1~10장, iPad 미지원 시 불필요.
- [x] LAUNCH-CHECKLIST updated. (iOS ✅ / Android 🟡)

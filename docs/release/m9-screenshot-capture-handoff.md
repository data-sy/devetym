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

- [ ] iOS 6.9"/6.7" set (5–8 shots), dark primary + a light alternate or two, clean status bar.
- [ ] Android phone set (2–8 shots), 9:16, dark primary.
- [ ] Play feature graphic 1024×500 + hi-res icon 512×512.
- [ ] Store specs re-verified against current console requirements at capture time.
- [ ] LAUNCH-CHECKLIST updated.

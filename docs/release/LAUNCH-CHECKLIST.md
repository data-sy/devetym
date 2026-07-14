# DevEtym — Launch Checklist (iOS + Android)

> **What this is.** The consolidated pre-launch checklist called for by the M9 slice (§3 point 3, "정직한 핸드오프 — 최종 체크리스트를 산출"). It maps a generic 6-category launch framework onto DevEtym's **actual, verified state** and points at where each item's real work/evidence lives.
>
> **What this is NOT.** The milestone status source of truth. That stays in [`../../ROADMAP.md`](../../ROADMAP.md) (M9). When a status here disagrees with ROADMAP, **ROADMAP wins** and this file is the stale one — fix it here.
>
> **App:** 개발 어원 사전 (DevEtym) · KMP/Compose Multiplatform, single codebase for **Android + iOS**.
> **IDs:** appId/bundleId `com.oddmuffin.devetym` · Android 8.0+ (API 26) · iOS 16+ · `versionName=0.1.0`, `versionCode=1`.
> **Last synced to repo state:** 2026-07-14.
>
> **🧭 Launch sequence (decided 2026-07-13).** Critical path **A → (B·C·D parallel) → E·F**: **A** flip repo public (secret sweep first, irreversible; upstream of everything) → **B** deploy Pages (policy URL, WU-1) · **C** real-device smoke + a11y audit (WU-11) · **D** capture screenshots → **E iOS store submit FIRST** (dev account paid, prior launch experience, no cohort gate → review直行) · **F Android LATER** (closed-testing gate: 20 testers × 14 days). **iOS and Android ship as separate todos** (WU-12a / WU-12b) — different store gates. Source of truth: ROADMAP M9 "출시 시퀀스 확정". **Current (2026-07-14): A·B·C·D done → E (iOS submit) in progress** — ASC app record created + screenshots uploaded, final pre-submit cross-check `[AI]` passed (review-note example fixed: quicksort→debounce, D9 addendum); remaining = review-note paste (s2) → re-archive/re-upload (b6) → submit (s4, human-only). F (Android) trails. Execution canon: [submission handoff](../handoff/26-07-13-ios-submission-handoff.md) §1.

## Legend

**Status:** ✅ done · 🟡 partial / jig ready, confirm or follow-up pending · ⬜ not started · 🚫 blocker (needs a decision before submit) · ➖ N/A for this app

**Owner** (M9 taxonomy — see [m9 slice](../specs/m9-release-verification-draft.md) §tag-legend):
- `[AI]` — closed by Claude/CI automatically (tests, scripts, static checks).
- `[AI→H]` — Claude produces a confirmation jig; a human observes/confirms (minutes).
- `[H]` — irreducible human gate (real device, secret keys, store review). **Awaits human instruction — no autonomous execution.**
- `[Defer]` — parallel/follow-up track, does not block release.

---

## ⚠️ Blockers & Decisions (resolve before store submit)

These are the items that can actually stop or misrepresent the launch. Everything else is mechanical.

1. ✅ **Privacy policy ↔ implementation mismatch — RESOLVED (2026-07-06).** Decision: reconcile to "**does not currently collect**" (Firebase-later). [`site/privacy-policy.md`](../../site/privacy-policy.md) rewritten: no analytics collected; on-device-only data; the only outbound data is the search keyword + a random rate-limit device id sent for the AI-fallback feature (honestly disclosed). Store labels updated to match ([store-metadata §3–4](m9-store-metadata-draft.md)). ⚠️ Still recommend legal review before external release.
2. ✅ **Privacy policy deployed → live public URL — RESOLVED (2026-07-13).** Repo flipped **public** (secret sweep clean) → PR #10 merged → GitHub Pages enabled (Actions source) → policy/ToS live (all 200): <https://data-sy.github.io/devetym/privacy-policy>, <https://data-sy.github.io/devetym/terms-of-service>, index <https://data-sy.github.io/devetym/>. URL reflected in [store-metadata §1](m9-store-metadata-draft.md). ✅ in-app policy link aligned (2026-07-13 shell-redesign step 1 — `Constants.privacyPolicyUrl`, stale `devetym.app/privacy` literal replaced).
3. ✅ **Terms of Service — drafted (2026-07-06).** [`site/terms-of-service.md`](../../site/terms-of-service.md), incl. AI-generated-content disclaimer + acceptable-use (rate limits). ⚠️ Legal review recommended before publish.
4. 🟡 **Store screenshots — iOS DONE (captured 2026-07-13, framed 5-cut × 2 sizes + ASC upload 2026-07-14); Android not captured.** Recipe: [m9-screenshot-capture-handoff](m9-screenshot-capture-handoff.md). Blocks only F (Android listing), not E.
5. 🟡 **Signing — iOS proven (2026-07-14: automatic signing `DEVELOPMENT_TEAM` in project.yml, Release archive reached ASC validation on 1st upload attempt); Android not wired.** `androidApp/build.gradle.kts` release buildType has no `signingConfig`; keystore creation is `[H]` (secret key). Guide: [signing-upload-guide](m9-signing-upload-guide.md). Blocks only F.
6. 🟡 **Developer accounts — Apple confirmed (2026-07-13), Google unknown.** Apple Developer Program **enrolled + billed** (user has prior iOS launch experience) → iOS submit unblocked on this axis. Google Play Console enrollment / billing status still unknown in-repo (needed for F/WU-12b).
7. 🟢 **Crash reporting added & unified (WU-4 → WU-4B, 2026-07-10).** Sentry now wired as a **single commonMain KMP** (`sentry-kotlin-multiplatform` 0.27.0) — the WU-4 platform-split seam was superseded by WU-4B. **Android = real** (via KMP's transitive `sentry-android`, uncaught-handler, CI-green). **iOS = real** — Kotlin `CrashReporter` initializes Sentry (iosMain no-op removed; `doInitKoin` reads Info.plist `SentryDsn`); Sentry Cocoa static xcframework linked into the app (project.yml) with **Xcode simulator build verified SUCCEEDED**. The old WU-11 SPM/Swift activation path is obsolete. Privacy policy §2-2 + store labels updated. Analytics still deferred (Blocker #1 → Firebase-later). **Blocker #7: fully closed (2026-07-14, PR #14)** — real DSN issued (root `.env`, build-time codegen injection replacing the Info.plist/BuildConfig paths), symbol upload wired (Android mapping · iOS dSYM), and **real crash delivery verified on both platforms** via a temporary crash button (removed after verification). Release-build re-check remains at the TestFlight gate (dashboard b7). See [WU-4 ledger §5–6](../handoff/26-07-10-wu4-crash-reporting-ledger.md).

---

## 1. Functionality & QA

- ✅ `[AI]` **Core flows exercised on 5-axis green.** unit (`:shared:testDebugUnitTest` 121) · native (`:shared:iosSimulatorArm64Test` 111) · link (`linkDebugFrameworkIosSimulatorArm64`) · assemble (`:androidApp:assembleDebug`) · guard (`:androidApp:testDebugUnitTest`). Real Koin graph completeness + native SQLite roundtrip pulled down from human gates. Evidence: ROADMAP M9 `[AI]` track.
- ✅ `[H]` **Multiple devices / screen sizes / OS versions.** iPhone 16 sim + Android API 36 emulator (Tier 1, full flow) **+ real-device iPhone 13 mini smoke signed off (2026-07-13, shell-redesign rounds 1–2 — [checklist](m9-shell-redesign-device-checklist.md))**. Optional residue: real-DPI icon recheck. Script: [m9-device-smoke-script](m9-device-smoke-script.md).
- ✅ `[AI]` **Network-unstable / offline handling.** Offline-first: bundle dictionary (650+ terms) serves without network; AI fallback on miss. Emulator saw AI miss→proxy generation + DNS-flake recovery. Evidence: ROADMAP Android smoke.
- 🟡 `[AI→H]` **Crash / error handling.** Two real first-launch crashes already caught by device smoke and fixed (iOS `-lsqlite3` link; Android manifest class path). Error paths logged via `AnalyticsService.logError` — but see §4 (no live backend). Crash reporting: Sentry wired on both platforms (WU-4B); real-DSN delivery verified 2026-07-14 (PR #14) — Blocker #7 closed.
- 🟡 `[AI]` **Memory leaks / performance (load, battery).** No profiling done. VM lifecycle (ViewModelStore) is a `[Defer]` track. Load feels instant (bundle-local). Battery/leak profiling not performed → open.
- ✅ `[AI]` / 🟡 `[H]` **Dark mode / accessibility.** WCAG contrast: 36 pairs, **all pass AA**. contentDescription coverage scanned. 3 appearance modes (light/dark/system) verified on emulator. **Remaining `[H]`:** real TalkBack/VoiceOver gestures + Dynamic Type. Script: [m9-accessibility-audit-script](m9-accessibility-audit-script.md).
- ➖ **Payments / IAP.** None in app. N/A.
- ✅ `[AI]` **Known gaps — both RESOLVED (2026-07-10, WU-8/WU-9):** (a) clipboard seam wired to Detail copy action (later upgraded to full-payload copy, shell-redesign §2-E); (b) Android splash wired (`core-splashscreen`, brand `#2E5D3A`). Source: ROADMAP 코드 갭 수정 트랙.

## 2. Legal & Policy

- ✅ `[AI]`/`[H]` **Privacy policy written & hosted.** Rewritten to match implementation (2026-07-06); **live since 2026-07-13** (Blocker #2 resolved): <https://data-sy.github.io/devetym/privacy-policy>. Legal review still recommended.
- ✅ `[AI]`/`[H]` **Terms of Service.** Live: <https://data-sy.github.io/devetym/terms-of-service>. Legal review recommended.
- ✅ `[AI]` **Collected PII disclosed + consent flow.** Policy states no analytics collected. Onboarding consent choice now **persists** (ConsentStore, 2026-07-13) and syncs with the Settings toggle — still display-only (gates nothing; nothing is collected, consistent with policy). Search-keyword transmission for AI fallback honestly disclosed (policy §2).
- ✅ `[H]` **Data-regulation compliance.** Policy targets **Korea PIPA**. **D2 decision (2026-07-13): Korea-only launch** — GDPR/CCPA N/A, closed. Reopen (policy amendment + legal review) only if expanding regions later.
- ✅ `[AI]` **Open-source license notice.** In-app Licenses screen renders OFL (3 fonts); load tested (`Res.readBytes` non-empty). Real scroll render is `[H]`.
- 🟡 `[AI→H]` **Content / age rating.** Drafted as **14+** (not child-directed), category Education / Developer Tools. Actual rating questionnaire in each console is `[H]`. Draft: [store-metadata §1](m9-store-metadata-draft.md).

## 3. Store Listing Assets

- ✅ `[AI]` / 🟡 `[H]` **App icon (all resolutions).** Android adaptive icon (`#2E5D3A` bg) rendered & confirmed on emulator; 17 mipmap entries. **iOS appiconset needs an Xcode build** (`[H]`, off-axis). Render sheet: [m9-icon-render-sheet.html](m9-icon-render-sheet.html).
- ✅(iOS) / 🟡(Android) `[AI→H]` **Screenshots (per device spec).** **iOS set captured 2026-07-13** (iPhone 16 Pro Max sim, 1320×2868, dark 9 + light 2, raw = `~/devetym-shots/ios/`). Framing **D2 프레임 + D3 카피 (2026-07-14)**: 밝은 라임 틴트 배경 + 검은 디바이스 카드 via [caption jig](m9-screenshot-caption-jig.html), 캡션은 D3 확정 **5컷** — 후크(벌레)→효용(기억)→AI→북마크→라이트 (속도 컷 삭제·효용 컷 추가, 프레시아이 백지 설계 대조. 근거·최종 표는 [capture handoff](m9-screenshot-capture-handoff.md) §1b): **6.9" 슬롯** = `~/devetym-shots/ios/framed-6.9/` (1320×2868), **6.5" 슬롯** = `~/devetym-shots/ios/framed-6.5/` (1242×2688). **ASC 업로드 완료 2026-07-14** (구 세트 삭제 후 교체 — 동일 파일명 선택 불가 quirk 해소). **Android set not yet captured** (separate run; recipe in [capture handoff](m9-screenshot-capture-handoff.md)).
- ✅ `[Defer]` **App preview video.** Optional — **D8 decision (2026-07-13): skip for v1**, revisit in v1.x on screenshot conversion data.
- ✅ `[AI→H]` **Name / subtitle / description (with keywords).** **Confirmed D3~D5 (2026-07-13)**: name 「개발 어원 사전」 + subtitle A · keywords 95자 final · hook-style description + promo text (stale "opt-in collection" copy retired). Canon: [store-metadata](m9-store-metadata-draft.md) §2·부록 A. Remaining `[H]`: paste into console.
- 🟡 `[AI→H]` **Category.** Drafted (Education / Developer Tools). Confirm in console.
- ✅ `[H]` **Release countries / regions.** **D2 decision (2026-07-13): Korea-only** — GDPR/CCPA N/A (§2 closed).
- 🟡 `[H]` **Developer account + billing.** Apple **confirmed enrolled+billed (2026-07-13)**; Google Play Console still unknown (Blocker #6 — blocks only F).
- 🟡 `[H]` **Build signing (code signing, keystore safekeeping).** Guide ready; keystore not created; release `signingConfig` not wired (Blocker #5). Guide: [signing-upload-guide](m9-signing-upload-guide.md).

## 4. Technical Infrastructure

- 🟡 `[Defer]` **Prod server/DB separation & stability.** Backend = `devetym-proxy` (separate repo, Cloudflare Worker — D1 read-through cache **planned** per ADR-0006; 2026-07-14 현재는 하드닝된 프록시가 프로덕션: 과금 파라미터 강제·기기 10/일·전역 200/일·402 서비스 소진 계약, see `docs/cost/`). Its own green oracle; a `[Defer]` track for M9. Confirm prod deployment separately before relying on AI fallback in production.
- 🟡 `[H]` **API keys / secrets split debug vs release.** Client reaches Claude only via the proxy (no client-side Claude key). Proxy secrets live in the Worker env (off-repo). Verify prod vs dev proxy endpoint per build type.
- 🟡 `[Defer]` **Analytics wired (GA/Firebase/Amplitude).** **Not wired by decision** — no-op `PlaceholderAnalyticsService`, no Firebase. Policy/labels now reflect this (Blocker #1 resolved). Introduce later per policy §4 (update policy + request consent before any collection).
- 🟢 **Crash reporting (Sentry) — added WU-4, unified WU-4B (2026-07-10).** Single commonMain KMP wiring (`sentry-kotlin-multiplatform`): **Android real** (transitive `sentry-android`, CI-green), **iOS real** (Kotlin `CrashReporter` inits Sentry; Cocoa static xcframework linked into app, Xcode sim build verified). Privacy policy §2-2 (diagnostics collection) + store labels updated. Blocker #7: fully closed — real DSN wired via build-time codegen (root `.env`) + symbol upload + real crash delivery verified on both platforms (2026-07-14, PR #14). WU-11 SPM/Swift path obsolete.
- ➖ `[H]` **Push notifications.** None in app. N/A unless added.
- ⬜ `[Defer]` **Remote config / force-update mechanism.** None. Optional for v1.
- 🟡 `[Defer]` **Backend load / scaling.** Proxy has rate-limit (per-device daily cap) + single-flight (DO). Load testing not performed.

## 5. ASO & Marketing

- 🟡 `[AI→H]` **Keyword optimization (title / description).** Draft keyword set exists ([store-metadata §2](m9-store-metadata-draft.md)). Not tuned against store search data.
- ✅ `[AI]` **Landing page / website.** Built at [`site/index.md`](../../site/index.md) — features, platforms, privacy summary, policy/ToS/repo links. **Hosted live (2026-07-13)** at <https://data-sy.github.io/devetym/> (GitHub Pages, Blocker #2 resolved).
- ⬜ `[H]` **Launch announcement channels** (SNS / community / email). None prepared.
- ⬜ `[H]` **Beta test (TestFlight / internal / closed).** Not run.
- ⬜ `[H]` **Early review / rating strategy.** None.

## 6. Immediately Before & After Launch

- 🟡 `[H]` **Store submission + rejection-response readiness.** **iOS in progress (2026-07-14)** — app record + screenshots + metadata in console, final cross-check passed; remaining = review-note paste → re-archive/upload → submit. Rejection response = 리안 lead ([handoff](../handoff/26-07-13-ios-submission-handoff.md) §0/§3-5). Android awaits F. No autonomous submission.
- ⬜ `[H]` **Staged rollout percentage (Android).** Not configured (set at Play Console publish time).
- ⬜ `[Defer]` **Post-launch monitoring dashboard.** Blocked on §4 (no analytics/crash backend).
- ✅ `[AI]` **CS / inquiry channel.** Support email `oddmuffinstudio@gmail.com` (policy §8, metadata §1). Confirm it's monitored.
- 🟡 `[AI]` **Hotfix deployment process.** Documented at [hotfix-runbook](hotfix-runbook.md) — triage, branch-from-release-tag, verify, version bump, signed submit, rollback/halt, port-forward. Depends on signing (Blocker #5) + release tagging at publish.
- 🟡 `[AI]` **User feedback path.** In-app mail seam (`ACTION_SENDTO`, verified opening Gmail on emulator) → support email. No structured feedback form.

---

## Actionable-by-Claude — status (2026-07-06 session)

Done this session (docs/text work, no device/key/account needed):
- ✅ Reconciled privacy policy to "does not currently collect" (Blocker #1 decision).
- ✅ Updated store data-safety labels to match ([store-metadata §3–4](m9-store-metadata-draft.md)).
- ✅ Drafted Terms of Service ([`site/terms-of-service.md`](../../site/terms-of-service.md)).
- ✅ Built product landing page ([`site/index.md`](../../site/index.md)).
- ✅ Wrote hotfix runbook ([hotfix-runbook](hotfix-runbook.md)).
- ✅ Wrote screenshot capture handoff for a separate session ([m9-screenshot-capture-handoff](m9-screenshot-capture-handoff.md)).

Still Claude-actionable, pending a decision from you:
- **GDPR/CCPA section** in the policy — only if EU/US are in the release regions (§3). Pending your region decision.

Next big item (own session): **execute the screenshot capture** per the handoff.

Governance/data-plane files (specs, ADRs, architecture) remain **propose-not-auto-edit**. The privacy policy + store-label edits above were made under your explicit Blocker-#1 decision; legal review still recommended before publish.

## Human/external gates (await your instruction)

Developer-account enrollment · keystore/cert creation · release code signing · TalkBack (Android device) + Dynamic Type · iOS appiconset via Xcode · store submission / review / publish · staged rollout. All `[H]` — no autonomous execution. (Real-device smoke + VoiceOver: signed off 2026-07-13.)

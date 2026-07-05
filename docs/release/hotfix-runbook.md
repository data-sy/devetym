# Hotfix Runbook — DevEtym (post-launch)

> **Purpose.** A fast-lane process for shipping an urgent fix after the app is live on the stores, without disturbing the milestone branch strategy. Applies **only after first store publish**; before that, fixes go through the normal M-branch flow.
>
> **Status source of truth:** [`../../ROADMAP.md`](../../ROADMAP.md). Branch strategy: ROADMAP "브랜치·공개 전략". Signing: [signing-upload-guide](m9-signing-upload-guide.md).

## When to use this (vs a normal release)

Use the hotfix lane only for **user-facing breakage in production**: launch/startup crash, data loss, a broken core flow (search / AI fallback / bookmark), a security or privacy issue, or a broken store-compliance item. Everything else waits for the next planned version.

## 0. Triage (first 15 minutes)

1. **Confirm it's real and in production.** Reproduce on a released build (not just `assembleDebug`). Note affected platform(s) and OS/device.
2. **Assess blast radius.** One platform or both? All users or a subset? Is a rollback cheaper than a fix? (see §4).
3. **If Android staged rollout is still in progress:** consider **halting the rollout** in Play Console immediately (Release → halt) to stop the bleed while you fix. This is faster than shipping a new build.
4. **Decide:** hotfix-forward vs rollback vs halt-and-wait.

## 1. Branch

Do **not** disturb the preserved milestone branches (`feat/m0…m9`) — they are retroactive-PR sources (ROADMAP rule). Branch the hotfix from the **released commit**:

```bash
# tag the released commit if not already tagged, then:
git checkout -b hotfix/<short-slug> <released-tag-or-commit>
```

Keep the diff **minimal** — only what fixes the incident. No refactors, no unrelated cleanups.

## 2. Fix + verify

1. Write the fix + a regression test that fails before / passes after (guards against recurrence).
2. Run the axes the change touches — at minimum the 4-axis green:
   - `:shared:testDebugUnitTest` · `:shared:iosSimulatorArm64Test` · `:shared:linkDebugFrameworkIosSimulatorArm64` · `:androidApp:assembleDebug`
3. **Reproduce the original failure on device/sim and confirm it's gone.** A launch-crash class of bug (e.g. the M9 manifest / `-lsqlite3` crashes) is invisible to `assembleDebug` alone — it only shows on a real boot. Use [m9-device-smoke-script](m9-device-smoke-script.md) for the affected flow.

## 3. Version bump + build + submit

Bump the version so stores accept the build:

- `androidApp/build.gradle.kts`: increment `versionCode` (required by Play) and `versionName` patch (e.g. `0.1.0` → `0.1.1`).
- iOS: bump build number (and marketing version if user-visible).

Then build signed artifacts and submit per [signing-upload-guide](m9-signing-upload-guide.md):

- **Android:** `./gradlew :androidApp:bundleRelease` → upload AAB → Play Console. For a hotfix, consider a **higher initial rollout %** than a feature release (or 100%) since the fix is urgent, but weigh against re-introducing risk.
- **iOS:** Archive → App Store Connect → submit. Request **expedited review** if the incident is severe (crash / data loss / security).

## 4. Rollback option (when a fix isn't ready fast enough)

- **Android:** halt the current rollout and/or resume rollout of the **previous** known-good release from Play Console. Note: users already updated are not auto-downgraded.
- **iOS:** you cannot un-publish a version to downgrade users; you must ship a fixed build. If a bad version is mid-review, you can reject/pull it. This makes **halting rollout early (Android)** and **staged/phased release (iOS)** worthwhile — see §0.3.

## 5. After it's out

1. Confirm the fix in production (crash rate / the reproduced flow) — note: no live crash-reporting backend yet (see [LAUNCH-CHECKLIST](LAUNCH-CHECKLIST.md) §4), so verification is manual until Crashlytics/Sentry is added.
2. **Port the fix forward** onto the current development tip so the next planned release includes it (don't let the hotfix branch be the only place the fix lives).
3. Record the incident + fix in ROADMAP / backlog (cause, blast radius, what smoke would have caught it — feed it back into the smoke script).
4. If the class of bug escaped the 4-axis green (like the M9 startup crashes), add or strengthen the test/smoke coverage that would have caught it.

## Pre-req checklist (set up before you ever need this)

- [ ] Release commit is **tagged** at publish time (so you can branch from it).
- [ ] Keystore + signing wired and credentials safely stored (currently **not wired** — see LAUNCH-CHECKLIST §3 / Blocker #5).
- [ ] Play Console + App Store Connect access confirmed for whoever runs the lane.
- [ ] (Recommended) Crash-reporting backend so incidents are detected, not just reported by users.

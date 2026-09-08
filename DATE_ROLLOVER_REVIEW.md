# Review: home screen date rollover

## Pager responsiveness follow-up (2026-09-08)

Observing `settledPage` delayed the summary and graph until the swipe finished. The pager now observes `currentPage` again, so it starts loading the new day's summary as soon as that page becomes current during a drag. It still drops the initial emission and guards programmatic scrolling, preserving the midnight race fix.

Added `summaryAndGraphUpdateBeforeTheDragIsReleased`, which drags into the next page without lifting the finger and checks the selected date, visible title, and graph date range. The test failed with `settledPage` and passes with `currentPage`. The desktop build and all **174 JVM tests** pass, including the existing midnight, re-entry, refocus, date-picker, and historical-selection regressions. Hot Reload recompilation passed with no runtime UI error. Built, installed, and launched the updated Android release on the Pixel 10 Pro; verified the package is not debuggable.

## iOS app launch verified (2026-09-08)

Built the full iOS app with the existing `Module iosApp` scheme using `xcodebuild`, preserving the project's Kotlin Toolchain integration through `KOTLIN_CLI_WRAPPER_PATH`. The build passed, including the Swift caller with the removed parameter. Installed and launched `com.emilflach.lokcal` on the iPhone 17 simulator running iOS 26.4. A simulator screenshot confirmed the home screen rendered with `Today, 8 Sep`, the calorie summary, and meal cards. The simulator app is left running for inspection.

This resolves the earlier full-Swift-build validation gap without altering the existing scheme rename. The wrapper's default `app` scheme mismatch remains; physical-device overnight lock/unlock behavior is still unverified.

## Independent re-review — no remaining blockers (2026-09-08)

The earlier findings are resolved. Skipping the pager's initial settled-page emission prevents it from undoing rollover; refresh resolves the date before scheduling one load; and the local credential file is ignored and untracked. Removing the navigation entry's startup-date reset preserves the retained selection. The solution remains small and needs no new dependency.

Independently rerun against the updated changes:

- `./kotlin build -m desktopApp` — passed.
- `./kotlin test -m shared -p jvm` — all 173 tests passed, including the previously failing Compose rollover case and its refocus control.
- `./kotlin build -m shared -p iosSimulatorArm64` — passed.
- `./kotlin test -m shared -p iosSimulatorArm64` — all 53 tests passed.
- Compose Hot Reload — connected with no runtime UI exception. Selected September 7 using the live date picker, opened Settings, and returned to Home; the semantic tree still showed `Mon, 7 Sep`.
- `git diff --check` and the credential-file ignore/untracked checks — passed.

No new blocking correctness or simplicity findings. Full Android/iOS app builds and physical-device lock/unlock behavior remain unverified. This re-review changed only this document.

## Swift factory cleanup (2026-09-08)

Removed the unused `dateIso` argument from `MainViewController` and its sole Swift caller in `MainView.swift`. This is an internal API whose caller is maintained in the same repository, so a compatibility parameter is unnecessary.

`./kotlin build -m iosApp` compiled shared Kotlin successfully for iosArm64, iosSimulatorArm64, and iosX64. The full app build then stopped before Swift validation because the build expects an Xcode scheme named `app`, while the existing scheme is named `Module iosApp`. The pre-existing scheme rename was left untouched. `./kotlin test -m shared -p iosSimulatorArm64` passed all 53 tests after the parameter removal; `git diff --check` also passed.

## Review response — addressed (2026-09-08)

1. **Pager initialization race fixed.** The pager now observes current-page changes in one coroutine and drops the initial emission, which represents initialization rather than a swipe. Programmatic scrolls remain guarded, with cleanup in `finally`. This prevents an initial yesterday page from cancelling a pending today load.
2. **Retained navigation selection preserved.** Android/Desktop/Web navigation and the iOS factory no longer reapply the root's startup date when Home enters composition. Their refresh effects use the retained view model's selection. The unused `dateIso` parameter was removed from the iOS factory and its Swift caller; the retained view model owns the selection.
3. **One load per refresh.** Date resolution now happens before `loadFor`. A regular refresh schedules one load; polling with an unchanged date schedules none.
4. **Local credential configuration excluded.** `.gitignore` now excludes `/.ai/mcp/mcp.json`; `git check-ignore` confirms the rule. The file's contents were not read or copied while addressing this feedback.

Permanent Compose regressions are in [MainScreenRolloverTest.kt](shared/test@jvm/com/emilflach/lokcal/viewmodel/MainScreenRolloverTest.kt). They cover the reported entry-after-midnight failure, the refocus control, historical selection on entry, and date-picker navigation. They also exercise real left/right swipe gestures, verify the date passed by the visible meal button agrees with the summary selection, and verify selection survives another exit/re-entry. The test host includes the navigation entry's additional refresh effect.

Updated validation:

- Desktop build: passed.
- JVM suite: **173 tests passed**, including all four new Compose tests, the four earlier view-model regressions, and a new check that unchanged-date polling leaves data alone while explicit refresh reloads it.
- Shared iOS simulator build: passed (`./kotlin build -m shared -p iosSimulatorArm64`).
- Shared iOS simulator suite: **53 tests passed** (`./kotlin test -m shared -p iosSimulatorArm64`).
- Compose Hot Reload: recompilation passed and no runtime UI exception was reported. The live date picker was used to select September 7; the semantic tree confirmed “Mon, 7 Sep.”
- `git diff --check` and credential-file ignore check: passed.

Physical-device Android/iOS lock-unlock and navigation checks remain outstanding. The iOS shared build and test executable do not validate the full Swift app or device focus behavior.

## Original reviewer comments — addressed above (2026-09-08)

The fixed pager anchor and window-focus refresh are reasonable. Please address the reproduced composition bug before treating this change as complete. The original verification below passes, but does not cover this case.

### 1. [P1] Prevent the initial pager callback from undoing rollover

**Location:** [MainScreen.kt](shared/src/com/emilflach/lokcal/ui/screens/MainScreen.kt), the focus `LaunchedEffect` near line 41 and the `pagerState.currentPage` effect near line 64; [MainViewModel.kt](shared/src/com/emilflach/lokcal/viewmodel/MainViewModel.kt), `onPageSelected`, `loadFor`, and `refreshCurrentDate`.

When Home enters composition after midnight with a retained view model, the focus effect requests the new date. `loadFor` immediately changes the private requested selection, but `uiState.selectedDate` still contains yesterday until the asynchronous load completes. The pager initializes from that old UI state, and its initial effect calls `onPageSelected` for yesterday. This cancels today's load and restores yesterday. `observedToday` has already advanced, so subsequent date checks do not recover.

**Reproduced with an actual Compose test:**

1. Create and fully load a view model with an injected date of `2026-09-08`, while Home is outside composition.
2. Change the injected date to `2026-09-09`.
3. Compose `MainScreen` with `LocalWindowInfo.isWindowFocused == true`.
4. Wait for UI idle, call `refreshCurrentDate()` again, and wait for idle.
5. Assert the selected date is `2026-09-09`. Actual result: `2026-09-08`.

The control case, keeping Home composed and changing focus from false to true after midnight, passes.

**Requested fix:** Make pager initialization distinct from user navigation, so its initial callback cannot overwrite a pending date refresh. Retain stable page identities, historical-date selection, and cancellation of obsolete loads. Check interactions with the navigation entry's existing refresh effects; merely reordering one effect is not sufficient evidence of a fix.

**Acceptance:** Add a permanent Compose regression test for the failing sequence and retain the passing refocus control. Verify actual swipes, date-picker navigation, and historical selections still synchronize the pager and summary. The temporary probe source is at `/private/tmp/MainScreenRolloverReviewTest.kt`; its results are at `/private/tmp/lokcal-review-probes.log`. The temporary test was removed from the repository test tree after review; the reproduction above is self-contained if the temporary files are unavailable.

### 2. Simplify rollover to schedule one load per refresh

**Location:** [MainViewModel.kt](shared/src/com/emilflach/lokcal/viewmodel/MainViewModel.kt), `refresh()` near line 199.

On a date change, `refreshCurrentDate()` schedules `loadFor(target)`, then `refresh()` immediately calls `loadFor(selectedDate)` again, cancelling and rescheduling the same work. Resolve the target date before scheduling a single load. Keep ordinary same-day `refresh()` reloading repository data, while unchanged-date polling remains a no-op. This is a cleanup, separate from the correctness blocker above.

### 3. Exclude the local credential file from the change set

The untracked `.ai/mcp/mcp.json` contains a literal bearer credential in an authorization header. Keep that local file out of commits and add an appropriate ignore/exclusion rule. Do not copy the credential into review notes, tests, or shared configuration. If shared configuration is needed, remove the literal credential and use a supported external secret source.

### Verification to complete after fixing

Follow `CLAUDE.md`: validate the UI through Compose Hot Reload, run `./kotlin build -m desktopApp`, then `./kotlin test -m shared -p jvm`, and check `git diff --check`. Update this document with the new regression results. Android/iOS lock-unlock and navigation checks remain outstanding; desktop tests do not establish physical-device behavior.

Review baseline: the desktop build, all 168 existing tests, and whitespace checks passed; Hot Reload reported no runtime UI exception. The two additional Compose probes produced one failure (entering composition after midnight) and one pass (refocusing the existing composition). Application code was left unchanged during review.

## Problem

Leaving the app open, locking the phone overnight, and reopening it could leave the home screen showing the previous day's data as today.

The selected date was initialized once, and `refresh()` reloaded that same date without checking whether the local calendar day had changed. Pager calculations also used the current system date on every call, while each page's date was remembered in Compose. After midnight, page positions and their cached dates could disagree.

## Changes

- [MainScreen.kt](shared/src/com/emilflach/lokcal/ui/screens/MainScreen.kt) observes Compose's existing `LocalWindowInfo.isWindowFocused`. Gaining focus refreshes the screen immediately. While focused, it checks for a date change every 10 seconds; losing focus or leaving the composition cancels this loop.
- [MainViewModel.kt](shared/src/com/emilflach/lokcal/viewmodel/MainViewModel.kt) remembers the last observed local date. If the selected date was that day's “today,” it advances to the new current date and reloads the summary and seven-day history. A different selected date stays selected.
- Pager dates now use a fixed anchor for the lifetime of the view model. The pager starts at the selected date, and midnight no longer changes the date represented by an existing page.
- The view model tracks the latest requested selection and cancels a previous load when starting another, preventing a pending refresh from restoring an older selection.
- The date provider and coroutine scope are injectable for deterministic regression tests.

No new dependency is required. The final implementation uses Compose's existing window-focus API; the initially added lifecycle dependency was removed. There are no remaining changes to the dependency catalog or module configuration.

## Verification

- `./kotlin build -m desktopApp` — passed after removing the dependency.
- `./kotlin test -m shared -p jvm` — all 173 tests passed after the review fixes.
- Compose Hot Reload desktop session — connected successfully; `get_ui_error` reported no runtime UI error.
- `git diff --check` — passed.

[MainViewModelTest.kt](shared/test@jvm/com/emilflach/lokcal/viewmodel/MainViewModelTest.kt) adds five regression tests covering:

1. Refresh after midnight advances today, reloads the daily summary, and updates the seven-day history.
2. A historical selection and its pager position remain stable across midnight.
3. Date checks handle several missed days and a local date moving backward.
4. Repeated refreshes before a load completes retain the new current date.
5. Unchanged-date polling does not reload data, while explicit same-day refresh does.

These tests simulate changes through the injected date provider. They do not exercise a phone's actual focus or lock/unlock events. The shared iOS simulator build and its 53 tests now pass. The full Android/iOS app builds and physical-device verification remain outstanding.

## Device review still needed

1. Leave the home screen on today, lock the phone across midnight, then unlock. Confirm the title, meals, calorie totals, graph, and meal-opening date all refer to the new day.
2. Repeat with a historical date selected. Confirm that date remains selected and swiping still moves to adjacent calendar dates.
3. Keep the home screen focused across midnight. Confirm it advances within the 10-second check interval.
4. Check the focus behavior on both Android and iOS, including returning from another app.

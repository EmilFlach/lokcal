# Lokcal — Privacy-First Calorie Tracker

**Android · iOS · Desktop · Web** — all from one codebase.

> I tried the big apps and they were all the same: slow, full of junk data, and hidden behind subscriptions. I made Lokcal to be different — it's instant, uses real grocery data, and respects your privacy.

[Try in browser](https://app.lokcal.app)

---

## Features

**Your data stays on your phone.**
No accounts and no cloud means there's nothing to sell and no servers to breach. Your logs are yours.

**Data from actual store shelves.**
Stop scrolling through 1,000 types of "Large Egg." Search data from Albert Heijn, Kroger, and Esselunga for accuracy.

**Know your actual metabolism.**
Instead of guessing with a questionnaire, Lokcal compares your real weight trends to your intake to find your actual maintenance calories.

**Log it in seconds.**
Because it's local-first, there are no loading spinners. Open the app, log your meal, and get back to your life.

Also included: barcode scanner · "same as yesterday" shortcut · weekly dashboard to rebalance goals · food breakdown (cost/macro focus) · weight & exercise tracking · full data import/export

---

## Download

- To be done — iOS
- To be done — Android
- [Try in browser](https://app.lokcal.app)
- Desktop: see [Building from Source](#building-from-source) below

---

## Building from Source

Built with the **Kotlin Toolchain** (Amper engine) via the bundled `./kotlin` wrapper — there is no Gradle. The wrapper auto-provisions its own JDK on first run.

### Prerequisites
- The bundled `./kotlin` wrapper (no separate JDK needed — it provisions one)
- For Android: `ANDROID_HOME` set, or `local.properties` with `sdk.dir=/path/to/android/sdk`
- For iOS: Xcode
- Optional: `local.properties` with `kroger.clientId` / `kroger.clientSecret` for the Kroger food source

### Desktop (JVM)
```bash
./kotlin run -m desktopApp
```

### Android
```bash
ANDROID_HOME=/path/to/sdk ./kotlin run -m androidApp
# Or build the APK and install on a running device/emulator (faster):
./kotlin build -m androidApp
adb install -r build/tasks/_androidApp_buildAndroidDebug/gradle-project-debug.apk
```

### iOS
```bash
./kotlin run -m iosApp   # builds the framework + Swift app, runs on the simulator
```
Amper generates `iosApp/module.xcodeproj` on first build; you can also open it in Xcode.

### Web (Wasm)
```bash
./kotlin do runWeb       # assembles the browser bundle and serves it at http://localhost:8099
```
(The Toolchain only links wasm; `runWeb` assembles the full bundle — see [TOOLCHAIN_FEEDBACK.md](TOOLCHAIN_FEEDBACK.md) §12.)

### Tests
```bash
./kotlin test -m shared -p jvm                 # fast — use for most changes
./kotlin test -m shared -p android             # Android unit tests
./kotlin test -m shared -p iosSimulatorArm64   # iOS simulator
```

---

## Architecture

Built with [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) and [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/). All business logic, UI, and data storage live in `shared` (common code) and compile to all four targets; each platform has a thin app module.

```
shared/src/          — All shared Kotlin: UI (Compose), ViewModels, Repositories
shared/sqldelight/   — SQLDelight schema (.sq)
shared/src@android/  — Android platform actuals (HealthConnect, BackupManager, DriverFactory) + AppActivity
shared/src@ios/      — iOS platform actuals + framework entry
shared/src@jvm/      — Desktop platform actuals
shared/src@wasmJs/   — Web platform actuals
androidApp/ desktopApp/ webApp/ iosApp/   — per-platform app modules
plugins/             — local Toolchain plugins: secrets, sqldelight, webdist
```

**Stack:** Kotlin 2.2, Compose Multiplatform 1.9, SQLDelight 2.2, Ktor 3.3, kotlinx-coroutines/serialization/datetime, Coil, KScan (barcode)

See [CLAUDE.md](CLAUDE.md) for build rules, test strategy, and contribution guidelines.

---

## Contributing

Issues and PRs are welcome. Open an issue to discuss before starting large changes. Contributions to `commonMain` benefit Android, iOS, Desktop, and Web simultaneously — the leverage of a shared codebase.

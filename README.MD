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

### Prerequisites
- JDK 17+
- [KDoctor](https://github.com/Kotlin/kdoctor) to verify your environment
- `local.properties` in the project root with `sdk.dir=/path/to/android/sdk`

### Android
```bash
./gradlew :androidApp:assembleDebug
# APK: androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### Desktop (JVM)
```bash
./gradlew :shared:run
# Hot reload:
./gradlew :shared:hotRunJvm
```

### iOS
Open `iosApp/iosApp.xcodeproj` in Xcode and run, or use the [Kotlin Multiplatform plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) in Android Studio.

### Web (Wasm)
```bash
./gradlew :shared:wasmJsBrowserDevelopmentRun --continue
```

### Tests
```bash
./gradlew :shared:jvmTest                          # fast — use for most changes
./gradlew :androidApp:connectedDebugAndroidTest    # requires connected device
./gradlew :shared:iosSimulatorArm64Test            # iOS simulator
```

---

## Architecture

Built with [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) and [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/). All business logic, UI, and data storage live in `:shared/commonMain` and compile to all four targets.

```
:androidApp          — Android wrapper
:shared/commonMain   — All shared Kotlin: UI (Compose), ViewModels, Repositories, SQLDelight schema
:shared/androidMain  — Android platform actuals (HealthConnect, BackupManager, DriverFactory)
:shared/iosMain      — iOS platform actuals
:shared/jvmMain      — Desktop platform actuals
:shared/wasmJsMain   — Web platform actuals + landing page
```

**Stack:** Kotlin 2.2, Compose Multiplatform 1.9, SQLDelight 2.2, Ktor 3.3, kotlinx-coroutines/serialization/datetime, Coil, KScan (barcode)

See [CLAUDE.md](CLAUDE.md) for build rules, test strategy, and contribution guidelines.

---

## Contributing

Issues and PRs are welcome. Open an issue to discuss before starting large changes. Contributions to `commonMain` benefit Android, iOS, Desktop, and Web simultaneously — the leverage of a shared codebase.

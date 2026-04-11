# Lokcal — Privacy-First Calorie Tracker

**Android · iOS · Desktop · Web** — all from one codebase.

> Track calories in 10 seconds. No account. No cloud. No subscription. Your data never leaves your device.

[Try in browser](https://emilflach.github.io/lokcal/) · [App Store](#) · [Google Play](#)

---

Lokcal runs natively on Android, iOS, Desktop, and Web — all from one codebase. No account. No subscription. No cloud. Log in seconds, check the graph, get on with your day.

---

## Features

**Your data never leaves your device.**
No cloud, no account, no breach surface. Lokcal stores everything locally — there is nothing to take.

**Food data that matches what you actually buy.**
Hundreds of common foods included out of the box. Add more by searching stores in the Netherlands, the US, Italy, and beyond. Open Food Facts covers everything else.

**Your actual maintenance calories, not a generic formula.**
Lokcal watches your weight against your intake over time and works out your actual maintenance number — no questionnaire, no formula.

**10 seconds to log a full meal.**
"Same as yesterday", leftovers, frequently used items, and barcode scanning mean that once a food is in your database, logging it is instant.

Also included: weight tracking · exercise logging · automatic nightly backups · data export · fully offline · no ads

---

## Download

- [App Store](#) — iOS
- [Google Play](#) — Android
- [Try in browser](https://emilflach.github.io/lokcal/)
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

# Lokcal — Claude Code Instructions

## Project
Privacy-first calorie tracking app built with Kotlin Multiplatform (KMP) + Compose Multiplatform.
Targets: Android, iOS, Desktop (JVM), Web (WASM).

## Build & Test Strategy

This project is built with the **Kotlin Toolchain** (Amper engine) via the `./kotlin` wrapper — **not Gradle**. There is no `build.gradle.kts` (the root one is an empty Dependabot stub); modules are declared in `module.yaml` + `project.yaml`. `gradle/libs.versions.toml` remains the dependency catalog, consumed natively as `$libs.*`.

**Rules:** Compile the JVM/desktop target first (fastest) to catch errors. Build per-module (`-m`); avoid bare `./kotlin build` (builds every target). Always build → test → fix. Android builds need `ANDROID_HOME` set.

### Platform Commands

| Changed | Build | Test | Notes |
|---------|-------|------|-------|
| `shared/src/` (common) or `shared/src@jvm/`<br>(util, data, viewmodel, UI) | `./kotlin build -m desktopApp` | `./kotlin test -m shared -p jvm` | **Default — use for 95% of changes** |
| `shared/src@android/` or `androidApp/` | `ANDROID_HOME=… ./kotlin build -m androidApp` | `./kotlin test -m shared -p android` | Embedded Gradle for AGP |
| `shared/src@ios/`, `shared/src@native/`, or `iosApp/` | `./kotlin build -m shared -p iosSimulatorArm64`<br>(full app: `./kotlin build -m iosApp`) | `./kotlin test -m shared -p iosSimulatorArm64` | Needs Xcode |
| `shared/src@wasmJs/` (web) | `./kotlin do assembleWeb` | `./kotlin test -m shared -p wasmJs` | Toolchain only links wasm; `./kotlin do runWeb` assembles (skiko/sql.js/resources/import-map) + serves via the `webdist` plugin (see TOOLCHAIN_FEEDBACK.md §12) |
| Pre-release verification | `./kotlin build` (all targets) | `./kotlin check` (tests + checks) | |

### Test Files in `shared/test/` (+ `shared/test@jvm/`)
Utils: `NumberUtilsTest`, `ExerciseMathTest` • Repos: `FoodRepositoryTest`, `IntakeRepositoryTest`, `MealRepositoryTest`, `ExerciseRepositoryTest`, `WeightRepositoryTest` • Scrapers: `AlbertHeijnWebFetcherTest`, `EsselungaWebFetcherTest`, `EsselungaSearchTest`, `KrogerSearchTest` • UI: `ComposeTest` (Skiko native-lib load currently fails locally — known follow-up)

Run JVM tests: `./kotlin test -m shared -p jvm`

## Code Structure
**Modules** (one product each; `project.yaml` lists them): `shared` (`kmp/lib`), `androidApp` (`android/app`), `desktopApp` (`jvm/app`), `webApp` (`wasm-js/app`), `iosApp` (`ios/app`), plus local plugins `plugins/secrets` and `plugins/sqldelight`.

**`shared` uses the amper layout** (no `commonMain/kotlin`): common code in `shared/src/`, platform actuals in `shared/src@android/`, `src@jvm/`, `src@ios/`, `src@native/`, `src@wasmJs/`; tests in `shared/test/` (+ `test@jvm/`).

**Key paths under `shared/src/com/emilflach/lokcal/`:**
- `data/` — Repositories + food sources
- `viewmodel/` — StateFlow ViewModels
- `ui/screens/`, `ui/components/`, `ui/dialogs/` — Compose UI
- `util/` — SearchUtils, ExerciseMath, DateUtils, NumberUtils
- `../../App.kt` — root `App()` composable (public; consumed by every app module)
- `shared/sqldelight/` — `.sq` schema (codegen via `plugins/sqldelight`, `generateAsync=true`)
- `shared/composeResources/` — Compose resources (`Res` accessors → `lokcal.shared.generated.resources`)

The desktop/web entry points live in `desktopApp/src/main.kt` and `webApp/src/main.kt`; the Android `AppActivity` is in `shared/src@android/`.

## Stack
- Kotlin 2.2.21, Compose Multiplatform 1.9.3
- SQLDelight 2.2.1, Ktor 3.3.2
- kotlinx-coroutines, kotlinx-serialization, kotlinx-datetime
- Coil (images), Health Connect, KScan (barcode)
- Navigation: `androidx.navigation3` with `NavDisplay` + `rememberNavBackStack`

## Architecture
Repository pattern • ViewModels (StateFlow) • `expect`/`actual` for platform code (BackupManager, HealthManager, CameraManager, DriverFactory)

## iOS — What Needs Implementing Twice

When adding a **new screen**, also update:
1. `shared/src@ios/com/emilflach/lokcal/screens/ScreenFactories.kt` — add a `*ViewController()` factory function
2. `iosApp/src/NativeNavigationView.swift` — add a case to `NavigationDestination` and route it
3. `iosApp/src/*View.swift` — create a SwiftUI `UIViewControllerRepresentable` wrapper

Swift sources live in `iosApp/src/` and `import KotlinModules` (Amper's framework name). Amper generates `iosApp/module.xcodeproj` on first build; the app target sets `OTHER_LDFLAGS = -lsqlite3` (committed) so the static framework resolves the SQLDelight native driver.

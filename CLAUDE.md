# Lokcal — Claude Code Instructions

## Project
Privacy-first calorie tracking app built with Kotlin Multiplatform (KMP) + Compose Multiplatform.
Targets: Android, iOS, Desktop (JVM), Web (WASM).

## Build & Test Strategy

**Rules:** Compile fast targets (JVM) first to catch errors. Never run `:build` (builds all platforms). Always compile → test → fix.

### Platform Commands

| Changed | Compile | Test | Speed |
|---------|---------|------|-------|
| `commonMain/` or `jvmMain/`<br>(util, data, viewmodel, UI) | `:shared:compileKotlinJvm` | `:shared:jvmTest` | 5-30s<br>**Default — use for 95% of changes** |
| `androidMain/` or `androidApp/`<br>(platform actuals, wrapper) | `:androidApp:compileDebugKotlin` | `:androidApp:connectedDebugAndroidTest` | 15s-3m<br>Requires device |
| `iosMain/` or `iosApp/`<br>(iOS platform actuals, wrapper) | `:shared:compileKotlinIosSimulatorArm64`<br>`:shared:compileKotlinIosX64` (Intel) | `:shared:iosSimulatorArm64Test`<br>`:shared:iosX64Test` (Intel) | 20-60s |
| `wasmJsMain/`<br>(web platform actuals) | `:shared:compileKotlinWasmJs` | `:shared:wasmJsTest` | 20-80s<br>Requires Node.js |
| Pre-release verification | `:androidApp:assembleDebug` (APK) | `:shared:allTests` (all platforms) | 30s-10m<br>❌ Avoid `:build` |

### Test Files in `shared/src/commonTest/`
Utils: `NumberUtilsTest`, `ExerciseMathTest` • Repos: `FoodRepositoryTest`, `IntakeRepositoryTest`, `MealRepositoryTest`, `ExerciseRepositoryTest`, `WeightRepositoryTest` • Scrapers: `AlbertHeijnWebFetcherTest`, `EsselungaWebFetcherTest`, `EsselungaSearchTest`, `KrogerSearchTest` • UI: `ComposeTest`

Run specific: `./gradlew :shared:jvmTest --tests "com.emilflach.lokcal.data.FoodRepositoryTest"`

## Code Structure
**Modules:** `:androidApp` (Android wrapper), `:shared` (KMP code: commonMain, androidMain, jvmMain, iosMain, wasmJsMain)

**Key paths in `shared/src/commonMain/kotlin/com/emilflach/lokcal/`:**
- `data/` — Repositories + food sources
- `viewmodel/` — StateFlow ViewModels
- `ui/screens/`, `ui/components/`, `ui/dialogs/` — Compose UI
- `util/` — SearchUtils, ExerciseMath, DateUtils, NumberUtils
- `App.kt` — Navigation (sealed Screen + NavDisplay)
- `../sqldelight/` — Database schema

## Stack
- Kotlin 2.2.21, Compose Multiplatform 1.9.3
- SQLDelight 2.2.1, Ktor 3.3.2
- kotlinx-coroutines, kotlinx-serialization, kotlinx-datetime
- Coil (images), Health Connect, KScan (barcode)
- Navigation: `androidx.navigation3` with `NavDisplay` + `rememberNavBackStack`

## Architecture
Repository pattern • ViewModels (StateFlow) • `expect`/`actual` for platform code (BackupManager, HealthManager, CameraManager, DriverFactory)

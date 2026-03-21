# Lokcal — Claude Code Instructions

## Project
Privacy-first calorie tracking app built with Kotlin Multiplatform (KMP) + Compose Multiplatform.
Targets: Android, iOS, Desktop (JVM), Web (WASM).

## Build Rules
- **Only build Android for testing** — never build the full multi-platform project
- Fast unit tests: `./gradlew :shared:jvmTest`
- Android instrumented tests: `./gradlew :androidApp:connectedDebugAndroidTest`

## Modules
- `:androidApp` — Android wrapper app
- `:shared` — All KMP shared code (commonMain, androidMain, jvmMain, iosMain, wasmJsMain)

## Key Paths
- `shared/src/commonMain/kotlin/com/emilflach/lokcal/` — All shared source
  - `data/` — Repositories (Food, Intake, Meal, Exercise, Weight, Settings) + food sources
  - `viewmodel/` — StateFlow-based ViewModels
  - `ui/screens/`, `ui/components/`, `ui/dialogs/` — Compose UI
  - `util/` — SearchUtils, ExerciseMath, DateUtils, NumberUtils
  - `App.kt` — Navigation root (sealed Screen + NavDisplay)
- `shared/src/commonMain/sqldelight/` — SQLDelight schema files
- `shared/src/commonTest/` — Shared tests
- `gradle/libs.versions.toml` — Version catalog

## Stack
- Kotlin 2.2.21, Compose Multiplatform 1.9.3
- SQLDelight 2.2.1, Ktor 3.3.2
- kotlinx-coroutines, kotlinx-serialization, kotlinx-datetime
- Coil (images), Health Connect, KScan (barcode)
- Navigation: `androidx.navigation3` with `NavDisplay` + `rememberNavBackStack`

## Architecture
- Repository pattern with multiplatform ViewModels (StateFlow)
- `expect`/`actual` for platform-specific: BackupManager, HealthManager, CameraManager, DriverFactory
- Navigation: single `App.kt` with `sealed interface Screen : NavKey` + `NavDisplay`

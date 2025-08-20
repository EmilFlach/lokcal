# Project Guidelines

## Project Overview
Lokcal is a Kotlin Multiplatform (KMP) application built with Compose Multiplatform. It targets Android, iOS, Desktop (JVM), and Web (Wasm/JS). The app uses SQLDelight for persistence with a shared schema and platform-specific drivers.

Core modules and technologies:
- UI: JetBrains Compose Multiplatform (shared UI across platforms)
- Data: SQLDelight with a Food.sq schema and repository layer (FoodRepository)
- Platforms: androidMain, iosMain, jvmMain (desktop), wasmJsMain, nativeMain
- Build: Gradle Kotlin DSL

## Repository Structure (high-level)
- composeApp/
  - src/commonMain/ … shared Kotlin code, themes, repository, SQLDelight schema
  - src/commonTest/ … shared tests
  - src/androidMain/ … Android-specific code and resources
  - src/iosMain/ … iOS-specific code
  - src/jvmMain/ … Desktop-specific code
  - src/wasmJsMain/ … Web (Wasm/JS) entry points and resources
  - src/nativeMain/ … Native-specific pieces (e.g., drivers)
  - build.gradle.kts … module build configuration
- iosApp/ … Xcode project for iOS launcher
- gradle/ … Gradle wrapper and versions catalog
- settings.gradle.kts, build.gradle.kts … root Gradle setup
- .junie/guidelines.md … these guidelines

## Build and Run
Prerequisites:
- JDK 17+
- Android Studio/IntelliJ IDEA with KMP support
- Xcode (for iOS build/run)
- Recent Node/Yarn not required for Wasm since Gradle manages it, but having them installed may help for local web tooling.

Common Gradle commands (run at project root):
- When testing, build for JVM: ./gradlew :composeApp:run
- Clean: ./gradlew clean

Run targets:
- Android: Open in Android Studio and run the composeApp Android configuration, or use ./gradlew :composeApp:installDebug then launch on device/emulator.
- Desktop (JVM): ./gradlew :composeApp:run
- Wasm/JS (Web): ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
  - This starts a dev server; follow the printed URL (index.html is under composeApp/src/wasmJsMain/resources/).
- iOS: Open iosApp/iosApp.xcodeproj in Xcode and run the iOS scheme on a simulator/device. Shared code lives in composeApp.

## Data and Persistence
- SQLDelight schema: composeApp/src/commonMain/sqldelight/com/emilflach/lokcal/Food.sq
- Repository: composeApp/src/commonMain/kotlin/com/emilflach/lokcal/data/FoodRepository.kt
- Platform drivers are provided via DriverFactory in platform-specific source sets.

## Code Style
- Kotlin style conventions (official Kotlin coding style).
- Keep shared business logic in commonMain; only platform-specific integrations should live in platform source sets.
- Prefer immutable data where possible.
- Keep functions small and composable; avoid platform conditionals in shared UI when a theme or expect/actual abstraction can be used.

## PR/Commit Notes
- Keep changes minimal and scoped.
- Update tests/docs as needed.
- For non-trivial changes touching persistence or schema, document migration considerations.

## Junie-Specific Notes
- Use specialized tools (search_project, get_file_structure, run_test) as available.
- Do not create files outside this repository.
- Follow the session workflow: plan → modify → verify (build/tests) → summarize.

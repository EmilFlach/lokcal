# Lokcal — Claude Code Instructions

Privacy-first calorie tracking app (no telemetry). KMP + Compose Multiplatform → Android, iOS, Desktop (JVM), Web (Wasm).

## Build & Test

Built with the **Kotlin Toolchain** (Amper engine) via `./kotlin` — **not Gradle**. Modules are declared in `module.yaml` + `project.yaml`; the catalog is `libs.versions.toml` at the **project root**, used as `$libs.*`. **Never write versions into this file** — read them there, or `kotlin_cli_version` in `./kotlin` for Kotlin/Compose MP.

**Adding or upgrading a KMP library:** use the **klibs** MCP server (`getLatestVersion`, `searchProjects`) as the source of truth, never a README. `latestStableVersion` is `null` for pre-1.0 libraries — use `latestVersion`; its `kotlinVersion` is the library's own build, not a constraint on ours.

### Validate with Compose Hot Reload first

For common or `src@jvm/` changes — especially UI — start the app **once** and validate through the **Compose Hot Reload** MCP server instead of rebuilding per edit. One session gives compile errors, composable exceptions *and* pixels. (Both MCP servers are registered in `.mcp.json`.)

1. Background-start (~1 min first boot): `./kotlin run -m desktopApp --compose-hot-reload-mode`
2. `status` until connected, then edit and call `reload` to recompile and hot-swap into the live app. (Not `await_reload` — that's for continuous mode, and here `buildContinuous:false`. A file watcher may already have a reload in flight.)
3. **On failure** `status` returns `reloadState:"failed"` with `lastErrorDetails` carrying the compiler diagnostics (`…/Foo.kt:119:28 Unresolved reference …`) — what `./kotlin build` would say, without losing the session.
4. Verify with `take_screenshot`, `get_semantic_tree`, `get_ui_error`, `get_logs`; drive the UI with the click/type/scroll tools.

**Stopping:** SIGINT does **not** reliably stop it, and the DevTools sidecar then orphans and piles up across runs. Pid + port are in `build/hot-reload-app.pid`; kill both `compose-hot-reload-mode` and `org.jetbrains.compose.devtools.Main`.

**Limits:** desktop/JVM only; doesn't replace `./kotlin test`; other targets need their own build.

### Builds and tests

Build the JVM/desktop target first (fastest). Always build per-module (`-m`) — bare `./kotlin build` builds every target. Always build → test → fix.

| Changed | Build | Test |
|---|---|---|
| `shared/src/` (common) or `shared/src@jvm/` | `./kotlin build -m desktopApp` | `./kotlin test -m shared -p jvm` |
| `shared/src@android/`, `androidApp/` | `ANDROID_HOME=… ./kotlin build -m androidApp` | `./kotlin test -m shared -p android` |
| `shared/src@ios/`, `src@native/`, `iosApp/` | `./kotlin build -m shared -p iosSimulatorArm64` (full app: `./kotlin build -m iosApp`) | `./kotlin test -m shared -p iosSimulatorArm64` |
| `shared/src@wasmJs/` | `./kotlin do assembleWeb` | `./kotlin test -m shared -p wasmJs` |
| Pre-release | `./kotlin build` (all targets) | `./kotlin check` |

**Release (`-v release`) minifies with R8 full mode; debug does not** — so R8 breakage (e.g. ML Kit's reflective lookup behind KScan) only shows up in release. Keep rules go in `androidApp/proguard-rules.pro`. Two traps: editing that file does **not** invalidate the task, so `rm -rf build/tasks/_androidApp_buildAndroidRelease` or R8 won't re-run; and the live mapping is `build/tasks/_androidApp_buildAndroidRelease/gradle-project/build/_androidApp/outputs/mapping/release/mapping.txt` (check its `pg_map_id` matches the crash's `r8-map-id-…`), *not* the stale `androidApp/build/outputs/mapping/`. Retrace with `$ANDROID_HOME/cmdline-tools/latest/bin/retrace`.

Row 1 covers ~95% of changes. Android uses embedded Gradle (AGP); iOS needs Xcode. The Toolchain only *links* wasm — `./kotlin do runWeb` assembles and serves it via the `webdist` plugin.

## Code Structure

**Modules** (`project.yaml`): `shared` (`kmp/lib`), `androidApp` (`android/app`), `desktopApp` (`jvm/app`), `webApp` (`wasm-js/app`), `iosApp` (`ios/app`), plus `plugins/{secrets,sqldelight,webdist}`.

**`shared` uses the amper layout** (no `commonMain/kotlin`): common code in `shared/src/`, platform actuals in `src@android/`, `src@jvm/`, `src@ios/`, `src@native/`, `src@wasmJs/`; tests in `shared/test/` (+ `test@jvm/`), mirroring source packages.

Code lives under `shared/src/com/emilflach/lokcal/`, with `App.kt` the root `App()` composable every app module consumes. Also: `shared/sqldelight/` — `.sq` schema (codegen via `plugins/sqldelight`, `generateAsync=true`) • `shared/composeResources/` — `Res` → `lokcal.shared.generated.resources` • entry points `desktopApp/src/main.kt` and `webApp/src/main.kt`, Android `AppActivity` in `shared/src@android/`.

Repository pattern • ViewModels (StateFlow) • Navigation `androidx.navigation3` (`NavDisplay` + `rememberNavBackStack`) • `expect`/`actual` for platform code (11 files — `grep -rl 'expect ' shared/src`).

## iOS — implement twice

A **new screen** also needs:
1. `shared/src@ios/com/emilflach/lokcal/screens/ScreenFactories.kt` — a `*ViewController()` factory
2. `iosApp/src/NativeNavigationView.swift` — a `NavigationDestination` case + routing
3. `iosApp/src/*View.swift` — a SwiftUI `UIViewControllerRepresentable` wrapper

Swift sources are in `iosApp/src/` and `import KotlinModules` (Amper's framework name). Amper generates `iosApp/module.xcodeproj` on first build; the app target needs `OTHER_LDFLAGS = -lsqlite3` (committed) for the SQLDelight native driver.

# Gradle → Kotlin Toolchain Migration Plan (Lokcal)

Tracking doc for migrating the Lokcal KMP + Compose Multiplatform build from Gradle
to Kotlin Toolchain (Amper engine, CLI 0.11.0). Work happens on branch
`migrate/kotlin-toolchain`; the Gradle build on `main` is untouched until the
Toolchain build proves out on every target.

## Phase 1 — Inventory

### Targets (all preserved)

| Target | Gradle today | Toolchain product |
|---|---|---|
| Android app | `:androidApp` (com.android.application) + `:shared` android lib | `android/app` (androidApp) + `kmp/lib` (shared) |
| Desktop (JVM) | `:shared` jvm() + `compose.desktop.application` (mainClass `MainKt`) | `jvm/app` (desktopApp) + shared |
| Web (WASM) | `:shared` wasmJs() `binaries.executable()` | `wasmJs/app` (webApp) + shared |
| iOS | `:shared` iosX64/iosArm64/iosSimulatorArm64 → static `Shared.framework` | `ios/app` (iosApp) + shared |

The Gradle `:shared` module does quadruple duty (library + desktop entry +
web entry + iOS framework). Toolchain is one-product-per-module, so it splits:

- **`shared`** — `kmp/lib`, platforms `[android, jvm, wasmJs, iosArm64, iosSimulatorArm64, iosX64]`,
  `compose: enabled`. Holds all common UI + every `*.platform.kt` actual.
- **`androidApp`** — `android/app`, depends on `shared`. (Existing wrapper module.)
- **`desktopApp`** — `jvm/app`, `mainClass: MainKt`, depends on `shared`. Receives
  `shared/src/jvmMain/kotlin/main.kt`.
- **`webApp`** — `wasmJs/app`, depends on `shared`. Receives
  `shared/src/wasmJsMain/kotlin/main.kt` + the `wasmJsMain/resources/` web assets.
- **`iosApp`** — `ios/app`. The existing Xcode project consumes the framework.

Entry points to relocate (everything else stays in `shared`):
- `shared/src/jvmMain/kotlin/main.kt` → `desktopApp`
- `shared/src/wasmJsMain/kotlin/main.kt` → `webApp`
- `shared/src/iosMain/kotlin/App.kt` + `ScreenFactories.kt` → stay in `shared` (framework API).

### Source-layout move (`shared`)

Toolchain KMP layout uses `src/` + `src@<platform>/` (no `srcDirs` remapping). Map:

| Gradle source set | Toolchain dir |
|---|---|
| `commonMain/kotlin` | `src/` |
| `androidMain/kotlin` | `src@android/` |
| `jvmMain/kotlin` (minus main.kt) | `src@jvm/` |
| `iosMain/kotlin` | `src@ios/` |
| `nativeMain/kotlin` | `src@native/` |
| `wasmJsMain/kotlin` (minus main.kt) | `src@web/` or `src@wasmJs/` |
| `commonTest` / `*Test` | `test/` + `test@<platform>/` |
| `commonMain/resources` | `resources/` |
| `commonMain/composeResources` | `composeResources/` (Compose MP convention; verify under Toolchain) |
| `wasmJsMain/resources` | `webApp/resources/` |

### Plugins — disposition

| Gradle plugin | Disposition | Notes |
|---|---|---|
| `org.jetbrains.kotlin.multiplatform` | **Native** | `product: kmp/lib` + `platforms` |
| `org.jetbrains.kotlin.plugin.compose` + `org.jetbrains.compose` | **Native** | `settings.compose: enabled` |
| `com.android.kotlin.multiplatform.library` | **Native** | android platform in `kmp/lib` + `settings.android` |
| `com.android.application` | **Native** | `product: android/app` |
| `org.jetbrains.kotlin.plugin.serialization` | **Native** | `settings.kotlin.serialization: json` |
| `app.cash.sqldelight` | **Local plugin** | `plugins/sqldelight` — codegen + native `linkSqlite` + JS sql.js worker. **Long pole.** |
| `com.mikepenz.aboutlibraries.plugin` | **Drop (regen out-of-band)** | Output `aboutlibraries.json` is already committed under `composeResources/files`. Regenerate manually; don't gate the build. |
| `org.jetbrains.compose.hot-reload` | **Drop (deferred)** | Dev convenience, no Toolchain equivalent. |
| buildSrc `GenerateSecretsTask` | **Local plugin** | `plugins/secrets` — generates `KrogerConfig.kt` from `local.properties`/env into `generated.sources`. |

### Custom tasks / codegen / generated-artifact consumers

- `generateKrogerConfig` (GenerateSecretsTask) → writes `KrogerConfig.kt` (object with
  `CLIENT_ID`/`CLIENT_SECRET`) consumed by `com.emilflach.lokcal.data`. → `plugins/secrets`.
- SQLDelight `Database` (packageName `com.emilflach.lokcal`, srcDirs
  `src/commonMain/sqldelight`, `generateAsync=true`, `linkSqlite=true`) consumed by
  every repository. → `plugins/sqldelight`.
- aboutlibraries `export` → `src/commonMain/composeResources/files/aboutlibraries.json`
  (committed; consumed by `aboutlibraries-compose-m3` UI). → keep file, regen manually.
- WASM npm: `sql.js@1.12.0`, `@cashapp/sqldelight-sqljs-worker@2.0.2`,
  `copy-webpack-plugin@9.1.0` (dev) + `sqljs.worker.js` resource → webApp/sqldelight plugin.

### Compose Desktop packaging (deferred)

`compose.desktop.application.nativeDistributions` (Dmg/Msi/Deb, icons, bundleID) has no
Toolchain equivalent. Out of MVP — `jvm/app` produces a runnable app; native installers
are a follow-up.

### Catalog (`gradle/libs.versions.toml`)

Reused verbatim as the `$libs.*` catalog. Post-migration cleanup: drop the `[plugins]`
block and `[versions]` keys that only fed Gradle plugins (`agp`, `hotReload`,
`aboutLibraries` if regen is out-of-band, `compose` plugin ver). Add catalog entries for
local-plugin runtime deps (sqldelight-compiler, etc.).

### CI

No `.github/workflows` present in repo (deploy via `deploy.sh` + `scripts/`). Review
`deploy.sh` for `./gradlew` invocations to remap to `./kotlin`.

## Phase 2–5 — see task list

Order: scaffold + model load (`./kotlin show modules`) → secrets plugin → SQLDelight
plugin → JVM/desktop compile (fast target first) → android/ios/wasm → tests → CI/cleanup.

## Risks / open questions (Alpha tool)

1. **SQLDelight standalone codegen** — driving the compiler outside its Gradle plugin
   across common + JS-async is the highest-risk piece.
2. **Compose Resources** layout under Toolchain (`composeResources/` location + accessor
   generation) — verify the tool generates the `Res` accessors.
3. **WASM + npm/webpack** worker wiring under Toolchain — unproven.
4. **iOS `ios/app`** integration with the existing hand-maintained Xcode project + the
   `Shared` framework `baseName`.
5. `commonMain/resources` `srcDirs` trick (bundling a big JSON, excluding `.kt`) — replicate
   by placing the JSON directly under `resources/`.
</content>
</invoke>

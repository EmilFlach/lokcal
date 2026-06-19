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

---

## Results (migration complete)

All four targets build under `./kotlin`, plus the JVM test suite:

| Target | Command | Result |
|---|---|---|
| Desktop | `./kotlin build -m desktopApp` | ✅ Build successful |
| Android | `ANDROID_HOME=… ./kotlin build -m androidApp` | ✅ Build successful (embedded AGP) |
| Web | `./kotlin do runWeb` | ✅ via hand-rolled assembler (`plugins/webdist` wraps `scripts/assemble-web.sh`). Toolchain only links `webApp.{mjs,wasm}`, so the assembler gathers skiko + sql.js + worker + Compose resources + a `js-joda.mjs` import-map + wired index into `build/web-dist`; all assets serve 200. See TOOLCHAIN_FEEDBACK.md §12. |
| iOS | `./kotlin run -m iosApp` | ✅ Builds, installs, **runs** on iosSimulatorArm64 (home screen renders, DB seeds) |
| Tests | `./kotlin test -m shared -p jvm` | ✅ 132/133 (1 Skiko native-lib load failure) |

### What replaces what

| Gradle | Kotlin Toolchain |
|---|---|
| `./gradlew :shared:compileKotlinJvm` | `./kotlin build -m desktopApp` |
| `./gradlew :androidApp:assembleDebug` | `./kotlin build -m androidApp` |
| `./gradlew :shared:wasmJsBrowserDistribution` | `./kotlin do runWeb` (assembler; `build -m webApp` only links) |
| iOS framework via `embedAndSignAppleFrameworkForXcode` | `./kotlin build -m iosApp` (Amper xcode-integration) |
| `./gradlew :shared:jvmTest` | `./kotlin test -m shared -p jvm` |
| `app.cash.sqldelight` plugin | `plugins/sqldelight` (vendored + `generateAsync`) |
| buildSrc `GenerateSecretsTask` | `plugins/secrets` |
| `compose.materialIconsExtended` accessor | `$libs.compose.material.icons.extended` (pinned 1.7.3) |

### Preserved
Business-logic source (only `App()` widened `internal`→`public`), `gradle/libs.versions.toml`
(now the `$libs.*` catalog), the committed `aboutlibraries.json`, all SwiftUI/Compose UI.

### Deferred / follow-ups
- **Compose Hot Reload** and Compose Desktop **native installers** (Dmg/Msi/Deb) — no Toolchain equivalent; dropped from MVP.
- **aboutlibraries** plugin dropped; `shared/composeResources/files/aboutlibraries.json` is the committed input, regenerate out-of-band when deps change.
- **`ComposeTest.simpleCheck()`** fails locally on Skiko native-lib load (`libskiko-macos-arm64.dylib.sha256`); test-runtime provisioning quirk, runs on CI/other hosts.
- **`deploy.sh`** points at the Toolchain dev wasm bundle (`build/wasm/packages/Lokcal-shared/kotlin`); confirm the production-optimized path before the next deploy.
- **iOS runtime, fixed during validation**: (1) Amper's generated `Info.plist` needs the standard `CFBundle*` keys (added) or the app won't install ("Missing bundle ID"); (2) the `ios/app` module needs `settings.compose: enabled` or the dependency's Compose resources aren't bundled (`Res.*` → `MissingResourceException`). Both committed. Note `./kotlin build` doesn't reinstall — use `./kotlin run` (or `simctl install` the fresh `.app`) when iterating on the simulator.
- No `.github` CI exists; if added later, drop `setup-java` and call `./kotlin build`/`check` (the wrapper provisions its own JDK).

### PR description (for `migrate/kotlin-toolchain` → `main`)

> **build: migrate from Gradle to Kotlin Toolchain**
>
> Replaces the Gradle build with the Kotlin Toolchain (Amper engine, CLI 0.11.0) driven by `./kotlin`. KMP layout: `shared` (`kmp/lib`) + per-target app modules (`androidApp`, `desktopApp`, `webApp`, `iosApp`) + two local plugins (`plugins/secrets`, `plugins/sqldelight`). `gradle/libs.versions.toml` is reused verbatim as the `$libs.*` catalog; a stub `build.gradle.kts` remains for Dependabot.
>
> See the "What replaces what", "Preserved", and "Deferred / follow-ups" tables above. Test plan = the Results table (all four targets build; JVM tests 132/133).
</content>
</invoke>

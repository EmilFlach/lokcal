# Kotlin Toolchain — migration findings & feedback

Notes from migrating **Lokcal** (a Kotlin Multiplatform + Compose Multiplatform app
targeting Android, iOS, Desktop/JVM, Web/WASM) from Gradle to the Kotlin Toolchain.
Intended for the JetBrains Kotlin Toolchain team.

- **CLI version:** `Kotlin Toolchain 0.11.0 (35ef359, 2026-05-20)` (SDKMAN `kotlintoolchain`)
- **Host:** macOS 26.5, Apple M4 Max; Xcode toolchain present; Android SDK at `~/Library/Android/sdk`
- **Branch:** `migrate/kotlin-toolchain` (12 commits; `main` keeps the Gradle build)
- **Status:** all four targets build; iOS runs on simulator; JVM tests 132/133. Migration is functionally complete.
- **This is a living document** — we expect to add findings as we fix more. Commit hashes are short refs on the branch above.

The project stack that exercised these paths: Compose Multiplatform UI + resources, SQLDelight
(`generateAsync`, native + web-worker drivers), Ktor, Coil, Health Connect, kscan, a hand-maintained
iOS Xcode project with native SwiftUI consuming the Kotlin framework, and a WASM target with an
`sql.js` web-worker.

---

## Inputs provided by the human (for reproducibility)

The agent did not discover everything unaided; these inputs shaped the work:

1. **Colleague's agent skills** — <https://github.com/singleton11/kotlin-toolchain-skills/> (the four
   `kotlin-toolchain*` / `gradle-to-kotlin-toolchain*` skills). Installed under `.claude/skills/`.
2. **The product-types doc page** — <https://kotlin-toolchain.org/latest/user-guide/product-types/>
   (pasted in). Corrected an early (wrong) assumption that the Toolchain couldn't cover all of
   Lokcal's targets; the platform hierarchy clearly does.
3. **A reusable SQLDelight Toolchain plugin** from a previous project of the human's
   (`amper-playground/sqldelight-plugin`) — vendored and extended (see §3). Without this, the
   SQLDelight codegen would have been built from scratch.
4. **The iOS simulator crash report** (`.ips`) for the startup crash — provided the stack that
   pinpointed the missing-Compose-resources cause (§9).

---

## Severity legend
🐞 likely bug · 🧩 missing capability / gap · ⚠️ surprising/undocumented behavior (papercut) · 📄 docs

---

## Findings

### 1. 📄🧩 No `gradle-kmp` layout in 0.11.0 — KMP migrations must move every source file
**Commit:** `580b746`
The only `layout:` values accepted are `maven-like` and `amper`. Older Amper / Gradle-based docs
describe a `gradle-kmp` layout that keeps the Gradle KMP source tree (`src/commonMain/kotlin`,
`src/androidMain/kotlin`, …) **and** generates Compose-resource accessors. That value is rejected:

```
module.yaml:5:9: Unknown value `gradle-kmp`. Expected one of: [`maven-like`, `amper`]
```
A top-level `module:` key (also shown in some docs) is rejected too: `Unknown property `module``.
`maven-like` only helps `jvm/*`. So a KMP project must physically relocate **all** sources into the
`amper` layout (`src/`, `src@<platform>/`, `composeResources/`, tests → `test/` + `test@<platform>/`).

**Ask:** a `gradle-kmp` (or `maven-like`-for-KMP) layout would make KMP migrations dramatically
cheaper and lower-risk. At minimum, align the docs with what 0.11.0 accepts.

### 2. ⚠️📄 `plugin.yaml` task-action specifier: `!<fqn>` is parsed literally; only `!fqn` works
**Commit:** `d6d755b`
The plugin-authoring material shows `action: !<fully.qualified.fn>`. With the angle-bracket
(YAML verbatim-tag) form, 0.11.0 takes the brackets as part of the name and fails:

```
plugin.yaml: The task action function specifier `<…​.generateSecrets>` doesn't correspond to any
available `@TaskAction`-annotated top-level functions. Available task action functions: generateSecrets
plugin.yaml: Unable to resolve `outputDir` on a non-mapping type `TaskAction`
```
The bare form `action: !com.example.generateSecrets` works. The second error is a confusing
cascade from the first. **Ask:** accept/repair the `!<…>` form or fix the docs+error.

### 3. 🧩 No native SQLDelight (codegen) support → reach into Gradle-plugin internals
**Commit:** `e82c68a`
There is no first-class SQLDelight integration, so codegen is a local `jvm/amper-plugin` that drives
SQLDelight's **Gradle-plugin internal** classes (`SqlDelightEnvironment`,
`SqlDelightDatabasePropertiesImpl`, `SqlDelightCompilationUnitImpl`) directly. This works well via
`generated.sources` (verified across all targets, incl. `generateAsync=true` for the web-worker
driver), but it depends on `app.cash.sqldelight:gradle-plugin` internals — fragile across SQLDelight
versions. **Ask:** guidance/examples for code-generators that today only ship a Gradle plugin; or a
sanctioned codegen entry point.

### 4. 🧩📄 Compose catalog is missing keys the docs imply, with a misleading error
**Commit:** `580b746`
`$compose.materialIconsExtended` and `$compose.components.uiToolingPreview` are referenced in Amper
docs but are **not** in the 0.11.0 `$compose.*` catalog. The error is misleading — it says Compose is
disabled even though `compose: enabled` is set:

```
Compose is disabled and dependency `compose.materialIconsExtended` is not available.
Set `compose.enabled` to `true` to activate the Compose library catalog.
```
(material-icons-extended was dropped by CMP after 1.7.3, which the Gradle `compose.materialIconsExtended`
accessor pinned implicitly — so this also silently changes behavior on migration.) Workaround: a
catalog entry pinned to `org.jetbrains.compose.material:material-icons-extended:1.7.3`.
**Ask:** fix the error text (don't claim Compose is disabled when it isn't); document which `$compose.*`
keys exist in this version.

### 5. ⚠️ Compose-resource accessor package is discoverable only by digging
**Commit:** `580b746`
To keep existing imports (`lokcal.shared.generated.resources.Res`), the accessor package is set via
`settings.compose.resources.packageName` + `exposedAccessors: true`. These weren't easy to find from
the CLI/docs (found by grepping the distribution jars). **Ask:** surface these in DSL docs / `--help`.

### 6. 🐞🧩 iOS: no way to add app-target linker flags; `linkerOptions` doesn't reach the app link
**Commit:** `87d20ed`
The `KotlinModules` framework is **static**, so the app must link system libs the framework references.
SQLDelight's native driver (sqliter) needs `-lsqlite3` (Gradle's `sqldelight { linkSqlite }`). Neither
`settings@ios.kotlin.linkerOptions` on the library module nor `settings.kotlin.linkerOptions` on the
`ios/app` module reached the app link — the generated `module.xcodeproj` had **no** `OTHER_LDFLAGS`,
and the app link failed:
```
❌ "_sqlite3_open_v2", referenced from: … in KotlinModules[2](KotlinModules.framework.o)
❌ ld: symbol(s) not found for architecture x86_64
```
Only a **manual** `OTHER_LDFLAGS = -lsqlite3` edit in the generated pbxproj fixed it.
**Ask:** a supported way to pass app-target linker flags (a `linkSqlite`-style toggle, or have
`linkerOptions` propagate to the iOS app link / `OTHER_LDFLAGS`).

### 7. 🐞 iOS: generated `Info.plist` lacks `CFBundle*` keys → app won't install
**Commit:** `e6bf145`
A freshly generated `ios/app` (Amper creates `module.xcodeproj` + uses `src/Info.plist`) produced an
`.app` with **no bundle identifier**, so the simulator refused it:
```
Simulator device failed to install the application. Missing bundle ID.
Failed to get bundle ID from …/iosApp.app
```
The Amper-generated project doesn't inject `CFBundleIdentifier`/`CFBundleExecutable`/etc., and the
KMP-wizard `Info.plist` relied on the old Xcode project to supply them. Fix: add the standard
`CFBundle*` keys (wired to build-setting vars) + a real `PRODUCT_BUNDLE_IDENTIFIER`.
**Ask:** a generated default `ios/app` should be installable out of the box (inject `CFBundle*`, or
set `GENERATE_INFOPLIST_FILE`).

### 8. 🐞⚠️ iOS: dependency Compose resources aren't bundled unless `compose: enabled` on the `ios/app` module
**Commits:** `5ca5513` (fix), diagnosed from the human's crash report
With Compose enabled only on the library (`shared`) and **not** on the `ios/app` module, the app built
and launched but crashed at first composition:
```
Uncaught Kotlin exception: org.jetbrains.compose.resources.MissingResourceException:
  Missing resource with path: …/iosApp.app/compose-resources/composeResources/
  lokcal.shared.generated.resources/drawable/noise.png
```
The `.app` had **no `compose-resources/` directory at all** (Android bundled them fine). Adding
`settings.compose: enabled` to the `ios/app` module made Amper package the dependency's resources
(16 files) and the app then ran. **Ask:** bundle Compose resources from dependencies that declare
them regardless of the app module's own `compose` flag — or emit a diagnostic. This is easy to miss
because it builds + launches and only crashes at runtime.

### 9. ⚠️ iOS: generated project is `module.xcodeproj` (target `app`) and is only created when absent
**Commit:** `87d20ed`
The auto-generated project is literally named `module.xcodeproj` with target `app` (not
`<moduleName>.xcodeproj`). It's generated only when missing and **not regenerated** afterward — so
`module.yaml` setting changes silently don't reflect until you delete it. This caused a real
"changed the setting, nothing happened" detour (a stale cached project). **Ask:** document the name +
regeneration behavior; consider reflecting setting changes into an existing generated project, or
warning when it's stale.

### 10. ⚠️ `./kotlin build -m <iosApp>` doesn't reinstall on the simulator
Minor but cost time: after rebuilding, `xcrun simctl launch` runs the **previously installed** app, so
a fixed build appeared to "still crash." `./kotlin run -m iosApp` (build→install→launch) is the right
loop. **Ask:** a note in docs, or have `run` always reinstall the just-built bundle (it may already;
the trap was mixing `build` + manual `simctl launch`).

### 11. 🐞 `compose.uiTest` on JVM fails to load Skiko native lib
**No commit (open).** `./kotlin test -m shared -p jvm` passes 132/133; the one failure is the Compose
UI test:
```
org.jetbrains.skiko.LibraryLoadException: Cannot find libskiko-macos-arm64.dylib.sha256,
proper native dependency missing.
```
The `.dylib` resolves but its `.sha256` sidecar doesn't — looks like a Toolchain test-runtime/skiko
provisioning gap rather than a test bug. Not yet root-caused.

### 12. 🐞🧩 `wasm-js/app`: no runnable/servable browser bundle is produced (the single biggest gap)
**Commit:** `deploy.sh` disabled; `webApp/resources/index.html` entry corrected.
This is the most impactful finding and **supersedes earlier drafts of §12/§13** (see the Correction
note below). For a Compose `wasm-js/app`:

- `./kotlin run -m webApp` is rejected: *"Module 'webApp' of type 'wasm-js/app' cannot be run directly
  by the Kotlin Toolchain at the moment."* No `run`, no `package`, no dev server (docs: "incomplete preview").
- A **clean** `./kotlin build -m webApp` produces **only** `build/tasks/_webApp_linkWasmJs/webApp.{mjs,wasm}`
  (+ `import-object`/`js-builtins`). `./kotlin show tasks` confirms the terminal task is `:webApp:linkWasmJs`
  — there is **no** webpack / browser-distribution / bundle task.
- It does **not** assemble anything servable: **no skiko** (`skiko.mjs/.wasm`, required for Compose to
  render), **no npm install** (`build/wasm/` doesn't even exist after a clean build → no `sql.js`),
  **no `index.html` wiring**, and the prepared Compose resources
  (`build/artifacts/PreparedComposeResourcesDirArtifact/…`) are never merged into a web root.

So a Compose web app **cannot be run or deployed** from Toolchain 0.11.0 today — you only get the raw
linked module. (For comparison, Gradle's `wasmJsBrowserDistribution` assembled skiko + npm + resources
+ a wired `index.html` into one servable dir.)

**Asks (in priority order):** (a) a browser-distribution step that assembles a servable bundle
(skiko + resources + a wired index + the linked module); (b) stage npm-shipped runtime **assets** into
that bundle — e.g. the SQLDelight web-worker's `sqljs.worker.js` does `importScripts("./sql-wasm.js")`
and `locateFile`s `sql-wasm.wasm`, which Gradle's `copy-webpack-plugin` (`devNpm(...)` +
`webpack.config.d`) handled; (c) a `run`/dev-server for browser apps. Until then there's no clean
local-run story for a Compose web app.

> **Correction (for the record):** earlier notes here claimed a servable bundle existed at
> `build/wasm/packages/Lokcal-shared/kotlin/` and that staging `sql.js` made `./kotlin build -m webApp`
> serveable. That directory was a **stale artifact** from an earlier broad build; it is **not**
> reproduced by a clean `build -m webApp`. The web app has only ever compiled/linked under Toolchain,
> never rendered in a browser. The `deploy.sh` change that copied `sql.js` into that path has been
> reverted.
>
> **Workaround (now in the repo):** `webApp/scripts/assemble-web.sh` reproduces the missing
> distribution step by hand — it gathers the linked `webApp.*`, extracts version-matched
> `skiko.mjs/.wasm` from `skiko-js-wasm-runtime-<resolved-ver>.jar` in the Toolchain m2
> cache, `npm pack`s `sql.js`, copies `webApp/resources/*` (incl. the committed
> `sqljs.worker.js` + wired `index.html`), and lays Compose resources at
> `composeResources/lokcal.shared.generated.resources/…` — into `build/web-dist`.
> `webApp/scripts/run-web.sh` serves it; `webApp/scripts/deploy-web.sh` uses it. Verified: every asset the
> compiled app requests returns `200` (entry, skiko, sql.js engine+worker, drawables,
> `.cvr` strings, seed CSVs). This is ~50 lines of glue that the toolchain should own.
> Also wrapped as Toolchain commands via a local plugin (`plugins/webdist`):
> `./kotlin do assembleWeb` and `./kotlin do runWeb` (the `@TaskAction` shells the
> scripts; the nested `./kotlin build -m webApp` inside a `./kotlin do` works fine).
>
> **Sub-finding — bare npm specifiers don't resolve in the browser.** Even with the
> bundle assembled, the app threw `TypeError: Failed to resolve module specifier
> "@js-joda/core"` at runtime: Kotlin/Wasm emits bare ESM imports (here from
> kotlinx-datetime → `@js-joda/core`, version `3.2.0` per the kotlinx-datetime klib),
> which browsers can't resolve without a bundler or an **import map**. The missing
> distribution step is what would normally handle this. Fix in the assembler: stage
> `@js-joda/core`'s ESM as `js-joda.mjs` and add `<script type="importmap">` mapping
> `@js-joda/core` → `./js-joda.mjs` in `index.html`. A real distribution step must
> emit an import map (or bundle) for every bare specifier in the linked output.

### 13. 🐞🧩 Android: `run` provisions its own emulator (downloading a system image already installed) instead of using a running device — ~8 min first run
**No commit (tooling behavior).** `./kotlin run -m androidApp` took ~8 minutes. The build itself was
~14s; the rest was `:androidApp:installSystemImageAndroid` downloading
`sys-img/google_apis/arm64-v8a-35_r09.zip` into Amper's **private** cache
(`~/Library/Caches/JetBrains/Kotlin/download.cache/`, now 2.5 GB) and booting a fresh AVD — even though:
- the user's SDK **already has** `system-images;android-35;google_apis;arm64-v8a` installed, and
- a device was **already running** (`adb devices` → `emulator-5554 device`).

So `run` neither reused the installed system image nor the running emulator. It's a one-time download
(cached now), but it's a rough first impression and wasteful. **Ask:** reuse the configured Android
SDK's system images, and/or target an already-running device/emulator (an `adb`-visible device) rather
than always provisioning a managed AVD; at minimum make the managed-emulator path opt-in.

**Fast workaround** (seconds, uses your running emulator):
```sh
./kotlin build -m androidApp
adb install -r build/tasks/_androidApp_buildAndroidDebug/gradle-project-debug.apk
adb shell am start -n com.emilflach.lokcal.androidApp/com.emilflach.lokcal.AppActivity
```
(Verified: installs to `emulator-5554`, `topResumedActivity` = the app's `AppActivity`.)

Minor co-occurring warnings (mostly host-side, noted for completeness): `SDK XML versions up to 3 but
… version 4 was encountered` (embedded SDK tooling older than installed cmdline-tools), and a
duplicate `platform-tools-2` location on this machine.

---

### 14. 🐞 Intermittent `wasm-js` compile/link failure (`DisposableZipFileSystemAccessor.dispose`)
**No commit (flaky).** `./kotlin build -m webApp` (and `compileWasmJs`/`linkWasmJs`) occasionally fails
with a Kotlin compiler exception during klib disposal:
```
:webApp:compileWasmJs … at org.jetbrains.kotlin.cli.common.DisposableZipFileSystemAccessor.dispose(klibArguments.kt:131)
ERROR: Task ':webApp:compileWasmJs' failed: Kotlin WASM_JS compilation failed
```
No source changed between runs; a plain retry succeeds. Seen ~3× in this project. Looks like a
concurrency/disposal race in the wasm klib handling rather than a project issue. **Ask:** investigate
the disposal race; it makes wasm builds (and the `runWeb` workaround) non-deterministic.

## What worked well (worth keeping / advertising)

- **WASM npm deps auto-resolved.** The web-worker SQLDelight driver pulled `sql.js` automatically
  from klib npm metadata — **no manual `npm()`/`devNpm()` declarations** (Gradle required them).
  Pleasant surprise. (Caveat: there's no browser bundle to put them in yet — see §12.) (`191bb6b`)
- **Android "just worked"** via the embedded Gradle/AGP, only needing `ANDROID_HOME`. (`55ea88d`)
- **Version-catalog reuse**: `gradle/libs.versions.toml` consumed verbatim as `$libs.*`; only a stub
  `build.gradle.kts` is needed for Dependabot. (`487ed07`)
- **Local-plugin codegen pipeline** (`@TaskAction` → `generated.sources`) is clean and worked
  first-try for both a from-scratch plugin (secrets, `d6d755b`) and the vendored SQLDelight one
  (`e82c68a`).
- **`./kotlin show modules` + most model-validation errors** are clear and point at file:line.
- **iOS end-to-end is genuinely achievable**: Amper generated the Xcode project, ran the framework
  build via the `Build Kotlin with Amper` script phase, and the app runs natively on the simulator.

---

## Commit index (branch `migrate/kotlin-toolchain`)

| Commit | What |
|---|---|
| `6190728` | Scaffold: wrapper, `project.yaml`, inventory, skills |
| `d6d755b` | `secrets` local plugin (ports a buildSrc codegen task) — §2 |
| `e82c68a` | Vendored `sqldelight` local plugin + `generateAsync` — §3 |
| `580b746` | `shared` → amper layout; deps→`$libs`/`$compose`; JVM/desktop builds — §1, §4, §5 |
| `55ea88d` | `androidApp` → `android/app` |
| `191bb6b` | `webApp` → `wasm-js/app` (npm auto-resolve) |
| `87d20ed` | `iosApp` → `ios/app`; all four build — §6, §9 |
| `487ed07` | Remove Gradle files; keep catalog + Dependabot stub |
| `38a77e7` | Docs: CLAUDE.md + MIGRATION_PLAN |
| `e6bf145` | iOS `Info.plist` `CFBundle*` + bundle id — §7 |
| `5ca5513` | iOS `compose: enabled` so resources bundle — §8 |
| `0ca9490` | Docs: iOS verified running + the two iOS fixes |

See `MIGRATION_PLAN.md` for the full before/after, what-replaces-what table, and remaining follow-ups.

---

## Open / to revisit (may add findings here later)
- Root-cause the Skiko `.sha256` test-runtime failure (§11).
- WASM: no servable browser bundle is produced by Toolchain (§12). Worked around by `webApp/scripts/assemble-web.sh` (run via `./kotlin do runWeb`); revisit when the `wasm-js/app` preview gains a real distribution step so the hand-rolled assembler can be retired.
- Re-check whether `linkerOptions` is *supposed* to reach the iOS app link (§6) — if so, that's a bug to file with a minimal repro.
- Whether dependency Compose-resource bundling on iOS (§8) is intended to require the app-module `compose` flag.

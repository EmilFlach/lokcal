# Compose Multiplatform Application

## Before running!
 - check your system with [KDoctor](https://github.com/Kotlin/kdoctor)  
 - install JDK 17 or higher on your machine  
 - add `local.properties` file to the project root and set a path to Android SDK there  

### Android
To run the application on android device/emulator:  
 - open project in Android Studio and run imported android run configuration  

To build the application bundle:  
 - run `./gradlew :composeApp:assembleDebug`  
 - find `.apk` file in `composeApp/build/outputs/apk/debug/composeApp-debug.apk`  

Run android UI tests on the connected device: `./gradlew :composeApp:connectedDebugAndroidTest`

### Desktop
Run the desktop application: `./gradlew :composeApp:run`  
Run the desktop **hot reload** application: `./gradlew :composeApp:hotRunJvm`  
Run desktop UI tests: `./gradlew :composeApp:jvmTest`  

### iOS
To run the application on iPhone device/simulator:  
 - Open `iosApp/iosApp.xcproject` in Xcode and run standard configuration  
 - Or use [Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) for Android Studio  

Run iOS simulator UI tests: `./gradlew :composeApp:iosSimulatorArm64Test`  

### Wasm Browser (Alpha)
Run the browser application: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continue`  
Run browser UI tests: `./gradlew :composeApp:wasmJsBrowserTest`  


## Lokcal Calorie Tracker — Product Requirements

Last updated: 2025-08-22

Overview
- Lokcal is a fully local, privacy-first calorie tracking app built with Kotlin Multiplatform and Compose Multiplatform.
- Initial ingredient data is seeded from a bundled ingredients.json and migrated into a local SQLDelight database on first launch.
- Users can add their own foods/ingredients over time, and track meals (breakfast, lunch, dinner, snacks) quickly with minimal taps/clicks.
- Nightly automatic backups keep data safe, with optional cloud copy (e.g., Google Drive) while preserving local-first operation.

Goals
- Minimize time and interactions required to log daily intake.
- Ensure offline-first reliability with fast local storage and instant UI responses.
- Protect user privacy by keeping data local by default and giving explicit control over backups.

Functional Requirements
1. Ingredient Data and Seeding
   - The app ships with ingredients.json bundled in the app (commonMain resources). On first run:
     - Parse and insert all ingredients into the SQLDelight database in a single or batched transaction.
     - Mark the seed version to avoid reseeding on subsequent launches.
   - Display and use only the SQL database after seeding (ingredients.json is not read again except for fresh installs or resets).

2. Ingredient Management (CRUD)
   - Create: Users can add new custom ingredients with name, brand (optional), default unit(s), calories per unit/100g, and optional macros (protein, carbs, fat) if supported.
   - Read: Searchable list with incremental results; quick filters (recently used, frequently used).
   - Update: Edit any ingredient’s properties; maintain referential integrity with historical logs (existing meal entries remain unchanged, or store snapshot of nutritional values on entry when needed).
   - Delete: Allow safe deletion of custom ingredients; if an ingredient is referenced by entries, either prevent deletion or soft-delete and hide from search.

3. Meal Tracking
   - Meal Types: Breakfast, Lunch, Dinner, Snacks (fixed set for v1).
   - Entry: Add items to a meal by searching or picking from shortcuts:
     - Same as yesterday: One-click action to copy yesterday’s items for the same meal period to today.
     - Leftovers: One-click to copy remaining portion from a previous day’s meal (user can pick the source day/meal and adjust quantity).
     - Frequently tracked: A section showing top-N items used in the last N days for the selected meal period.
   - Quantities and Units: Support grams and common units (piece, slice, cup, tbsp, tsp) with per-unit calorie mapping where applicable.
   - Editing: Adjust quantities inline; quick remove; undo within several seconds after deletion.
   - Notes: Optional short text note per meal entry (e.g., “no dressing”).

4. Daily Summary and History
   - Show daily totals (kcal) and optionally macros if data available.
   - Navigate by day with swipe or arrow keys (desktop/web).
   - Calendar/History view to browse and duplicate meals from past days.

5. Backup and Restore
   - Nightly local backup of the SQLDelight database (and metadata) to app-private storage with retention (e.g., keep last 7 nightly backups).
   - Optional cloud backup (e.g., Google Drive on Android):
     - User opt-in, with clear disclosure; runs after local backup completes.
     - Handle connectivity gracefully; retry on next window if the device is offline.
   - Manual backup and restore flow:
     - Export backup file to user-selected location.
     - Import from a selected backup file; validate version and data integrity; confirm overwrite.

6. Settings
   - Backup schedule enable/disable and time-of-day selection.
   - Cloud backup provider enable/disable (e.g., Google Drive), account selection (platform-specific UI where applicable).
   - Units/preferences (metric/imperial), default meal order, enable/disable macros.
   - Data reset: Clear all app data and re-run initial seeding on next launch (with confirmation and warning).

7. Search and Shortcuts
   - Search with debounce and immediate results from local DB.
   - Keyboard shortcuts on desktop/web for adding entries, navigating days, and confirming actions.
   - Recent and frequent chips/buttons for quick add.

Non-Functional Requirements
- Performance:
  - Logging a typical meal (3–5 items) should be achievable in under 10 seconds by an experienced user.
  - Search results should appear within 100 ms for local datasets up to 10k ingredients.
  - App launch to usable state under 1.5 s on modern devices after first run.
- Offline:
  - All core features work fully offline. Backups defer until connectivity is available when using cloud providers.
- Privacy & Security:
  - All data stored locally by default; no analytics or remote sync unless user opts into cloud backup.
  - Backups stored in app-private storage; when exporting, inform user of sensitivity.
- Reliability:
  - Nightly backups run even if the app isn’t in the foreground (subject to platform constraints; e.g., Android WorkManager, iOS background tasks, desktop cron-like scheduling during app runtime).
  - Backup files include version metadata to support future migrations.
- Portability:
  - Implemented with SQLDelight common schema and platform-specific drivers via DriverFactory.

Data and Persistence
- Schema: Defined under composeApp/src/commonMain/sqldelight/... (Food.sq and related tables). Include tables for Ingredients, Meals, MealEntries, and any junction tables required for units and frequencies.
- Seeding:
  - Read ingredients.json on first app launch only.
  - Insert into DB in a transaction; record seed version/time in a Settings/Meta table.
- Migrations:
  - Use SQLDelight migrations for schema changes; ensure upgrade path for existing users.
- Snapshots vs. Live Values:
  - For accurate historical totals, store snapshot nutritional values per MealEntry at time of logging, or maintain immutable versioning for ingredients.

Scheduling and Backup Details
- Nightly schedule defaults to 02:00 local time; configurable in Settings.
- Retention policy: keep last 7 nightly backups by default (configurable later).
- Backup contents: entire SQLDelight database file plus metadata (app version, schema version, timestamp) and any user attachments if added in future.
- Restore validation: check schema version compatibility and integrity before applying; show clear confirmation of data overwrite.

UX Requirements (Minimal Clicks)
- “Same as yesterday” quick action button per meal copies items and quantities; surface immediately when opening a meal with no entries.
- “Leftovers” quick action surfaces when there’s a recent matching meal with remaining portion; allow quantity slider.
- “Frequently tracked” carousel shows top items for that meal time based on last N days (configurable N, default 14).
- Inline quantity adjustment with steppers and direct input; default focus on search field when opening Add Item.
- Use sensible defaults and remember last-used units per ingredient per user.

Platform Scope
- Android, iOS, Desktop (JVM), Web (Wasm/JS) using Compose Multiplatform UI.
- SQLDelight drivers provided per platform via DriverFactory.

Acceptance Criteria
- First launch imports bundled ingredients.json into SQLDelight; a second launch does not re-import.
- User can add, edit, and delete custom ingredients; deleted ingredients do not appear in search if soft-deleted.
- Users can log meals with search, “same as yesterday,” “leftovers,” and “frequently tracked” shortcuts available and functioning.
- Nightly local backups are created and rotated by retention policy; optional cloud backup succeeds when enabled.
- Manual export/import works with integrity checks and user confirmations.
- All features function offline (except cloud upload which queues/retries gracefully).

Out of Scope (v1)
- Account-based sync across devices (beyond optional cloud backup of a single device’s DB file).
- Community-shared ingredient databases or online catalogs.

Developer Notes
- Build and run:
  - Desktop (JVM): ./gradlew :composeApp:run
  - Android: ./gradlew :composeApp:installDebug (then launch) or use Android Studio
  - Wasm/JS: ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
  - iOS: open iosApp/iosApp.xcodeproj in Xcode and run a scheme
- Persistence:
  - See SQLDelight schema under composeApp/src/commonMain/sqldelight/…
  - Seed data lives at composeApp/src/commonMain/kotlin/com/emilflach/lokcal/data/ingredients.json
- Testing:
  - JVM tests: ./gradlew :composeApp:jvmTest
  - iOS simulator tests: ./gradlew :composeApp:iosSimulatorArm64Test
  - Browser tests: ./gradlew :composeApp:wasmJsBrowserTest


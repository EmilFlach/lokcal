// Intentionally empty. This project is built with the Kotlin Toolchain (Amper engine),
// not Gradle. All dependency versions live in `gradle/libs.versions.toml`, which the
// Toolchain consumes natively as its `$libs.*` catalog.
//
// This stub exists so GitHub Dependabot's `package-ecosystem: gradle` detector
// discovers the project: its file fetcher requires a `build.gradle(.kts)` (or
// `settings.gradle(.kts)`) before it will scan `gradle/libs.versions.toml` for updates.
// The Kotlin Toolchain ignores this file entirely.

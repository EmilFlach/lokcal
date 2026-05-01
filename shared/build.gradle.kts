@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)

import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.gradle.ComposeHotRun
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.io.FileInputStream
import java.util.*

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.hotReload)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.aboutlibraries)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) FileInputStream(f).use { load(it as java.io.InputStream) }
}
val generateKrogerConfig by tasks.registering(GenerateSecretsTask::class) {
    packageName = "com.emilflach.lokcal.data"
    objectName = "KrogerConfig"
    secrets.set(mapOf(
        "CLIENT_ID" to localProps.getProperty("kroger.clientId", ""),
        "CLIENT_SECRET" to localProps.getProperty("kroger.clientSecret", ""),
    ))
    outputDir = layout.buildDirectory.dir("generated/secrets/commonMain/kotlin")
}

kotlin {
    android {
        namespace = "com.emilflach.lokcal"
        compileSdk = 36
        minSdk = 26
        
        androidResources {
            enable = true
        }
        
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")

    jvm()

    wasmJs {
        browser {}
        binaries.executable()
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            // Include the large JSON as a resource without bundling .kt sources
            resources.srcDirs("src/commonMain/resources", "src/commonMain/kotlin")
            resources.exclude("**/*.kt")
            kotlin.srcDir(generateKrogerConfig)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.ui.backhandler)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.coil)
            implementation(libs.coil.network.ktor)
            implementation(libs.kotlinx.datetime)
            implementation(libs.sqlDelight.primitive.adapters)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.compose.charts)
            implementation(libs.kscan)
            implementation(libs.haze)
            implementation(libs.aboutlibraries.compose.m3)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.androidx.activityCompose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqlDelight.driver.android)
            implementation(libs.slf4j.api)
            implementation(libs.slf4j.simple)
            implementation(libs.androidx.documentfile)
            implementation(libs.androidx.work.runtime)
            implementation(libs.androidx.connect.client)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqlDelight.driver.sqlite)
            implementation(libs.slf4j.api)
            implementation(libs.slf4j.simple)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqlDelight.driver.native)
        }
        nativeMain.dependencies {
            implementation(libs.sqlDelight.driver.native)
        }
        wasmJsMain.dependencies {
            implementation(libs.sqlDelight.driver.js)
            implementation(libs.navigation3.browser)
            implementation(npm("sql.js", "1.12.0"))
            implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.0.2"))
            implementation(devNpm("copy-webpack-plugin", "9.1.0"))
        }

    }
}


compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Lokcal"
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("desktopAppIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("desktopAppIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("desktopAppIcons/MacosIcon.icns"))
                bundleID = "com.emilflach.lokcal.desktopApp"
            }
        }
    }
}

tasks.withType<ComposeHotRun>().configureEach {
    mainClass = "MainKt"
}

sqldelight {
    // Link system SQLite on native targets (align with sample config)
    linkSqlite.set(true)
    databases {
        create("Database") {
            // Database configuration here.
            // https://cashapp.github.io/sqldelight
            packageName.set("com.emilflach.lokcal")
            srcDirs.setFrom("src/commonMain/sqldelight")
            generateAsync.set(true)
        }
    }
}

aboutLibraries {
    collect {
        fetchRemoteLicense = false
        fetchRemoteFunding = false
    }
    export {
        outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>().configureEach {
    args.addAll(listOf("--mutex", "file:${file("../build/.yarn-mutex")}"))
}

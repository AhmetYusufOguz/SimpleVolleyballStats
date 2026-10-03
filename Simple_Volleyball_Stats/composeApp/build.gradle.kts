@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
    dependencies {
        // SQLDelight'ın içindeki bozuk sürücüyü ezmek için güncel sürücüyü zorluyoruz
        classpath("org.xerial:sqlite-jdbc:3.46.0.0")
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.0.0"
    id("app.cash.sqldelight") version "2.0.2"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.10.0")
            implementation(libs.androidx.activity.compose)
            // SQLDelight Android Driver
            implementation("app.cash.sqldelight:android-driver:2.0.2")
            // Koin Android
            implementation("io.insert-koin:koin-android:3.5.3")
        }
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:1.10.0")
            implementation("org.jetbrains.compose.foundation:foundation:1.10.0")
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation("org.jetbrains.compose.ui:ui:1.10.0")
            implementation("org.jetbrains.compose.components:components-resources:1.10.0")
            implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.10.0")
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // Koin (Dependency Injection)
            implementation("io.insert-koin:koin-core:3.5.3")
            implementation("io.insert-koin:koin-compose:1.1.2")
            implementation("io.insert-koin:koin-compose-viewmodel:1.2.0-Beta4")
            // SQLDelight
            implementation("app.cash.sqldelight:runtime:2.0.2")
            implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
            // Kotlinx Serialization
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            // Navigation (Compose Multiplatform)
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.7.0-alpha07")
            // ViewModel (Compose Multiplatform)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
            // DataStore (Settings için)
            implementation("androidx.datastore:datastore-preferences-core:1.1.1")
            // Okio (DataStore için gerekli)
            implementation("com.squareup.okio:okio:3.9.0")
        }
        iosMain.dependencies {
            // SQLDelight iOS Driver
            implementation("app.cash.sqldelight:native-driver:2.0.2")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// SQLDelight Configuration
sqldelight {
    databases {
        create("VolleyballDatabase") {
            packageName.set("org.ahmetyusufoguz.simplevolleyballstats.database")
            srcDirs.setFrom("src/commonMain/sqldelight")
            version = 1
        }
    }
    linkSqlite = false
}

android {
    namespace = "org.ahmetyusufoguz.simplevolleyballstats"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.ahmetyusufoguz.simplevolleyballstats"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation("org.jetbrains.compose.ui:ui-tooling:1.10.0")
}

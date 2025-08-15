@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
    id("aevum.app")
    id("aevum.compose")

    alias(libs.plugins.ksp)
    alias(libs.plugins.kapt)
    alias(libs.plugins.hilt)
}

hilt {
    enableAggregatingTask = true
}

android {
    namespace = "dae.aevum"

//    androidResources {
//        generateLocaleConfig = true
//    }

    defaultConfig {
        applicationId = "dae.aevum"
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        checkDependencies = true
        ignoreTestSources = true
        warningsAsErrors = true
        lintConfig = rootProject.file("lint.xml")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        animationsDisabled = true
        unitTests.isIncludeAndroidResources = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        managedDevices {
            devices {
                create<ManagedVirtualDevice>("pixel5") {
                    device = "Pixel 5"
                    apiLevel = 31
                }
                create<ManagedVirtualDevice>("nexus7") {
                    device = "Nexus 7"
                    apiLevel = 31
                }
                create<ManagedVirtualDevice>("nexus10") {
                    device = "Nexus 10"
                    apiLevel = 31
                }
            }
            groups.create("screenshotDevices") {
                targetDevices.addAll(devices.toList())
            }
        }
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    // region Internal Modules
    implementation(projects.db)
    implementation(projects.common)
    implementation(projects.network)
    implementation(projects.domain)
    implementation(projects.ui)
    // endregion

    // region Logging
    implementation(libs.timber)
    // endregion

    // region Networking
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.base)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    // endregion

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)
    kapt(libs.core.ktx)
//    ksp(libs.core.ktx)

    // region Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.nav)
    // endregion

    // region Glance AppWidget
    implementation(libs.glance.appwidget)
    // endregion

    testImplementation(libs.junit)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ui.test.junit4)
}
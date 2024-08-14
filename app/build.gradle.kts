@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.ManagedVirtualDevice


@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("aevum.app")

    alias(libs.plugins.ksp)
    alias(libs.plugins.kapt)
    alias(libs.plugins.hilt)

    alias(libs.plugins.compose.compiler)
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
//        viewBinding = true
//        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.9"
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
            devices.create<ManagedVirtualDevice>("pixel5") {
                device = "Pixel 5"
                apiLevel = 31
            }
            devices.create<ManagedVirtualDevice>("nexus7") {
                device = "Nexus 7"
                apiLevel = 31
            }
            devices.create<ManagedVirtualDevice>("nexus10") {
                device = "Nexus 10"
                apiLevel = 31
            }
            groups.create("screenshotDevices") {
                targetDevices.addAll(devices.toList())
            }
        }
    }

    ksp {
        arg("room.generateKotlin", "true")
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
    implementation(libs.hilt.compiler)
    implementation(libs.hilt.nav)
    // endregion

    testImplementation(libs.junit)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
}
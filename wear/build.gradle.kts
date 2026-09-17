plugins {
    id("aevum.app")

    alias(libs.plugins.ksp)
}

android {
    namespace = "dae.aevum.wear"

    defaultConfig {
        applicationId = "dae.aevum"
        minSdk = 30
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    lint {
        checkDependencies = true
        ignoreTestSources = true
        warningsAsErrors = true
        lintConfig = rootProject.file("lint.xml")
    }
}

dependencies {
    implementation(projects.common)

    implementation(libs.timber)
    implementation(libs.play.services.wearable)
    implementation(libs.coroutines.play.services)
    implementation(libs.datastore.preferences)

    implementation(libs.wear.tiles)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.material3)
    implementation(libs.wear.protolayout.expression)

    implementation(libs.androidx.appcompat)
    implementation(libs.compose.activity)

    testImplementation(libs.junit)
}

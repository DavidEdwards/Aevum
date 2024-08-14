@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("aevum.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kapt)
//    alias(libs.plugins.hilt)
}

ksp {
    arg("room.generateKotlin", "true")
    arg("room.schemaLocation", "$projectDir/schemas")
    allWarningsAsErrors = true
}

kapt {
    correctErrorTypes = true
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        named("test") {
            assets.srcDir(project.file("schemas"))
        }
    }
}

dependencies {
    implementation(libs.timber)

    implementation(projects.common)

    implementation(libs.hilt.android)
    implementation(libs.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)
    kapt(libs.core.ktx)
//    ksp(libs.core.ktx)
}
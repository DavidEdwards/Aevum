package dae.aevum.build_logic

import com.android.build.gradle.BaseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.baseSetup() {
    println("Applying base setup to $path")
    val libs: VersionCatalog =
        extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    println("Found libs: $libs")
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            languageVersion.set(KotlinVersion.KOTLIN_1_9)
            jvmTarget.set(JvmTarget.JVM_17)
            optIn.addAll(
                listOf(
                    "kotlin.RequiresOptIn",
                    "kotlin.ExperimentalStdlibApi",
                    "kotlin.contracts.ExperimentalContracts",
                    "kotlin.time.ExperimentalTime",
                    "kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "kotlinx.coroutines.FlowPreview",
                ),
            )
            allWarningsAsErrors.set(true)
        }
    }
    println("Configuring Kotlin project extension")
    extensions.configure<KotlinProjectExtension> {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
    println("Configuring Android base extension")
    extensions.configure<BaseExtension> {
        namespace = "dae.aevum." + path.removePrefix(":").replace(':', '.')
        println("Configuring $namespace")
        compileOptions {
            isCoreLibraryDesugaringEnabled = true
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        defaultConfig {
            multiDexEnabled = true
            minSdk = libs.findVersion("sdk-min").get().requiredVersion.toInt()
            targetSdk = libs.findVersion("sdk-target").get().requiredVersion.toInt()
        }
        compileSdkVersion(libs.findVersion("sdk-compile").get().requiredVersion.toInt())
        testOptions {
            unitTests.isReturnDefaultValues = true
            animationsDisabled = true
            unitTests.isIncludeAndroidResources = true
        }

        packagingOptions {
            with(resources.pickFirsts) {
                add("META-INF/atomicfu.kotlin_module")
                add("META-INF/core.kotlin_module")
                add("META-INF/{AL2.0,LGPL2.1}")
                add("META-INF/gradle/incremental.annotation.processors")
            }
        }
    }
    println("Adding dependencies")
    dependencies.run {
        println("Adding core dependencies")
        add("coreLibraryDesugaring", libs.findLibrary("desugar").get())
//        if (project.path != ":logging:core") {
//            add("implementation", project(":logging:core"))
//        }

        println("Adding compose dependencies")
        add("implementation", platform(libs.findLibrary("compose-bom").get()))
        add("androidTestImplementation", platform(libs.findLibrary("compose-bom").get()))

        println("Adding coroutines dependencies")
        listOf(
            "coroutines.core",
            "coroutines.android",
        ).forEach {
            println("· Adding $it")
            add("implementation", libs.findLibrary(it).get())
        }

        println("Adding test dependencies")
        add("testImplementation", libs.findBundle("testing-jvm").get())
    }
}

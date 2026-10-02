import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    `maven-publish`
}

group = "org.jaudiotagger"
version = "0.1.0-SNAPSHOT"

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm {
        // keep the bytecode runnable on older desktop JVMs
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    macosArm64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-io-core:0.9.1")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation("net.jthink:jaudiotagger:3.0.1")
        }
    }
}

android {
    namespace = "org.jaudiotagger.kt"
    compileSdk = 35
    defaultConfig {
        // android.system.Os.pread/pwrite/ftruncate need API 21
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Both jvm and native tests read audio samples from the repo's testdata directory
val testDataDir = rootProject.projectDir.resolve("testdata").absolutePath

tasks.withType<Test>().configureEach {
    environment("TESTDATA_DIR", testDataDir)
}

tasks.withType<KotlinNativeTest>().configureEach {
    environment("TESTDATA_DIR", testDataDir)
}

// Kotlin 2.4 registers prepareKotlinIdeaImport on modules; some IDE versions still
// request prepareKotlinBuildScriptModel on subprojects (it lives on the root only).
tasks.register("prepareKotlinBuildScriptModel") {
    dependsOn(tasks.named("prepareKotlinIdeaImport"))
}

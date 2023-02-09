import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("kommons-multiplatform-library-conventions")
    @Suppress("DSL_SCOPE_VIOLATION")
    kotlin("plugin.serialization") version libs.versions.kotlin.asProvider().get()
}

description = "Kommons URI is a Kotlin Multiplatform Library for handling (Data) URIs."

kotlin {

    jvmToolchain(11)
    with(javaToolchains.launcherFor(java.toolchain).get().metadata) { logger.info("Using JDK $languageVersion toolchain installed in $installationPath") }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(platform(libs.kotlinx.serialization.bom.get()))
                api("org.jetbrains.kotlinx:kotlinx-serialization-core")

                api(platform(libs.ktor.bom.get()))
                api("io.ktor:ktor-http")
                api("io.ktor:ktor-utils")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
                implementation(project(":kommons-test"))
            }
        }
    }
}

tasks.withType(KotlinJvmCompile::class).configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

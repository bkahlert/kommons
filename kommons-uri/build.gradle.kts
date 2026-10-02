import com.bkahlert.kommons.gradle.jvmBytecodeTarget

plugins {
    id("kommons-multiplatform-library-conventions")
    alias(libs.plugins.kotlin.serialization)
}

description = "Kommons URI is a Kotlin Multiplatform Library for handling (Data) URIs."

// java.net.spi.URLStreamHandlerProvider needs Java 9+
jvmBytecodeTarget(11)

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project.dependencies.platform(libs.kotlinx.serialization.bom))
            api("org.jetbrains.kotlinx:kotlinx-serialization-core")

            api(project.dependencies.platform(libs.ktor.bom))
            api("io.ktor:ktor-http")
            api("io.ktor:ktor-utils")
        }
        commonTest.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
            implementation(project(":kommons-test"))
        }
    }
}

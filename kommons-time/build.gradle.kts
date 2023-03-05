plugins {
    id("kommons-multiplatform-library-conventions")
    @Suppress("DSL_SCOPE_VIOLATION")
    kotlin("plugin.serialization") version libs.versions.kotlin.asProvider().get()
}

description = "Kommons Time is a Kotlin Multiplatform Library that extends the KotlinX multiplatform date/time library"

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(platform(libs.kotlinx.serialization.bom.get()))
                api("org.jetbrains.kotlinx:kotlinx-serialization-core")

                api(libs.kotlinx.datetime)
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

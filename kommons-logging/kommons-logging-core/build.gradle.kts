plugins {
    id("kommons-multiplatform-library-conventions")
}

description = "Kommons Logging: Core is a Kotlin Multiplatform Library with convenience features for Kotlin Logging and SLF4J"

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.logging)
                implementation(project(":kommons-core"))
                implementation(project(":kommons-debug"))
                implementation(kotlin("reflect"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(project(":kommons-test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.slf4j.api)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.logback.classic)
            }
        }
        val jsMain by getting
        val jsTest by getting
    }
}

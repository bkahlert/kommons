plugins {
    id("kommons-multiplatform-jvm-js-library-conventions")
}

description = "Kommons Logging: Core is a Kotlin Multiplatform Library with convenience features for Kotlin Logging and SLF4J"

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlin.logging)
                api(project(":kommons-core"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(project(":kommons-test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(kotlin("reflect")) { because("isCompanion") }
                api(libs.slf4j.api)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.logback.classic)
            }
        }
        val jsMain by getting
        val jsTest by getting {
            dependencies {
                implementation(project(":kommons-debug"))
            }
        }
    }
}

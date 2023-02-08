plugins {
    id("kommons-multiplatform-library-conventions")
}

description = "Kommons Core is a Kotlin Multiplatform Library that offers shared features for all Kommons modules."

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinx.datetime)
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
                implementation(libs.slf4j.simple)
                implementation(project(":kommons-exec"))
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

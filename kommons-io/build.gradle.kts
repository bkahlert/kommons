plugins {
    id("kommons-multiplatform-library-conventions")
}

description = "Kommons IO is a Kotlin Library for simpler IO handling on the JVM."

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.logging)
                implementation(project(":kommons-core"))
                implementation(project(":kommons-text"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(project(":kommons-test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
                implementation(libs.slf4j.api)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.slf4j.simple)
                implementation(project(":kommons-exec"))
            }
        }
    }
}

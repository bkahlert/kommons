plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Kommons Exec is a Kotlin Library to execute command lines and shell scripts."

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":kommons-core"))
                implementation(project(":kommons-debug"))
                implementation(project(":kommons-io"))
                implementation(project(":kommons-logging:kommons-logging-core"))
                implementation(project(":kommons-text"))
                implementation(libs.kotlin.logging)
                implementation(libs.plexus.utils)
                implementation(kotlin("reflect")) { because("get PID") }
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(project(":kommons-test"))
                implementation(libs.logback.classic)
            }
        }
    }
}

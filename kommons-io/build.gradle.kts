plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Kommons IO is a Kotlin Library for simpler IO handling on the JVM."

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kommons-core"))
                api(project(":kommons-text"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(project(":kommons-test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(project(":kommons-exec"))
            }
        }
    }
}

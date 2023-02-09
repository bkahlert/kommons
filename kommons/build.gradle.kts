description = "Features for Kotlin™ Multiplatform You Didn't Know You Were Missing"

plugins {
    id("kommons-multiplatform-library-conventions")
}

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kommons-core"))
                api(project(":kommons-text"))
                api(project(":kommons-uri"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(project(":kommons-debug"))
                api(project(":kommons-io"))
                api(project(":kommons-logging:kommons-logging-core"))
                api(project(":kommons-exec"))
            }
        }
        val jsMain by getting {
            dependencies {
                api(project(":kommons-debug"))
                api(project(":kommons-logging:kommons-logging-core"))
            }
        }
        val nativeMain by getting
    }
}

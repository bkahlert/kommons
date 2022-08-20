plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Kommons Logging SLF4J is a Kotlin Library with basic SLF4J features."

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(libs.slf4j.api)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.logback.classic)
            }
        }
    }
}

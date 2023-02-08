plugins {
    id("kommons-multiplatform-jvm-js-library-conventions")
}

description = "Kommons Debug is a Kotlin Multiplatform Library for print debugging."

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
        val jvmMain by getting {
            dependencies {
                api(kotlin("reflect"))
                api(libs.slf4j.api)
                api(project(":kommons-io"))
            }

            languageSettings.optIn("kotlin.reflect.jvm.ExperimentalReflectionOnLambdas")
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.slf4j.simple)
            }
        }
    }
}

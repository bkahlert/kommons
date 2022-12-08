plugins {
    id("kommons-multiplatform-jvm-js-library-conventions")
}

description = "Kommons Debug is a Kotlin Multiplatform Library for print debugging."

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
                implementation(project(":kommons-io"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.slf4j.simple)
            }
        }

        jvmMain.languageSettings.apply {
            optIn("kotlin.reflect.jvm.ExperimentalReflectionOnLambdas")
        }
    }
}

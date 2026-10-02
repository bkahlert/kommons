plugins {
    id("kommons-multiplatform-jvm-js-library-conventions")
}

description = "Kommons Debug is a Kotlin Multiplatform Library for print debugging."

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":kommons-core"))
            api(project(":kommons-text"))
        }
        commonTest.dependencies {
            implementation(project(":kommons-test"))
        }
        jvmMain {
            dependencies {
                api(kotlin("reflect"))
                api(libs.slf4j.api)
                api(project(":kommons-io"))
            }

            languageSettings.optIn("kotlin.reflect.jvm.ExperimentalReflectionOnLambdas")
        }
        jvmTest.dependencies {
            implementation(libs.slf4j.simple)
        }
    }
}

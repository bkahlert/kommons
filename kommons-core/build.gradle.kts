plugins {
    id("kommons-multiplatform-library-conventions")
}

description = "Kommons Core is a Kotlin Multiplatform Library that offers shared features for most Kommons modules."

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(project(":kommons-test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(kotlin("reflect")) { because("sealedSubclasses and objectInstance") }
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

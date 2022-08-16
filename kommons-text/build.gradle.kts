plugins {
    id("kommons-multiplatform-library-conventions")
}

description = "Kommons Text is a Kotlin Multiplatform Library for Unicode-aware text manipulations."

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kommons-core"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(project(":kommons-test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.icu4j)
            }
        }
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                implementation(npm("xregexp", libs.versions.xregexp.get())) { because("regex classes for char meta data") }
                implementation(npm("@stdlib/string-next-grapheme-cluster-break", libs.versions.stdlib.js.get())) { because("grapheme sequence") }
            }
        }
        val jsTest by getting
    }
}

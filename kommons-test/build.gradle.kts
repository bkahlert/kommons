plugins {
    id("kommons-multiplatform-library-conventions")
}

description = "Kommons Test is a Kotlin Multiplatform Library to ease testing."

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlin("test"))
                api(libs.kotest.assertions.core)
                api(libs.kotest.assertions.json)
                implementation(libs.kotest.common) // because("mpp.bestName")
                api(project(":kommons-core"))
                api(project(":kommons-text"))
                api(project(":kommons-time"))
                api(project(":kommons-uri"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
                api(kotlin("test-junit5"))
                api(libs.bundles.junit.jupiter)
                implementation(libs.bundles.junit.platform)
                api(project(":kommons-debug"))
                api(project(":kommons-io"))
            }

            languageSettings.optIn("kotlin.reflect.jvm.ExperimentalReflectionOnLambdas")
        }
    }
}

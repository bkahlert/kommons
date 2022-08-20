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
                implementation(libs.kotest.common)
                implementation(project(":kommons-core"))
                implementation(project(":kommons-debug"))
                implementation(project(":kommons-io"))
                implementation(project(":kommons-text"))
            }
        }
        val commonTest by getting
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
                api(kotlin("test-junit5"))
                api(libs.bundles.junit.jupiter)
                implementation(libs.bundles.junit.platform)
                implementation(project(":kommons-logging:kommons-logging-slf4j"))
            }
        }
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting

        jvmMain.languageSettings.apply {
            optIn("kotlin.reflect.jvm.ExperimentalReflectionOnLambdas")
        }
    }
}

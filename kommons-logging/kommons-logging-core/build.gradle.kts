plugins {
    id("kommons-multiplatform-jvm-js-library-conventions")
}

description = "Kommons Logging: Core is a Kotlin Multiplatform Library with convenience features for Kotlin Logging and SLF4J"

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlin.logging)
            api(project(":kommons-core"))
        }
        commonTest.dependencies {
            implementation(project(":kommons-test"))
        }
        jvmMain.dependencies {
            api(kotlin("reflect")) { because("isCompanion") }
            api(libs.slf4j.api)
        }
        jvmTest.dependencies {
            implementation(libs.logback.classic)
        }
    }
}

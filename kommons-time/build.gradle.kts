plugins {
    id("kommons-multiplatform-library-conventions")
    alias(libs.plugins.kotlin.serialization)
}

description = "Kommons Time is a Kotlin Multiplatform Library that extends the KotlinX multiplatform date/time library"

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project.dependencies.platform(libs.kotlinx.serialization.bom))
            api("org.jetbrains.kotlinx:kotlinx-serialization-core")

            api(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
            implementation(project(":kommons-test"))
        }
    }
}

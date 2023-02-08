plugins {
    id("kommons-multiplatform-library-conventions")
}

description = "Kommons Kaomoji is a Kotlin Multiplatform Library that offers Japanese style emoticons `(つ◕౪◕)つ━☆ﾟ.*･｡ﾟ"

kotlin {

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kommons-core"))
                api(project(":kommons-text"))
                api(libs.mordant)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(project(":kommons-test"))
            }
        }
    }
}

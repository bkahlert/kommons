plugins {
    id("kommons-multiplatform-library-conventions")
}

description = "Kommons Kaomoji is a Kotlin Multiplatform Library that offers Japanese style emoticons `(つ◕౪◕)つ━☆ﾟ.*･｡ﾟ"

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":kommons-core"))
            api(project(":kommons-text"))
            api(libs.mordant)
        }
        commonTest.dependencies {
            implementation(project(":kommons-test"))
        }
    }
}

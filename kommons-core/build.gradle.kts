plugins {
    id("kommons-multiplatform-library-conventions")
}

description = "Kommons Core is a Kotlin Multiplatform Library that offers shared features for most Kommons modules."

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(project(":kommons-test"))
        }
        jvmMain.dependencies {
            api(kotlin("reflect")) { because("sealedSubclasses and objectInstance") }
            implementation(libs.slf4j.api)
        }
        jvmTest.dependencies {
            implementation(libs.slf4j.simple)
            implementation(project(":kommons-exec"))
        }
        jsTest.dependencies {
            implementation(project(":kommons-debug"))
        }
    }
}

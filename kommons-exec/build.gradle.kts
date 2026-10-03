plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Kommons Exec is a Kotlin Library to execute command lines and shell scripts."

kotlin {
    sourceSets {
        jvmMain.dependencies {
            api(project(":kommons-core"))
            api(project(":kommons-debug"))
            api(project(":kommons-io"))
            api(project(":kommons-logging:kommons-logging-core"))
            api(project(":kommons-text"))
            implementation(libs.plexus.utils)
        }
        jvmTest.dependencies {
            implementation(project(":kommons-test"))
            implementation(libs.logback.classic)
        }
    }
}

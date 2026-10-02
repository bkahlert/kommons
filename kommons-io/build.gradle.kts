plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Kommons IO is a Kotlin Library for simpler IO handling on the JVM."

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":kommons-core"))
            api(project(":kommons-text"))
        }
        commonTest.dependencies {
            implementation(project(":kommons-test"))
        }
        jvmTest.dependencies {
            implementation(project(":kommons-exec"))
        }
    }
}

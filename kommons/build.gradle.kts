description = "Features for Kotlin™ Multiplatform You Didn't Know You Were Missing"

plugins {
    id("kommons-multiplatform-library-conventions")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":kommons-core"))
            api(project(":kommons-text"))
            api(project(":kommons-uri"))
        }
        jvmMain.dependencies {
            api(project(":kommons-debug"))
            api(project(":kommons-io"))
            api(project(":kommons-logging:kommons-logging-core"))
            api(project(":kommons-exec"))
        }
        jsMain.dependencies {
            api(project(":kommons-debug"))
            api(project(":kommons-logging:kommons-logging-core"))
        }
    }
}

import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

description = "Features for Kotlin™ Multiplatform You Didn't Know You Were Missing"

kotlin {
    jvm()
    js(IR) {
        browser()
        nodejs()
        yarn.ignoreScripts = false // suppress "warning Ignored scripts due to flag." warning
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kommons-core"))
                api(project(":kommons-debug"))
                api(project(":kommons-io"))
                api(project(":kommons-text"))
            }
        }
    }
}

// getting rid of missing dependency declarations; see https://youtrack.jetbrains.com/issue/KT-46466
tasks.withType<Sign>().forEach { tasks.withType<AbstractPublishToMaven>().configureEach { dependsOn(it) } }

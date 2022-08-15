import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn
import java.time.Duration

description = "Kommons Test is a Kotlin Multiplatform Library to ease testing"

kotlin {
    explicitApi()

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = libs.versions.jvm.get()
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js(IR) {
        browser {
            testTask {
                testLogging.showStandardStreams = true
                useKarma {
                    useChromeHeadless()
                    timeout.set(Duration.ofSeconds(10))
                }
            }
        }
        nodejs()
        yarn.ignoreScripts = false // suppress "warning Ignored scripts due to flag." warning
    }

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
            }
        }
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting

        all {
            languageSettings.apply {
                optIn("kotlin.ExperimentalUnsignedTypes")
                optIn("kotlin.time.ExperimentalTime")
                optIn("kotlin.contracts.ExperimentalContracts")
                optIn("kotlin.experimental.ExperimentalTypeInference")
                progressiveMode = true // false by default
            }
        }
        jvmMain.languageSettings.apply {
            optIn("kotlin.reflect.jvm.ExperimentalReflectionOnLambdas")
        }
    }
}

// getting rid of missing dependency declarations; see https://youtrack.jetbrains.com/issue/KT-46466
tasks.withType<Sign>().forEach { tasks.withType<AbstractPublishToMaven>().configureEach { dependsOn(it) } }

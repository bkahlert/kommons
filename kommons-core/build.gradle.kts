import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn
import java.time.Duration

description = "Kommons Core is a Kotlin Multiplatform Library that offers shared features for all Kommons modules."

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
                implementation(libs.kotlin.logging)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(project(":kommons-test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
                implementation(libs.slf4j.api)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.slf4j.simple)
            }
        }
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
    }
}

// getting rid of missing dependency declarations; see https://youtrack.jetbrains.com/issue/KT-46466
tasks.withType<Sign>().forEach { tasks.withType<AbstractPublishToMaven>().configureEach { dependsOn(it) } }

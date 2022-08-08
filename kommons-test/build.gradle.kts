import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

plugins {
    kotlin("multiplatform")
}

description = "Kommons Test is a Kotlin Multiplatform Library to ease testing"

kotlin {
    explicitApi()

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
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
                api("io.kotest:kotest-assertions-core:5.4.1")
                implementation("io.kotest:kotest-common:5.4.1")
                implementation(project(":kommons-debug"))
            }
        }
        val commonTest by getting
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
                api(kotlin("test-junit5"))
                implementation(project.dependencies.platform("org.junit:junit-bom:5.9.0"))
                listOf("api", "engine").forEach { api("org.junit.jupiter:junit-jupiter-$it") }
                listOf("commons", "launcher").forEach { implementation("org.junit.platform:junit-platform-$it") }
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

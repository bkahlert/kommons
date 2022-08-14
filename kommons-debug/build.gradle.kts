import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

plugins {
    kotlin("multiplatform")
}

description = "Kommons Debug is a Kotlin Multiplatform Library to help you debug"

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
                @Suppress("SpellCheckingInspection")
                implementation("io.github.microutils:kotlin-logging:2.1.23") { because("SLF4J logger API + Kotlin wrapper") }
                implementation(project(":kommons-core"))
                implementation(project(":kommons-io"))
                implementation(project(":kommons-text"))
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
                implementation("org.slf4j:slf4j-api:1.7.36") { because("logger API") }
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.slf4j:slf4j-simple:1.7.36") { because("logger implementation for tests") }

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
        jvmMain.languageSettings.apply {
            optIn("kotlin.reflect.jvm.ExperimentalReflectionOnLambdas")
        }
    }
}

// getting rid of missing dependency declarations; see https://youtrack.jetbrains.com/issue/KT-46466
tasks.withType<Sign>().forEach { tasks.withType<AbstractPublishToMaven>().configureEach { dependsOn(it) } }

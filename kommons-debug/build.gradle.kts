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
                implementation("com.ibm.icu:icu4j:71.1") { because("grapheme sequence") }
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.slf4j:slf4j-simple:1.7.36") { because("logger implementation for tests") }

            }
        }
        val jsMain by getting {
            dependencies {
                implementation(npm("xregexp", "5.1.0")) { because("code point sequence") }
                implementation(npm("@stdlib/string-next-grapheme-cluster-break", "0.0.8")) { because("grapheme sequence") }
            }
        }
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

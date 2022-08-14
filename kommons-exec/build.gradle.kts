plugins {
    kotlin("multiplatform")
}

description = "Kommons Exec is a Kotlin Multiplatform Library to execute command lines and shell scripts—locally or in a Docker Container"

kotlin {
    explicitApi()

    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            // TODO check if still needed
//            minHeapSize = "128m"
//            maxHeapSize = "512m"
//            failFast = false
//            ignoreFailures = true
        }

        // TODO centralize
        tasks.register<Test>("smokeTest") {
            group = JavaBasePlugin.VERIFICATION_GROUP
            classpath = testRuns["test"].executionSource.classpath
            testClassesDirs = testRuns["test"].executionSource.testClassesDirs
            useJUnitPlatform { includeTags("smoke") }
        }
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                @Suppress("SpellCheckingInspection")
                implementation("io.github.microutils:kotlin-logging:2.1.23") { because("SLF4J logger API + Kotlin wrapper") }
                implementation("com.github.ajalt.mordant:mordant:2.0.0-beta7")
                implementation(project(":kommons-core"))
                implementation(project(":kommons-debug"))
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
                implementation("org.slf4j:slf4j-api:1.7.36") { because("logger API") }
                implementation("org.apache.commons:commons-compress:1.21")
                implementation("org.apache.commons:commons-exec:1.3")
                implementation("org.codehaus.plexus:plexus-utils:3.4.1")

                // TODO delete
                api("io.opentelemetry:opentelemetry-api:1.5.0")
                // TODO delete
                implementation("io.opentelemetry:opentelemetry-extension-annotations:1.5.0")
                // TODO delete
                implementation("io.opentelemetry:opentelemetry-extension-kotlin:1.5.0")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.slf4j:slf4j-simple:1.7.36") { because("logger implementation for tests") }

                // TODO delete
                implementation("io.opentelemetry:opentelemetry-sdk:1.5.0")
                // TODO delete
                implementation("io.opentelemetry:opentelemetry-exporter-jaeger:1.5.0")
                // TODO delete
                implementation("io.opentelemetry:opentelemetry-exporter-logging:1.5.0")
                // TODO delete
                implementation("io.grpc:grpc-netty:1.40.1")

                implementation("io.ktor:ktor-server-core:1.6.3") {
                    because("tests needing a short-lived webserver")
                }
                implementation("io.ktor:ktor-server-netty:1.6.3") {
                    because("tests needing a short-lived webserver")
                }

                // TODO delete
                implementation(project.dependencies.platform("org.junit:junit-bom:5.9.0"))
                listOf("commons", "launcher").forEach { implementation("org.junit.platform:junit-platform-$it") }
                runtimeOnly("org.junit.platform:junit-platform-console:1.8.0-RC1") {
                    because("needed to launch the JUnit Platform Console program")
                }

                // TODO get rid off
                implementation("io.strikt:strikt-core:0.30.1")
                implementation("io.strikt:strikt-jvm:0.30.1")
            }
        }

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

plugins {
    id("kommons-multiplatform-jvm-library-conventions")
}

description = "Kommons Exec is a Kotlin Library to execute command lines and shell scripts—locally or in a Docker Container."

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
                implementation(libs.kotlin.logging)
                implementation(libs.mordant)
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
                implementation(libs.slf4j.api)
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
                implementation(libs.slf4j.simple)

                // TODO delete
                implementation("io.opentelemetry:opentelemetry-sdk:1.5.0")
                // TODO delete
                implementation("io.opentelemetry:opentelemetry-exporter-jaeger:1.5.0")
                // TODO delete
                implementation("io.opentelemetry:opentelemetry-exporter-logging:1.5.0")
                // TODO delete
                implementation("io.grpc:grpc-netty:1.40.1")

                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)

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
    }
}

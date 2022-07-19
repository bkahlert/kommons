import org.gradle.api.plugins.JavaBasePlugin.VERIFICATION_GROUP
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT

val baseUrl: String get() = "https://github.com/bkahlert/kommons"

plugins {
    kotlin("multiplatform") version "1.7.10"
    id("org.jetbrains.dokka") version "1.7.0"
    id("maven-publish")
    signing
    id("nebula.release") version "16.0.0"
}

description = "Kommons is a Kotlin Multiplatform Library, with a minimal set" +
    " of dependencies, allowing you to run Command Lines and Shell Scripts," +
    " locally or in a Docker Container—and a dozen of other features like" +
    " various builders, an improved Java NIO 2 integration, decimal and" +
    " binary units, and Unicode-related features."
group = "com.bkahlert.kommons"

// version automatically determined by nebula-release plugin
// see https://github.com/nebula-plugins/nebula-release-plugin

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    explicitApi()

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
            kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            minHeapSize = "128m"
            maxHeapSize = "512m"
            failFast = false
            ignoreFailures = true
        }

        val testTask = tasks.withType<Test>().first()
        tasks.register<Test>("smokeTest") {
            group = VERIFICATION_GROUP
            classpath = testTask.classpath
            testClassesDirs = testTask.testClassesDirs
            useJUnitPlatform { includeTags("Smoke") }
        }

        tasks.withType<Test>().configureEach {
            testLogging {
                events = setOf(SKIPPED, FAILED, STANDARD_OUT, STANDARD_ERROR)
                exceptionFormat = FULL
                showExceptions = true
                showCauses = true
                showStackTraces = true
            }
        }
    }
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.bkahlert.kommons:kommons-debug:0.13.0-SNAPSHOT") { because("require, string, and time functions; trace") }
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("com.bkahlert.kommons:kommons-test:0.5.0-SNAPSHOT") { because("JUnit defaults, testEach") }
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.apache.commons:commons-compress:1.21")
                implementation("org.apache.commons:commons-exec:1.3")
                implementation("org.codehaus.plexus:plexus-utils:3.4.1")

                api("io.opentelemetry:opentelemetry-api:1.5.0")
                implementation("io.opentelemetry:opentelemetry-extension-annotations:1.5.0")
                implementation("io.opentelemetry:opentelemetry-extension-kotlin:1.5.0")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.opentelemetry:opentelemetry-sdk:1.5.0")
                implementation("io.opentelemetry:opentelemetry-exporter-jaeger:1.5.0")
                implementation("io.opentelemetry:opentelemetry-exporter-logging:1.5.0")
                implementation("io.grpc:grpc-netty:1.40.1")

                implementation("io.ktor:ktor-server-core:1.6.3") {
                    because("tests needing a short-lived webserver")
                }
                implementation("io.ktor:ktor-server-netty:1.6.3") {
                    because("tests needing a short-lived webserver")
                }
                implementation("org.slf4j:slf4j-nop:1.7.32") {
                    because("suppress failed to load class StaticLoggerBinder")
                }

                implementation(project.dependencies.platform("org.junit:junit-bom:5.9.0-M1"))
                listOf("commons", "launcher").forEach { implementation("org.junit.platform:junit-platform-$it") }
                runtimeOnly("org.junit.platform:junit-platform-console:1.8.0-RC1") {
                    because("needed to launch the JUnit Platform Console program")
                }

                implementation("io.strikt:strikt-core:0.30.1")
                implementation("io.strikt:strikt-jvm:0.30.1")
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

tasks {
    @Suppress("UnstableApiUsage")
    withType<ProcessResources> {
        filesMatching("build.properties") {
            expand(project.properties)
        }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    description = "Generates a JavaDoc JAR using Dokka"
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    tasks.named<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtml").also {
        dependsOn(it)
        from(it.get().outputDirectory)
    }
}

publishing {
    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_PASSWORD")
            }
        }

        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/bkahlert/kommons")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {

        withType<MavenPublication>().configureEach {

            artifact(javadocJar)

            pom {
                name.set("Kommons")
                description.set(project.description)
                url.set(baseUrl)
                licenses {
                    license {
                        name.set("MIT")
                        url.set("$baseUrl/blob/master/LICENSE")
                    }
                }
                scm {
                    url.set(baseUrl)
                    connection.set("scm:git:$baseUrl.git")
                    developerConnection.set("scm:git:$baseUrl.git")
                }
                issueManagement {
                    url.set("$baseUrl/issues")
                    system.set("GitHub")
                }

                ciManagement {
                    url.set("$baseUrl/issues")
                    system.set("GitHub")
                }

                developers {
                    developer {
                        id.set("bkahlert")
                        name.set("Björn Kahlert")
                        email.set("mail@bkahlert.com")
                        url.set("https://bkahlert.com")
                        timezone.set("Europe/Berlin")
                    }
                }
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}

// getting rid of missing dependency declarations
tasks.filter { it.name.startsWith("sign") }.also { signingTasks ->
    tasks.filter { it.name.startsWith("publish") && it.name.contains("Publication") }.forEach { it.dependsOn(signingTasks) }
}

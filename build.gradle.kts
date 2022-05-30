import org.gradle.api.plugins.JavaBasePlugin.VERIFICATION_GROUP
import org.gradle.api.tasks.testing.logging.TestLogEvent

val baseUrl: String get() = "https://github.com/bkahlert/kommons"

plugins {
    kotlin("multiplatform") version "1.6.20"
    id("org.jetbrains.dokka") version "1.6.20"
    id("maven-publish")
    signing
    id("nebula.release") version "15.3.1"
}

allprojects {
    apply { plugin("maven-publish") }
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
                events = setOf(TestLogEvent.SKIPPED, TestLogEvent.FAILED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR)
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
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
                api("com.bkahlert.kommons:kommons-debug:0.3.0-SNAPSHOT")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
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

                implementation(kotlin("test-junit5"))
                implementation(project.dependencies.platform("org.junit:junit-bom:5.8.0-RC1"))
                listOf("api", "params", "engine").forEach { implementation("org.junit.jupiter:junit-jupiter-$it") }
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
//                languageVersion = "1.5"
//                apiVersion = "1.5"
                optIn("kotlin.RequiresOptIn")
                optIn("kotlin.ExperimentalUnsignedTypes")
                optIn("kotlin.time.ExperimentalTime")
                optIn("kotlin.contracts.ExperimentalContracts")
                optIn("kotlin.experimental.ExperimentalTypeInference")
                progressiveMode = true // false by default
            }
        }
    }
}

tasks.withType<ProcessResources> {
    filesMatching("build.properties") {
        expand(project.properties)
    }
}

tasks.register<Copy>("assembleReadme") {
    from(projectDir)
    into(projectDir)
    setIncludes(listOf("README.template.md"))
    rename { "README.md" }
    expand("project" to project)
    shouldRunAfter(tasks.final)
}

val dokkaOutputDir = buildDir.resolve("dokka")

tasks.dokkaHtml {
    outputDirectory.set(file(dokkaOutputDir))
    dokkaSourceSets {
        configureEach {
            displayName.set(
                when (platform.get()) {
                    org.jetbrains.dokka.Platform.jvm -> "jvm"
                    org.jetbrains.dokka.Platform.js -> "js"
                    org.jetbrains.dokka.Platform.native -> "native"
                    org.jetbrains.dokka.Platform.common -> "common"
                }
            )
        }
    }
}

val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") {
    delete(dokkaOutputDir)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(deleteDokkaOutputDir) // TODO add jsGenerateExternalsIntegrated
    from(tasks.dokkaHtml.map { it.outputs })
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
val signingTasks = tasks.filter { it.name.startsWith("sign") }
listOf(
    tasks.getByName("publishKotlinMultiplatformPublicationToMavenLocal"),
    tasks.getByName("publishJsPublicationToMavenLocal"),
    tasks.getByName("publishJvmPublicationToMavenLocal"),
).forEach {
    it.dependsOn(signingTasks)
}

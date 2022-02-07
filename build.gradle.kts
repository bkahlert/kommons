import org.gradle.api.plugins.JavaBasePlugin.VERIFICATION_GROUP
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT

val baseUrl: String get() = "https://github.com/bkahlert/kommons"

plugins {
    kotlin("multiplatform") version "1.5.30"
    id("org.jetbrains.dokka") version "1.5.0"
//    id("com.github.ben-manes.versions") version "0.39.0"
//    id("se.patrikerdes.use-latest-versions") version "0.2.17"

//    id("org.ajoberstar.grgit") version "4.1.0"
    id("maven-publish")
    signing
    id("nebula.release") version "15.3.1"
}

allprojects {
    apply { plugin("maven-publish") }
//    apply { plugin("com.github.ben-manes.versions") }
//    apply { plugin("se.patrikerdes.use-latest-versions") }

//    configurations.all {
//        resolutionStrategy.eachDependency {
//            val kotlinVersion = "1.5.30"
//            val kotlinModules = listOf(
//                "bom", "reflect", "main-kts", "compiler", "compiler-embeddable",
//                "stdlib", "stdlib-js", "stdlib-jdk7", "stdlib-jdk8", "stdlib-common",
//                "test", "test-common", "test-js", "test-junit", "test-junit5").map { "kotlin-$it" }
//            if (requested.group == "org.jetbrains.kotlin" && requested.name in kotlinModules && requested.version != kotlinVersion) {
//                println("${requested.group}:${requested.name}:$kotlinVersion  ‾͞ヽ(#ﾟДﾟ)ﾉ┌┛ ͞͞ᐨ̵  ${requested.version}")
//                useVersion(kotlinVersion)
//                because("of ambiguity issues")
//            }
//        }
//    }
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
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    @Suppress("UNUSED_VARIABLE")
    sourceSets {
        val commonMain by getting
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
                implementation("io.opentelemetry:opentelemetry-extension-annotations:1.11.0")
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

                implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.30") {
                    because("filepeek takes 1.3")
                }
            }
        }
        val jsMain by getting
        val jsTest by getting
        val nativeMain by getting
        val nativeTest by getting

        all {
            languageSettings.apply {
                languageVersion = "1.5"
                apiVersion = "1.5"
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
            displayName.set(when (platform.get()) {
                org.jetbrains.dokka.Platform.jvm -> "jvm"
                org.jetbrains.dokka.Platform.js -> "js"
                org.jetbrains.dokka.Platform.native -> "native"
                org.jetbrains.dokka.Platform.common -> "common"
            })
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
    tasks.getByName("publishNativePublicationToMavenLocal")
).forEach {
    it.dependsOn(signingTasks)
}

import org.gradle.api.plugins.JavaBasePlugin.VERIFICATION_GROUP
import kotlin.text.toBoolean as kotlinToBoolean


val camelCaseRegex = Regex("(?<lowerLeftChar>[a-z0-9]|(?=[A-Z]))(?<upperRightChar>[A-Z])")
fun CharSequence.convertCamelCase(separator: Char, transform: (String) -> String): String = camelCaseRegex
    .replace(this.toString().decapitalize(), "\${lowerLeftChar}$separator\${upperRightChar}")
    .let(transform)

fun CharSequence.camelCaseToScreamingSnakeCase() = convertCamelCase('_', String::toUpperCase)

fun String?.toBoolean(default: Boolean = false): Boolean =
    this?.run { isBlank() || kotlinToBoolean() } ?: default

fun Project.findBooleanPropertyEverywhere(name: String, default: Boolean = false): Boolean =
    findPropertyEverywhere(name).toBoolean(default)

fun Project.findPropertyEverywhere(name: String): String? =
    extra.properties[name]?.toString()
        ?: findProperty(name)?.toString()
        ?: System.getenv(name.camelCaseToScreamingSnakeCase())

fun Project.findPropertyEverywhere(name: String, defaultValue: String): String =
    findPropertyEverywhere(name) ?: defaultValue

private var _releasingFinal: Boolean? = null
var Project.releasingFinal: Boolean
    get() = _releasingFinal ?: findBooleanPropertyEverywhere("releasingFinal", true)
    set(value) {
        _releasingFinal = value
    }

val Project.baseUrl: String get() = findPropertyEverywhere("baseUrl", "https://github.com/bkahlert/koodies")

/**
 * Returns whether this object represents a final version number
 * of the format `<major>.<minor.<patch>`.
 */
fun Any.isFinal(): Boolean =
    Regex("(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)").matches(toString())


plugins {
    kotlin("multiplatform") version "1.5.0"
    id("org.jetbrains.dokka") version "1.4.30"
    id("com.github.ben-manes.versions") version "0.38.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.16"

    id("org.ajoberstar.grgit") version "4.1.0"
    id("maven-publish")
    id("signing")
    id("nebula.release") version "15.3.1"
}

allprojects {
    apply { plugin("com.github.ben-manes.versions") }
    apply { plugin("se.patrikerdes.use-latest-versions") }
    configurations.all {
        resolutionStrategy.eachDependency {

            val kotlinVersion = "1.5.0"
            val kotlinModules = listOf(
                "bom", "reflect", "main-kts", "compiler", "compiler-embeddable",
                "stdlib", "stdlib-js", "stdlib-jdk7", "stdlib-jdk8", "stdlib-common",
                "test", "test-common", "test-js", "test-junit", "test-junit5").map { "kotlin-$it" }
            if (requested.group == "org.jetbrains.kotlin" && requested.name in kotlinModules && requested.version != kotlinVersion) {
                println("${requested.group}:${requested.name}:$kotlinVersion  ‾͞ヽ(#ﾟДﾟ)ﾉ┌┛ ͞͞ᐨ̵  ${requested.version}")
                useVersion(kotlinVersion)
                because("of ambiguity issues")
            }
        }
    }
}

description = "Random Kotlin Goodies"
group = "com.bkahlert"

repositories {
    mavenCentral()
    jcenter()
}

kotlin {
    explicitApi()

    if (releasingFinal && !version.isFinal()) {
        println("\n\t\tProperty releasingFinal is set but the active version $version is not final.")
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
                freeCompilerArgs = listOf("-Xjvm-default=all")
            }
        }

        tasks.withType<Test>().all {
            useJUnitPlatform()
            minHeapSize = "128m"
            maxHeapSize = "512m"
            failFast = false
            ignoreFailures = true
        }

        val anySetUpTest = tasks.withType<Test>().first()
        tasks.register<Test>("smokeTest") {
            group = VERIFICATION_GROUP
            classpath = anySetUpTest.classpath
            testClassesDirs = anySetUpTest.testClassesDirs
            useJUnitPlatform { includeTags("Smoke") }
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

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.apache.commons:commons-compress:1.20")
                implementation("org.apache.commons:commons-exec:1.3")
                implementation("org.codehaus.plexus:plexus-utils:3.3.0")
                implementation("org.jline:jline-reader:3.19.0")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(project.dependencies.platform("org.junit:junit-bom:5.8.0-M1"))
                listOf("api", "params", "engine").forEach { implementation("org.junit.jupiter:junit-jupiter-$it") }
                listOf("commons", "launcher").forEach { implementation("org.junit.platform:junit-platform-$it") }
                runtimeOnly("org.junit.platform:junit-platform-console:1.8.0-M1") {
                    because("needed to launch the JUnit Platform Console program")
                }

                implementation("io.strikt:strikt-core:0.30.1")
                implementation("io.strikt:strikt-jvm:0.30.1")

                implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.0") {
                    because("filepeek takes 1.3")
                }
            }
        }

        val jsMain by getting
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val nativeMain by getting
        val nativeTest by getting

        all {
            languageSettings.apply {
                languageVersion = "1.5"
                apiVersion = "1.5"
                useExperimentalAnnotation("kotlin.RequiresOptIn")
                useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
                useExperimentalAnnotation("kotlin.time.ExperimentalTime")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
                progressiveMode = true // false by default
            }
        }
    }
}

val dokkaOutputDir = buildDir.resolve("dokka")

tasks.dokkaHtml {
    outputDirectory.set(file(dokkaOutputDir))
    dokkaSourceSets {
        configureEach {
            val platformName = when (platform.get()) {
                org.jetbrains.dokka.Platform.jvm -> "jvm"
                org.jetbrains.dokka.Platform.js -> "js"
                org.jetbrains.dokka.Platform.native -> "native"
                org.jetbrains.dokka.Platform.common -> "common"
            }
            displayName.set(platformName)
        }
    }
}

val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") {
    delete(dokkaOutputDir)
}

val javadocJar = tasks.create<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(deleteDokkaOutputDir, tasks.dokkaHtml)
    from(dokkaOutputDir)
}

publishing {

    repositories {
        maven {
            name = "MavenCentral"
            url = if (version.isFinal()) {
                uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            } else {
                uri("https://oss.sonatype.org/content/repositories/snapshots/")
            }
            credentials {
                username = findPropertyEverywhere("sonatypeNexusUsername", "")
                password = findPropertyEverywhere("sonatypeNexusPassword", "")
            }
        }

        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/bkahlert/koodies")
            credentials {
                username = findPropertyEverywhere("githubUsername", "")
                password = findPropertyEverywhere("githubToken", "")
            }
        }
    }

    publications {
        
        withType<MavenPublication>().configureEach {

            artifact(javadocJar)

            pom {
                name.set("Koodies")
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
    sign(publishing.publications)
}

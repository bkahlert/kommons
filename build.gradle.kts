import org.gradle.api.plugins.JavaBasePlugin.DOCUMENTATION_GROUP
import org.gradle.api.plugins.JavaBasePlugin.VERIFICATION_GROUP
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN

plugins {
    kotlin("multiplatform") version Versions.kotlin
    id("org.jetbrains.dokka") version "1.4.20"
    id("com.github.ben-manes.versions") version "0.36.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.15"
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
    id("io.gitlab.arturbosch.detekt") version "1.16.0-RC1"

    id("org.ajoberstar.grgit") version "4.1.0"
    id("maven-publish")
    id("signing")
    id("nebula.release") version "15.3.1"
    id("nebula.source-jar") version "17.3.2"
    id("nebula.javadoc-jar") version "17.3.2"
    id("nebula.nebula-bintray-publishing") version "8.5.0"
}

allprojects {
    apply { plugin("com.github.ben-manes.versions") }
    apply { plugin("se.patrikerdes.use-latest-versions") }
}

description = "Random Kotlin Goodies"
group = "com.bkahlert.koodies"

configure<KtlintExtension> {
    debug.set(true)
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(true)
    enableExperimentalRules.set(true)
//    additionalEditorconfigFile.set(file("/some/additional/.editorconfig"))
    disabledRules.set(setOf(
        "no-consecutive-blank-lines"
    ))
    reporters {
        reporter(PLAIN)
        reporter(CHECKSTYLE)
    }
    kotlinScriptAdditionalPaths {
        include(fileTree("scripts/"))
    }
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://maven.pkg.github.com/bkahlert/koodies")
        credentials {
            username = project.findPropertyEverywhere("githubUsername", "")
            password = project.findPropertyEverywhere("githubToken", "")
        }
    }
}

kotlin {
    if (releasingFinal && !version.isFinal()) {
        println("\n\t\tProperty releasingFinal is set but the active version $version is not final.")
    }

    val features = listOf(
        "-Xinline-classes",
        "-Xopt-in=kotlin.RequiresOptIn",
        "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
        "-Xopt-in=kotlin.time.ExperimentalTime",
        "-Xopt-in=kotlin.contracts.ExperimentalContracts",
        "-Xopt-in=kotlin.io.path.ExperimentalPathApi"
    )

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.freeCompilerArgs += features
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
                useIR = true
                freeCompilerArgs += listOf(
                    "-Xjvm-default=all"
                )
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
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }

        binaries.executable()
        compilations.all { kotlinOptions { sourceMap = true; moduleKind = "umd"; metaInfo = true } }
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
        all {
            features.filter { it.startsWith("-Xopt-in=") }.forEach { feature ->
                languageSettings.useExperimentalAnnotation(feature.removePrefix("-Xopt-in="))
            }
        }

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                api("com.ionspin.kotlin:bignum:0.2.3") {
                    because("bigint for IPv6Address")
                }
            }
        }
        val commonTest by getting {
            dependencies {
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

                @Suppress("SpellCheckingInspection")
                implementation("com.tunnelvisionlabs:antlr4-runtime:${Versions.antlr4}") {
                    because("grapheme parsing")
                }
                @Suppress("SpellCheckingInspection")
                implementation("com.tunnelvisionlabs:antlr4-perf-testsuite:${Versions.antlr4}")


                implementation("com.github.ajalt:mordant:1.2.1") {// implementation("com.github.ajalt.mordant:mordant:2.0.0-alpha1")
                    exclude("org.jetbrains.kotlin", "kotlin-stdlib")
                }
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit.jupiter}")
                implementation("org.junit.jupiter:junit-jupiter-params:${Versions.junit.jupiter}")
                implementation("org.junit.jupiter:junit-jupiter-engine:${Versions.junit.jupiter}")
                implementation("org.junit.platform:junit-platform-commons:${Versions.junit.platform}")
                implementation("org.junit.platform:junit-platform-launcher:${Versions.junit.platform}")
                runtimeOnly("org.junit.platform:junit-platform-console:${Versions.junit.platform}") {
                    because("needed to launch the JUnit Platform Console program")
                }

                implementation("io.strikt:strikt-core:0.28.2")
                implementation("com.christophsturm:filepeek:0.1.2")
                implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}") {
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
            features.filter { it.startsWith("-Xopt-in=") }.forEach { feature ->
                languageSettings.useExperimentalAnnotation(feature.removePrefix("-Xopt-in="))
            }
        }

        all {
            languageSettings.apply {
                languageVersion = "1.4"
                apiVersion = "1.4"
                progressiveMode = true
            }
        }

        targets.all {
            compilations.all {
                kotlinOptions.freeCompilerArgs += features
            }
        }

        all {
            features.filter { it.startsWith("-Xopt-in=") }.forEach { feature ->
                languageSettings.useExperimentalAnnotation(feature.removePrefix("-Xopt-in="))
            }
        }

        publishing {
            publications {
                withType<MavenPublication>().matching { it.name.contains("kotlinMultiplatform") }.configureEach {
                    artifact(tasks.register<Jar>("dokkaHtmlJar") {
                        group = DOCUMENTATION_GROUP
                        dependsOn(tasks.dokkaHtml)
                        from(tasks.dokkaHtml.flatMap { it.outputDirectory })
                        archiveClassifier.set("kdoc")
                    })
                }
                withType<MavenPublication>().configureEach {
                    artifact(tasks.register<Jar>("${name}JavaDocJar") {
                        archiveBaseName.set("${project.name}-${this@configureEach.name}")
                        archiveClassifier.set("javadoc")
                    })

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
        }

        signing {
            sign(publishing.publications)
        }
    }
}

// TODO https://github.com/sksamuel/hoplite/blob/master/publish.gradle.kts

tasks.configureEach {
    onlyIf {
        if (Regex(".*MavenCentral.*").matches(name)) {
            !syncToMavenCentralUsingBintray
        } else {
            true
        }
    }
}

if (version.isFinal()) {
    bintray {
        user.set(findPropertyEverywhere("bintrayUser", ""))
        apiKey.set(findPropertyEverywhere("bintrayApiKey", ""))
        userOrg.set(user.get())
        repo.set("koodies")
        pkgName.set("koodies")
        labels.set(listOf("kotlin", "builder", "shellscript", "docker",
            "integration", "java", "nio", "nio2", "kaomoji", "border",
            "box", "logger", "fixture", "time", "unicode"))
        websiteUrl.set(baseUrl)
        issueTrackerUrl.set("$baseUrl/issues")
        licenses.set(listOf("MIT"))
        vcsUrl.set("$baseUrl.git")
        gppSign.set(false)
        syncToMavenCentral.set(syncToMavenCentralUsingBintray)
        sonatypeUsername.set(findPropertyEverywhere("sonatypeNexusUsername", ""))
        sonatypePassword.set(findPropertyEverywhere("sonatypeNexusPassword", ""))
    }
}

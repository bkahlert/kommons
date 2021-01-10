import org.gradle.api.plugins.JavaBasePlugin.DOCUMENTATION_GROUP
import org.gradle.api.plugins.JavaBasePlugin.VERIFICATION_GROUP
import org.jetbrains.dokka.Platform.native
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    base
    kotlin("multiplatform") version "1.4.21"
    id("org.jetbrains.dokka") version "1.4.20"
    id("com.github.ben-manes.versions") version "0.36.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.15"

    id("org.ajoberstar.grgit") version "4.1.0"
    id("maven-publish")
    id("signing")
    id("nebula.release") version "15.3.0"
//    id("nebula.nebula-bintray") version "8.5.0"
}

allprojects {
    apply { plugin("com.github.ben-manes.versions") }
    apply { plugin("se.patrikerdes.use-latest-versions") }
}

description = "Random Kotlin Goodies"
group = "com.bkahlert.koodies"

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
        println("\n\n\t\tProperty releasingFinal is set but the active version $version is not final.")
        println("\t\tTurning releasingFinal off. To release please read RELEASING.md.\n")
        releasingFinal = false
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
                useIR = true
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xopt-in=kotlin.io.path.ExperimentalPathApi"
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

    js(BOTH) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }

        compilations.all {
            kotlinOptions {
                sourceMap = true
                moduleKind = "umd"
                metaInfo = true
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

    targets.all {
        compilations.all {
            kotlinOptions {
                @Suppress("SpellCheckingInspection")
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xopt-in=kotlin.RequiresOptIn",
                    "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
                    "-Xopt-in=kotlin.time.ExperimentalTime",
                    "-Xopt-in=kotlin.contracts.ExperimentalContracts",
                    "-Xinline-classes"
                )
            }
        }
    }

    tasks.withType<Jar>().configureEach {
        manifest {
            attributes(mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            ))
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
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
                implementation("commons-io:commons-io:2.8.0")
                implementation("org.apache.commons:commons-compress:1.20")
                implementation("org.apache.commons:commons-exec:1.3")
                implementation("org.codehaus.plexus:plexus-utils:3.3.0")
                implementation("org.jline:jline-reader:3.16.0")

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

                implementation("io.strikt:strikt-core:0.28.1")
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

        val dokkaTask = tasks.withType<DokkaTask>().configureEach {
            dokkaSourceSets {
                configureEach {
                    if (platform.get() == native) {
                        displayName.set("native")
                    }
                }
            }
        }

        tasks {
            val dokkaOutputDir = "$buildDir/dokka"
            dokkaHtml { outputDirectory.set(file(dokkaOutputDir)) }

            val deleteDokkaOutputDir by registering(Delete::class) {
                delete(dokkaOutputDir)
            }

            register<Jar>("javadocJar") {
                group = DOCUMENTATION_GROUP
                dependsOn(deleteDokkaOutputDir, dokkaHtml)
                archiveClassifier.set("javadoc")
                from(dokkaOutputDir)
            }
        }
        val dockerJavadocJar by tasks.named("javadocJar")

        val dokkaHtmlJar by tasks.register<Jar>("dokkaHtmlJar") {
            group = DOCUMENTATION_GROUP
            dependsOn(tasks.dokkaHtml)
            from(tasks.dokkaHtml.flatMap { it.outputDirectory })
            archiveClassifier.set("html-doc")
        }

        signing {
            sign(publishing.publications)
        }

        publishing {
            publications {
                withType<MavenPublication>().configureEach {

                    if (name == "kotlinMultiplatform") {
                        artifact(dockerJavadocJar)
                        artifact(dokkaHtmlJar)
                    }

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
                        // TODO
//                        ciManagement {
//                            url.set("$baseUrl/issues")
//                            system.set("GitHub")
//                        }
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
                    url = if (releasingFinal) {
                        uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                    } else {
                        uri("https://oss.sonatype.org/content/repositories/snapshots/")
                    }
                    credentials {
                        username = findPropertyEverywhere("sonatypeNexusUsername", "")
                        password = findPropertyEverywhere("sonatypeNexusPassword", "")
                    }
                }

                if (releasingFinal) {
//                    maven {
//                        name = "BintrayMaven"
//                        url = uri("https://api.bintray.com/maven/bkahlert/koodies/koodies;publish=1")
//                        credentials {
//                            username = findPropertyEverywhere("bintrayUser", "")
//                            password = findPropertyEverywhere("bintrayApiKey", "")
//                        }
//                    }
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
//
//        bintray {
//            user.set(findPropertyEverywhere("bintrayUser", ""))
//            apiKey.set(findPropertyEverywhere("bintrayApiKey", ""))
//            userOrg.set(user.get())
//            repo.set("koodies")
//            pkgName.set("koodies")
//            labels.set(listOf("kotlin", "builder", "shellscript", "docker",
//                "integration", "java", "nio", "nio2", "kaomoji", "border",
//                "box", "logger", "fixture", "time", "unicode"))
//            websiteUrl.set(baseUrl)
//            issueTrackerUrl.set("$baseUrl/issues")
//            licenses.set(listOf("MIT"))
//            vcsUrl.set("$baseUrl.git")
//            gppSign.set(false)
//            syncToMavenCentral.set(true)
//            sonatypeUsername.set(findPropertyEverywhere("sonatypeNexusUsername", ""))
//            sonatypePassword.set(findPropertyEverywhere("sonatypeNexusPassword", ""))
//        }
        }
    }
}

import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("multiplatform") version "1.4.21"
    id("org.jetbrains.dokka") version "1.4.20"
    id("com.github.ben-manes.versions") version "0.36.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.15"

    id("org.ajoberstar.grgit") version "4.1.0"
    id("maven-publish")
    id("nebula.release") version "15.3.0"
    id("nebula.nebula-bintray") version "8.5.0"
}

allprojects {
    apply { plugin("com.github.ben-manes.versions") }
    apply { plugin("se.patrikerdes.use-latest-versions") }
}

description = "Random Kotlin Goodies"
group = "com.bkahlert.koodies"

repositories {
    jcenter()
}

kotlin {
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

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            minHeapSize = "128m"
            maxHeapSize = "512m"
            failFast = false
            ignoreFailures = true
        }
    }

    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
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
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
//                implementation("com.squareup.okio:okio-multiplatform:2.9.0")
//                implementation("com.squareup.okio:okio:2.9.0")
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
                implementation("com.tunnelvisionlabs:antlr4-runtime:4.7.4") {
                    because("grapheme parsing")
                }
                @Suppress("SpellCheckingInspection")
                implementation("com.tunnelvisionlabs:antlr4-perf-testsuite:4.7.4")


                implementation("com.github.ajalt:mordant:1.2.1") {// implementation("com.github.ajalt.mordant:mordant:2.0.0-alpha1")
                    exclude("org.jetbrains.kotlin", "kotlin-stdlib")
                }
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
                implementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
                implementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
                implementation("org.junit.platform:junit-platform-commons:1.7.0")
                implementation("org.junit.platform:junit-platform-launcher:1.7.0")
                runtimeOnly("org.junit.platform:junit-platform-console:1.7.0") {
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

        tasks.withType<DokkaTask>().configureEach {
            dokkaSourceSets {
                named(commonMain.name)
                named(jvmMain.name) { samples.from(jvmMain.resources.sourceDirectories.map { "$it" }) }
                named(jsMain.name)
                named(nativeMain.name)
            }
        }
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
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/bkahlert/koodies")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

bintray {
    user.set(System.getenv("BINTRAY_USER"))
    apiKey.set(System.getenv("BINTRAY_KEY"))
    repo.set("koodies")
    pkgName.set("gradle-bintray-plugin-example")
    userOrg.set(user.get())
    licenses.set(listOf("MIT"))
    vcsUrl.set("https://github.com/bkahlert/koodies.git")
}

plugins {
    kotlin("multiplatform") version "1.4.21"
    id("org.jetbrains.dokka") version "0.10.1"
    id("maven-publish")
    id("com.github.ben-manes.versions") version "0.36.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.15"
}

allprojects {
    apply { plugin("com.github.ben-manes.versions") }
    apply { plugin("se.patrikerdes.use-latest-versions") }
}

group = "koodies"
version = "1.0.1"

repositories {
    mavenCentral()
    jcenter()
}

kotlin {
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
    }
}

//publishing {
//    repositories {
//        maven {
//            description = "Kotlin Goodies"
//            url = URI.create("http://www.example.com/library")
//            artifacts {
//                add("library") {
//                    licenses {
//                        license {
//                            name = "The Apache License, Version 2.0"
//                            url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
//                        }
//                    }
//                    developers {
//                        developer {
//                            id = "bkahlert"
//                            name = "Björn Kahlert"
//                            email = "mail@bkahlert.com"
//                        }
//                    }
//                    scm {
//                        connection = "scm:git:git://example.com/my-library.git"
//                        developerConnection = "scm:git:ssh://example.com/my-library.git"
//                        url = "http://example.com/my-library/"
//                    }
//                }
//            }
//        }
//    }
//}

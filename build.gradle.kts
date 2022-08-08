plugins {
    kotlin("multiplatform") version "1.7.10" apply false
    id("org.jetbrains.dokka") version "1.7.10" apply false
    id("nebula.release") version "16.0.0" apply false
    id("maven-publish")
    signing
}

allprojects {
    group = "com.bkahlert.kommons"
    version = "2.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {

    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

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
            @Suppress("SpellCheckingInspection")
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
                    url.set("https://github.com/bkahlert/kommons")
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://github.com/bkahlert/kommons/blob/master/LICENSE")
                        }
                    }
                    scm {
                        url.set("https://github.com/bkahlert/kommons")
                        connection.set("scm:git:https://github.com/bkahlert/kommons.git")
                        developerConnection.set("scm:git:https://github.com/bkahlert/kommons.git")
                    }
                    issueManagement {
                        url.set("https://github.com/bkahlert/kommons/issues")
                        system.set("GitHub")
                    }
                    ciManagement {
                        url.set("https://github.com/bkahlert/kommons/issues")
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
}

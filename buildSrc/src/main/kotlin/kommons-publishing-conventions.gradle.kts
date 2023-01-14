import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    signing
    id("maven-publish")
    id("nebula.release")
}

val isSnapshot = version.toString().endsWith("-SNAPSHOT")
if (isSnapshot) {
    logger.lifecycle("Snapshot version: $version")
    tasks.withType<Sign>().configureEach {
        logger.info("Disabling task $name")
        enabled = false
    }
    tasks.withType<DokkaTask>().configureEach {
        logger.info("Disabling task $name")
        enabled = false
    }
}

val releaseVersion: String? = System.getenv("RELEASE_VERSION")
if (releaseVersion != null) version = releaseVersion

val dokkaPlugin by configurations
dependencies { dokkaPlugin("org.jetbrains.dokka:versioning-plugin:1.7.10") }

val javadocJar by tasks.registering(Jar::class) {
    description = "Generates a JavaDoc JAR using Dokka"
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    tasks.named<DokkaTask>("dokkaHtml").also { dokkaHtml ->
        dependsOn(dokkaHtml)
        from(dokkaHtml.get().outputDirectory)
    }
}

val publications: PublicationContainer = (extensions.getByName("publishing") as PublishingExtension).publications

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifact(javadocJar)
    }
}

// TODO consider switching to https://github.com/gradle-nexus/publish-plugin

publishing {
    repositories {
        @Suppress("SpellCheckingInspection")
        maven {
            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            name = "OSSRH"
            url = if (releaseVersion != null) releasesRepoUrl else snapshotsRepoUrl
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

    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set(project.name.split("-").joinToString(" ") { it.capitalize() })
            // needed as subprojects aren't yet evaluated, see https://docs.gradle.org/current/userguide/publishing_maven.html
            // otherwise description of Kotlin Multiplatform publication stays empty, and repo gets refused by Maven Central
            afterEvaluate { pom.description.set(description) }
            url.set("https://github.com/bkahlert/kommons/tree/master/${project.name}")

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

            issueManagement {
                url.set("https://github.com/bkahlert/kommons/issues")
                system.set("GitHub")
            }

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://github.com/bkahlert/kommons/blob/master/LICENSE")
                }
            }

            scm {
                connection.set("scm:git:https://github.com/bkahlert/kommons")
                developerConnection.set("scm:git:https://github.com/bkahlert")
                url.set("https://github.com/bkahlert/kommons")
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

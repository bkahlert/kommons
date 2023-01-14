plugins {
    id("java-platform")
    id("maven-publish")
    signing
}

group = "com.bkahlert.kommons"

val isSnapshot = version.toString().endsWith("-SNAPSHOT")
if (isSnapshot) {
    logger.lifecycle("Snapshot version: $version")
    tasks.withType<Sign>().configureEach {
        logger.info("Disabling task $name")
        enabled = false
    }
}

val releaseVersion: String? = System.getenv("RELEASE_VERSION")
if (releaseVersion != null) version = releaseVersion

val bomProject = project

// Explicitly exclude subprojects that will never be published
// so that when configuring this project we don't force their
// configuration and do unecessary work.
val excludeFromBom: List<String> = emptyList()
fun projectsFilter(candidateProject: Project) =
    excludeFromBom.all { !candidateProject.name.contains(it) } &&
        candidateProject.name != bomProject.name

// Declare that this subproject depends on all subprojects matching the filter
// When this subproject is configured, it will force configuration of all subprojects
// so that we can declare dependencies on them
rootProject.subprojects.filter(::projectsFilter).forEach { bomProject.evaluationDependsOn(it.path) }

dependencies {
    constraints {
        rootProject.subprojects.filter { project ->
            // Only declare dependencies on projects that will have publications
            projectsFilter(project) && project.tasks.findByName("publish")?.enabled == true
        }.forEach { api(project(it.path)) }
    }
}

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
    }

    publications {
        create<MavenPublication>("KommonsBom") {
            from(components["javaPlatform"])
            pom {
                name.set("Kommons Bill of Materials")
                description.set("Features for Kotlin™ Multiplatform You Didn't Know You Were Missing")
                url.set("https://github.com/bkahlert/kommons")

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
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}

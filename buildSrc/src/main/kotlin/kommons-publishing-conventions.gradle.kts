import com.vanniktech.maven.publish.JavaPlatform
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask

plugins {
    id("com.vanniktech.maven.publish")
    id("com.netflix.nebula.release")
}

// The release workflow passes the version through the environment; otherwise nebula infers it from the Git history.
val releaseVersion: String? = System.getenv("RELEASE_VERSION")
if (releaseVersion != null) version = releaseVersion

val isSnapshot = version.toString().endsWith("-SNAPSHOT")
if (isSnapshot) {
    logger.lifecycle("Snapshot version: $version")
    tasks.withType<Sign>().configureEach {
        logger.info("Disabling task $name")
        enabled = false
    }
    tasks.withType<DokkaGenerateTask>().configureEach {
        logger.info("Disabling task $name")
        enabled = false
    }
}

mavenPublishing {
    // Automatic release and deployment validation follow the mavenCentralAutomaticPublishing and
    // mavenCentralDeploymentValidation Gradle properties. The release workflow passes the first; a publish from a
    // developer machine keeps the plugin's default and leaves the deployment to be released by hand in the Portal.
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set(project.name.split("-").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } })
        // The module's build script sets its description after this convention has run. Without the deferral the
        // Kotlin Multiplatform publication's description stays empty, and Maven Central rejects the deployment.
        val mavenPom = this
        afterEvaluate { mavenPom.description.set(project.description) }
        url.set("https://github.com/bkahlert/kommons/tree/main/${project.name}")

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
                url.set("https://github.com/bkahlert/kommons/blob/main/LICENSE")
            }
        }

        scm {
            connection.set("scm:git:https://github.com/bkahlert/kommons")
            developerConnection.set("scm:git:https://github.com/bkahlert")
            url.set("https://github.com/bkahlert/kommons")
        }
    }
}

// Kotlin Multiplatform libraries publish every target with a Dokka javadoc jar; the BOM is a java-platform.
pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    mavenPublishing {
        configure(KotlinMultiplatform(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml")))
    }
}
pluginManager.withPlugin("java-platform") {
    mavenPublishing {
        configure(JavaPlatform())
    }
}

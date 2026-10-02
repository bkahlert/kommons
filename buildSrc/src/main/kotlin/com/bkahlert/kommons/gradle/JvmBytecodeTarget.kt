package com.bkahlert.kommons.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Compiles the JVM target of this multiplatform project to bytecode of the given Java [version] (8, 11, 17, ...)
 * for Kotlin and Java sources alike, independent of the JDK toolchain the build runs on.
 *
 * Both have to agree, otherwise the Kotlin Gradle plugin's JVM target validation fails the build.
 *
 * The API level is pinned as well (`-Xjdk-release`, `--release`): the build runs on a JDK 17 toolchain,
 * and without the pin a Java 9+ API call would compile into a Java 8 class file and fail at runtime
 * on a Java 8 consumer.
 */
fun Project.jvmBytecodeTarget(version: Int) {
    val javaVersion = JavaVersion.toVersion(version)
    val jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
    extensions.configure<KotlinMultiplatformExtension> {
        jvm {
            compilerOptions {
                this.jvmTarget.set(jvmTarget)
                freeCompilerArgs.add("-Xjdk-release=${jvmTarget.target}")
            }
        }
    }
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(version)
    }
}

package com.bkahlert.kommons.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Compiles the JVM target of this multiplatform project to bytecode of the given Java [version] (8, 11, 17, ...)
 * for Kotlin and Java sources alike, independent of the JDK toolchain the build runs on.
 *
 * Both have to agree, otherwise the Kotlin Gradle plugin's JVM target validation fails the build.
 */
fun Project.jvmBytecodeTarget(version: Int) {
    val javaVersion = JavaVersion.toVersion(version)
    extensions.configure<KotlinMultiplatformExtension> {
        jvm {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
            }
        }
    }
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

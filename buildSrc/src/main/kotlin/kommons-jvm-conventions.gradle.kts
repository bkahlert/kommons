import com.bkahlert.kommons.gradle.jvmBytecodeTarget

plugins {
    id("kotlin-conventions")
}

kotlin {
    jvm {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }
}

jvmBytecodeTarget(8)

// `-PtestJdk=21` runs the JVM tests on that JDK instead of the build toolchain; CI uses it for 21 and 25.
providers.gradleProperty("testJdk").orNull?.let { version ->
    val launcher = the<JavaToolchainService>().launcherFor {
        languageVersion.set(JavaLanguageVersion.of(version))
    }
    tasks.withType<Test>().configureEach {
        javaLauncher.set(launcher)
    }
}

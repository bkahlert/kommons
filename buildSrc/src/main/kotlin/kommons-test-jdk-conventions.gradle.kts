// `-PtestJdk=21` runs the tests on that JDK instead of the build toolchain; CI uses it for 21 and 25.
// Shared by the multiplatform conventions and the plain-JVM Spring Boot sample.
providers.gradleProperty("testJdk").orNull?.let { version ->
    val launcher = the<JavaToolchainService>().launcherFor {
        languageVersion.set(JavaLanguageVersion.of(version))
    }
    tasks.withType<Test>().configureEach {
        javaLauncher.set(launcher)
    }
}

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.nebula.release.plugin)
    implementation(libs.spring.dependency.management.gradle.plugin)
    implementation(libs.spring.boot.gradle.plugin)
}

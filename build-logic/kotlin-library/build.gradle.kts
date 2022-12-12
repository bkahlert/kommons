plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.spring.dependency.management.gradle.plugin)
    implementation(libs.spring.boot.gradle.plugin)
    implementation(project(":commons"))
}

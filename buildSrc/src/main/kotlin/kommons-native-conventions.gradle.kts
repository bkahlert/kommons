import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("kotlin-conventions")
}

// `-PosArchOnly=true` registers only the host's native target, which keeps local builds short.
// Source-set wiring (nativeMain, linuxMain, appleMain, ...) comes from Kotlin's default hierarchy template.
val osArchOnly: Boolean = providers.gradleProperty("osArchOnly").map(String::toBoolean).getOrElse(false)

kotlin {
    if (osArchOnly) {
        val hostOs = System.getProperty("os.name")
        val hostArch = System.getProperty("os.arch")
        when {
            hostOs == "Mac OS X" -> if (hostArch == "aarch64") macosArm64() else @Suppress("DEPRECATION") macosX64()
            hostOs == "Linux" -> if (hostArch == "aarch64") linuxArm64() else linuxX64()
            hostOs.startsWith("Windows") -> mingwX64()
            else -> throw GradleException("Host OS $hostOs is not supported in Kotlin/Native.")
        }
    } else {
        linuxX64()
        linuxArm64()
        mingwX64()
        // deprecated upstream since Kotlin 2.3.20; still published for existing consumers (spec decision 9)
        @Suppress("DEPRECATION")
        macosX64()
        macosArm64()
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        compilerOptions {
            optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }
}

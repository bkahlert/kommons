@file:Suppress("UNUSED_VARIABLE")

plugins {
    id("kotlin-conventions")
}

val osArchOnly: Boolean? by project

kotlin {

    if (osArchOnly == true) {
        val hostOs = System.getProperty("os.name")
        val hostArch = System.getProperty("os.arch")
        val nativeTarget = when {
            hostOs == "Mac OS X" -> if (hostArch == "aarch64") macosArm64() else macosX64()
            hostOs == "Linux" -> linuxX64()
            hostOs.startsWith("Windows") -> mingwX64()
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }

        sourceSets {
            val commonMain by getting
            val nativeMain by creating { dependsOn(commonMain) }
            val currentNativeMain = getByName("${nativeTarget.name}Main") { dependsOn(nativeMain) }

            val commonTest by getting
            val nativeTest by creating { dependsOn(commonTest) }
            val currentNativeTest = getByName("${nativeTarget.name}Test") { dependsOn(nativeTest) }
        }
    } else {
        targets {
            linuxX64()

            mingwX64()

            macosX64()
            macosArm64()

            // TODO sync with https://kotlinlang.org/docs/native-target-support.html#for-library-authors

//        iosX64()
//        iosArm64()
//        iosArm32()
//        iosSimulatorArm64()

//        tvos()
//        tvosSimulatorArm64()

//        watchosArm32()
//        watchosArm64()
//        watchosX86()
//        watchosX64()
//        watchosSimulatorArm64()
        }

        sourceSets {

            /*
             * MAIN SOURCE SETS
             */
            val commonMain by getting
            val nativeMain by creating { dependsOn(commonMain) }

            val linuxX64Main by getting { dependsOn(nativeMain) }

            val mingwX64Main by getting { dependsOn(nativeMain) }

            val macosX64Main by getting { dependsOn(nativeMain) }
            val macosArm64Main by getting { dependsOn(nativeMain) }

//        val iosX64Main by getting { dependsOn(desktopMain) }
//        val iosArm64Main by getting { dependsOn(desktopMain) }
//        val iosArm32Main by getting { dependsOn(desktopMain) }
//        val iosSimulatorArm64Main by getting { dependsOn(desktopMain) }

//        val tvosMain by getting { dependsOn(desktopMain) }
//        val tvosSimulatorArm64Main by getting { dependsOn(desktopMain) }

//        val watchosArm32Main by getting { dependsOn(desktopMain) }
//        val watchosArm64Main by getting { dependsOn(desktopMain) }
//        val watchosX86Main by getting { dependsOn(desktopMain) }
//        val watchosX64Main by getting { dependsOn(desktopMain) }
//        val watchosSimulatorArm64Main by getting { dependsOn(desktopMain) }

            /*
             * TEST SOURCE SETS
             */
            val commonTest by getting
            val nativeTest by creating { dependsOn(commonTest) }

            val linuxX64Test by getting { dependsOn(nativeTest) }

            val mingwX64Test by getting { dependsOn(nativeTest) }

            val macosX64Test by getting { dependsOn(nativeTest) }
            val macosArm64Test by getting { dependsOn(nativeTest) }

//        val iosX64Test by getting { dependsOn(nativeTest) }
//        val iosArm64Test by getting { dependsOn(nativeTest) }
//        val iosArm32Test by getting { dependsOn(nativeTest) }
//        val iosSimulatorArm64Test by getting { dependsOn(nativeTest) }

//        val tvosTest by getting { dependsOn(nativeTest) }
//        val tvosSimulatorArm64Test by getting { dependsOn(nativeTest) }

//        val watchosArm32Test by getting { dependsOn(nativeTest) }
//        val watchosArm64Test by getting { dependsOn(nativeTest) }
//        val watchosX86Test by getting { dependsOn(nativeTest) }
//        val watchosX64Test by getting { dependsOn(nativeTest) }
//        val watchosSimulatorArm64Test by getting { dependsOn(nativeTest) }
        }
    }
}

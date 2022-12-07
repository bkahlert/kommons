@file:Suppress("UNUSED_VARIABLE")

plugins {
    id("kotlin-conventions")
}

kotlin {
    explicitApi()

    targets {
        linuxX64()

        mingwX64()

        macosX64()
        macosArm64()

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

@file:Suppress("UNUSED_VARIABLE")

plugins {
    id("kotlin-conventions")
}

kotlin {
    targets {
        jvm {
            compilations.all {
                kotlinOptions.jvmTarget = "1.8"
            }
        }
    }

    sourceSets {
        val jvmMain by getting
        val jvmTest by getting
    }
}

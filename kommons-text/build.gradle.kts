import com.bkahlert.kommons.gradle.Unicode

plugins {
    id("kommons-multiplatform-library-conventions")
}

description = "Kommons Text is a Kotlin Multiplatform Library for Unicode-aware text manipulations."

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":kommons-core"))
        }
        commonTest.dependencies {
            implementation(project(":kommons-test"))
        }

        jvmMain.dependencies {
            api(libs.icu4j)
        }

        jsMain.dependencies {
            implementation(npm("xregexp", libs.versions.xregexp.get())) { because("regex classes for char meta data") }
            implementation(npm("@stdlib/string-next-grapheme-cluster-break", libs.versions.stdlib.js.get())) { because("grapheme sequence") }
        }

        nativeMain.dependencies {
            api(libs.mordant)
        }
    }
}

tasks.register("generateUnicodeData") {
    group = "build"
    description = "Regenerates src/nativeMain/.../UnicodeData.kt from the Unicode Character Database"
    val sourceFile = layout.projectDirectory.file("src/nativeMain/kotlin/com/bkahlert/kommons/text/UnicodeData.kt").asFile
    doLast {
        val generated = Unicode.UnicodeData.generate(sourceFile)
        logger.lifecycle("Generated $generated")
    }
}

import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

plugins {
    id("kotlin-conventions")
}

kotlin {
    targets {
        js(IR) {
            browser {
                testTask {
                    testLogging.showStandardStreams = true
                    useKarma {
                        useChromeHeadless()
                    }
                }
            }
            nodejs {
                testTask {
                    useMocha {
                        timeout = "10000"
                    }
                }
            }
            yarn.ignoreScripts = false // suppress "warning Ignored scripts due to flag." warning
        }
    }
}

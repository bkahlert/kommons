import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn
import java.time.Duration

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
                        timeout.set(Duration.ofSeconds(10))
                    }
                }
            }
            nodejs()
            yarn.ignoreScripts = false // suppress "warning Ignored scripts due to flag." warning
        }
    }
}

package com.bkahlert.kommons.test.junit.launcher

import org.junit.jupiter.engine.Constants
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.launcher.listeners.TestExecutionSummary
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** [TestExecutionListener] that briefly prints the result of the test execution. */
public class TestExecutionReporter(
    private val println: (String) -> Unit = { kotlin.io.println(it) },
) : SummaryGeneratingListener() {

    private var disabled = false

    private val TestExecutionSummary.succeeded: Long get() = testsSucceededCount
    private val TestExecutionSummary.failed: Long get() = testsFailedCount
    private val TestExecutionSummary.aborted: Long get() = testsAbortedCount
    private val TestExecutionSummary.skipped: Long get() = testsSkippedCount
    private val TestExecutionSummary.total: Long get() = testsStartedCount

    private val duration: Duration get() = summary.let { it.timeFinished - it.timeStarted }.milliseconds

    @Suppress("KDocMissingDocumentation")
    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        super.testPlanExecutionStarted(testPlan)
        disabled = testPlan.getConfigurationBooleanValue(DISABLED_PROPERTY_NAME, false)
    }

    @Suppress("KDocMissingDocumentation")
    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        super.testPlanExecutionFinished(testPlan)
        if (disabled) return

        with(summary) {
            buildString {
                appendLine()
                if (total == 0L) {
                    append(yellow("⁉︎ no tests executed"))
                } else {
                    append(cyan("${total.tests} within "))
                    append(brightCyan("$duration"))
                    append(": ")
                    listOf<Pair<Long, (String) -> String>>(
                        failed to { yellow("✘︎ $it failed") },
                        aborted to { red("ϟ $it crashed") },
                        succeeded to { green("✔︎ $it passed") },
                        skipped to { grey("◍ $it ignored") },
                    ).filter { (count, _) ->
                        count != 0L
                    }.joinTo(this, ", ") { (count, format) ->
                        when (count) {
                            total -> format("all")
                            else -> format("$count")
                        }
                    }
                }
                appendLine()
            }
        }.also(println)
    }

    public companion object {

        /** Property name used to configure whether the [TestExecutionReporter] is disabled. */
        public const val DISABLED_PROPERTY_NAME: String = "com.bkahlert.kommons.test.junit.launcher.reporter.disabled"

        /** @see Constants.DEACTIVATE_CONDITIONS_PATTERN_PROPERTY_NAME */
        public fun LauncherDiscoveryRequestBuilder.disableTestExecutionReporter(value: Boolean = true): LauncherDiscoveryRequestBuilder =
            configurationParameter(DISABLED_PROPERTY_NAME, value.toString())
    }
}

/** Gets the configuration value for the specified [name] or the specified [default] if no configuration is found or an error occurs. */
@Suppress("unused")
internal fun TestPlan.getConfigurationValue(name: String, default: String): String =
    kotlin.runCatching { configurationParameters.get(name).orElse(default) }.getOrDefault(default)

/** Gets the boolean configuration value for the specified [name] or the specified [default] if no configuration is found or an error occurs. */
@Suppress("unused")
internal fun TestPlan.getConfigurationBooleanValue(name: String, default: Boolean): Boolean =
    kotlin.runCatching { configurationParameters.getBoolean(name).orElse(default) }.getOrDefault(default)

private val Long.tests: String
    get() = when (this) {
        1L -> "$this test"
        else -> "$this tests"
    }

private fun red(string: CharSequence) = "\u001b[1;31m$string\u001B[0m"
private fun green(string: CharSequence) = "\u001b[1;32m$string\u001B[0m"
private fun yellow(string: CharSequence) = "\u001b[1;33m$string\u001B[0m"
private fun grey(string: CharSequence) = "\u001b[1;90m$string\u001B[0m"
private fun cyan(string: CharSequence) = "\u001b[1;36m$string\u001B[0m"
private fun brightCyan(string: CharSequence) = "\u001b[1;96m$string\u001B[0m"

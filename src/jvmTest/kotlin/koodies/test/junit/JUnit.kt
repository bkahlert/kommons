package koodies.test.junit

import koodies.io.ByteArrayOutputStream
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.launcher.Launcher
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.launcher.listeners.TestExecutionSummary
import java.io.PrintWriter

object JUnit {
    enum class ExecutionMode(private val value: String) {
        Concurrent("concurrent"), SameThread("same_thread");

        override fun toString(): String = value
    }

    fun runTests(
        vararg selectors: DiscoverySelector,
        launcherDiscoveryRequestBuilder: (LauncherDiscoveryRequestBuilder.() -> Unit)? = null,
        launcherConfigBuilder: (LauncherConfig.Builder.() -> Unit)? = null,
        launcher: (Launcher.() -> Unit)? = null,
    ): SummaryGeneratingListener =
        SummaryGeneratingListener().also { listener ->
            LauncherDiscoveryRequestBuilder
                .request()
                .selectors(*selectors)
                .apply(launcherDiscoveryRequestBuilder ?: {})
                .build().also { request ->
                    val launcherConfig = LauncherConfig.builder().apply(launcherConfigBuilder ?: {}).build()
                    LauncherFactory.create(launcherConfig).apply {
                        discover(request)
                        registerTestExecutionListeners(listener)
                        apply(launcher ?: {})
                        execute(request)
                    }
                }
        }

    fun LauncherDiscoveryRequestBuilder.disableConcurrency() {
        configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
    }

    fun LauncherDiscoveryRequestBuilder.concurrentMode(factor: ExecutionMode = ExecutionMode.Concurrent) {
        configurationParameter("junit.jupiter.execution.parallel.mode.default", "$factor")
    }

    fun LauncherDiscoveryRequestBuilder.concurrentClassMode(factor: ExecutionMode = ExecutionMode.Concurrent) {
        configurationParameter("junit.jupiter.execution.parallel.mode.classes.default", "$factor")
    }

    fun LauncherDiscoveryRequestBuilder.concurrentDynamicFactor(factor: Int = 10) {
        configurationParameter("junit.jupiter.execution.parallel.config.dynamic.factor", "$factor")
    }

    fun TestExecutionSummary.render(): String {
        val buffer = ByteArrayOutputStream()
        printTo(PrintWriter(buffer))
        return buffer.toString(Charsets.UTF_8)
    }

    fun TestExecutionSummary.print() {
        printTo(PrintWriter(System.out))
    }
}

package com.bkahlert.kommons.test.junit.launcher

import org.junit.jupiter.engine.Constants
import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.Launcher
import org.junit.platform.launcher.LauncherConstants
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherConfig.Builder
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.launcher.listeners.TestExecutionSummary
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

/**
 * Launches the specified [selectors] with a default configuration
 * that can be further customized with the optionally specifiable
 * [launcherConfigInit], [launcherInit], and [launcherDiscoveryRequestInit].
 * @see DiscoverySelectors
 * @see KotlinDiscoverySelectors
 */
public fun launchTests(
    vararg selectors: DiscoverySelector,
    init: (LauncherBuilder.() -> Unit)? = null,
): TestExecutionSummary {
    val listener = SummaryGeneratingListener()

    val launcherDiscoveryRequestInits: MutableList<LauncherDiscoveryRequestBuilder.() -> Unit> = mutableListOf()
    val launcherConfigInits: MutableList<Builder.() -> Unit> = mutableListOf()
    val launcherInits: MutableList<Launcher.() -> Unit> = mutableListOf()

    init?.also {
        (object : LauncherBuilder {
            override fun request(init: LauncherDiscoveryRequestBuilder.() -> Unit) = Unit.also { launcherDiscoveryRequestInits.add(init) }
            override fun config(init: Builder.() -> Unit) = Unit.also { launcherConfigInits.add(init) }
            override fun launcher(init: Launcher.() -> Unit) = Unit.also { launcherInits.add(init) }
        }).apply(it)
    }

    val launcherDiscoveryRequest = LauncherDiscoveryRequestBuilder
        .request()
        .apply { launcherDiscoveryRequestInits.forEach { it() } }
        .selectors(*selectors)
        .build()

    val launcherConfig = LauncherConfig
        .builder()
        .apply { launcherConfigInits.forEach { it() } }
        .build()

    val launcher = LauncherFactory.create(launcherConfig).apply {
        discover(launcherDiscoveryRequest)
        registerTestExecutionListeners(listener)
        launcherInits.forEach { it() }
    }

    launcher.execute(launcherDiscoveryRequest)

    return listener.summary
}

/** Builders to build everything required by [launchTests]. */
public interface LauncherBuilder {
    /** Applies the specified [init] to the [LauncherDiscoveryRequestBuilder]. */
    public fun request(init: LauncherDiscoveryRequestBuilder.() -> Unit)

    /** Applies the specified [init] to the [LauncherConfig]. */
    public fun config(init: Builder.() -> Unit)

    /** Applies the specified [init] to the [Launcher]. */
    public fun launcher(init: Launcher.() -> Unit)
}


/** @see Constants.DEACTIVATE_CONDITIONS_PATTERN_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.deactivateConditionsPattern(value: String = Constants.DEACTIVATE_ALL_CONDITIONS_PATTERN): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEACTIVATE_CONDITIONS_PATTERN_PROPERTY_NAME, value)

/** @see Constants.DEFAULT_DISPLAY_NAME_GENERATOR_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.defaultDisplayNameGenerator(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_DISPLAY_NAME_GENERATOR_PROPERTY_NAME, value)

@Suppress("SpellCheckingInspection")
/** @see Constants.EXTENSIONS_AUTODETECTION_ENABLED_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.extensionsAutodetectionEnabled(value: Boolean): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.EXTENSIONS_AUTODETECTION_ENABLED_PROPERTY_NAME, value.toString())

/** @see Constants.DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.defaultTestInstanceLifecycle(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME, value)

/** @see Constants.PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.parallelExecutionEnabled(value: Boolean): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME, value.toString())

/** @see Constants.DEFAULT_PARALLEL_EXECUTION_MODE */
public fun LauncherDiscoveryRequestBuilder.defaultParallelExecutionMode(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_PARALLEL_EXECUTION_MODE, value)

/** @see Constants.DEFAULT_CLASSES_EXECUTION_MODE_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.defaultClassesExecutionMode(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_CLASSES_EXECUTION_MODE_PROPERTY_NAME, value)

/** @see Constants.PARALLEL_CONFIG_STRATEGY_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.parallelConfigStrategy(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.PARALLEL_CONFIG_STRATEGY_PROPERTY_NAME, value)

/** @see Constants.PARALLEL_CONFIG_FIXED_PARALLELISM_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.parallelConfigFixedParallelism(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.PARALLEL_CONFIG_FIXED_PARALLELISM_PROPERTY_NAME, value)

/** @see Constants.PARALLEL_CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.parallelConfigDynamicFactor(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.PARALLEL_CONFIG_DYNAMIC_FACTOR_PROPERTY_NAME, value)

/** @see Constants.PARALLEL_CONFIG_CUSTOM_CLASS_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.parallelConfigCustomClass(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.PARALLEL_CONFIG_CUSTOM_CLASS_PROPERTY_NAME, value)

/** @see Constants.DEFAULT_TIMEOUT_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.defaultTimeout(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_TIMEOUT_PROPERTY_NAME, value)

/** @see Constants.DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.defaultTestableMethodTimeout(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME, value)

/** @see Constants.DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.defaultTestMethodTimeout(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME, value)

/** @see Constants.DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.defaultTestTemplateMethodTimeout(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME, value)

/** @see Constants.DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.defaultTestFactoryMethodTimeout(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME, value)

/** @see Constants.DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.defaultLifecycleMethodTimeout(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME, value)

/** @see Constants.DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.defaultBeforeAllMethodTimeout(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME, value)

/** @see Constants.DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.defaultBeforeEachMethodTimeout(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME, value)

/** @see Constants.DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.defaultAfterEachMethodTimeout(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME, value)

/** @see Constants.DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.defaultAfterAllMethodTimeout(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME, value)

/** @see Constants.TIMEOUT_MODE_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.timeoutMode(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.TIMEOUT_MODE_PROPERTY_NAME, value)

/** @see Constants.DEFAULT_TEST_METHOD_ORDER_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.defaultTestMethodOrder(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_TEST_METHOD_ORDER_PROPERTY_NAME, value)

/** @see Constants.DEFAULT_TEST_CLASS_ORDER_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.defaultTestClassOrder(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(Constants.DEFAULT_TEST_CLASS_ORDER_PROPERTY_NAME, value)

/** @see LauncherConstants.CAPTURE_STDOUT_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.captureStdout(value: Boolean): LauncherDiscoveryRequestBuilder =
    configurationParameter(LauncherConstants.CAPTURE_STDOUT_PROPERTY_NAME, value.toString())

/** @see LauncherConstants.CAPTURE_STDERR_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.captureStderr(value: Boolean): LauncherDiscoveryRequestBuilder =
    configurationParameter(LauncherConstants.CAPTURE_STDERR_PROPERTY_NAME, value.toString())

/** @see LauncherConstants.CAPTURE_MAX_BUFFER_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.captureMaxBuffer(value: String): LauncherDiscoveryRequestBuilder =
    configurationParameter(LauncherConstants.CAPTURE_MAX_BUFFER_PROPERTY_NAME, value)

/** @see LauncherConstants.DEACTIVATE_LISTENERS_PATTERN_PROPERTY_NAME */
public fun LauncherDiscoveryRequestBuilder.deactivateListenersPattern(value: String = LauncherConstants.DEACTIVATE_ALL_LISTENERS_PATTERN): LauncherDiscoveryRequestBuilder =
    configurationParameter(LauncherConstants.DEACTIVATE_LISTENERS_PATTERN_PROPERTY_NAME, value)


/** The printed form of this summary as a string. */
public val TestExecutionSummary.printed: String
    get() = ByteArrayOutputStream().also { printTo(PrintWriter(it)) }.toString(Charsets.UTF_8.toString())


/** Returns the configuration parameters as a map. */
public fun ConfigurationParameters.asMap(): Map<String, String?> =
    keySet().associateWith { get(it).orElse(null) }

package koodies.concurrent

import koodies.builder.BuilderTemplate
import koodies.builder.Init
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.concurrent.CommandLineExecutionOptions.Companion.CommandLineExecutionOptionsContext
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.ProcessTerminationCallback
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors
import koodies.concurrent.process.Processors.noopProcessor
import koodies.concurrent.process.process
import koodies.concurrent.process.toProcessor
import koodies.docker.DockerProcess
import koodies.docker.DockerRunCommandLine
import koodies.logging.LoggingOptions
import koodies.logging.LoggingOptions.Companion.LoggingOptionsContext
import koodies.logging.RenderingLogger
import koodies.logging.logging
import koodies.text.LineSeparators
import java.nio.file.Path

/* ALL PROCESS METHODS BELOW ALWAYS START THE PROCESS BUT DONT PROCESS */

/**
 * Creates a [DockerProcess] that executes this command line.
 */
public fun DockerRunCommandLine.toManagedProcess(expectedExitValue: Int?, processTerminationCallback: ProcessTerminationCallback?): DockerProcess =
    DockerProcess.from(this, expectedExitValue, processTerminationCallback)

/**
 * Creates a [ManagedProcess] that executes this command line.
 */
public fun CommandLine.toManagedProcess(expectedExitValue: Int? = 0, processTerminationCallback: ProcessTerminationCallback? = null): ManagedProcess =
    ManagedProcess.from(this, expectedExitValue, processTerminationCallback)

/**
 * Runs the specified [commandLine] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * **Important:** This function does just that—starting the process. To do something with the returned [ManagedProcess]
 * you can use one of the provided [Processors] or implement one on your own, e.g.
 * - `process(...).process()` defaults to [Processors.consoleLoggingProcessor] which prints all [IO] to the console
 * - `process(...).process { io -> doSomething(io) }` to process the [IO] the way you like
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun process(
    commandLine: CommandLine,
    expectedExitValue: Int? = 0,
    processTerminationCallback: ProcessTerminationCallback? = null,
): ManagedProcess = commandLine.toManagedProcess(expectedExitValue, processTerminationCallback)

/**
 * Runs the specified [command] using the specified [arguments] in `this` working directory.
 *
 * Optionally [redirects], the [environment] and the [expectedExitValue] (default: `0`) can be provided.
 *
 * **Important:** This function does just that—starting the process. To do something with the returned [ManagedProcess]
 * you can use one of the provided [Processors] or implement one on your own, e.g.
 * - `process(...).process()` defaults to [Processors.consoleLoggingProcessor] which prints all [IO] to the console
 * - `process(...).process { io -> doSomething(io) }` to process the [IO] the way you like.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun Path.process(
    command: String,
    vararg arguments: String,
    redirects: List<String> = emptyList(),
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: ProcessTerminationCallback? = null,
): ManagedProcess = process(CommandLine(redirects, environment, this, command, arguments.toList()), expectedExitValue, processTerminationCallback)


/* ALL EXECUTE METHODS BELOW ALWAYS START THE COMMANDLINE/PROCESS AND AND PROCESS IT SYNCHRONOUSLY */

/**
 * Helper to collect an optional [RenderingLogger], build [CommandLineExecutionOptions] and an optional [Processor]
 * to [execute] the given [CommandLine].
 */
public class CommandLineLoggingExecution(
    private val logger: RenderingLogger?,
    private val commandLine: CommandLine,
) {
    private var processor: Processor<ManagedProcess>? = null

    public fun executeWithOptionalProcessor(init: (CommandLineExecutionOptionsContext.() -> Processor<ManagedProcess>?)?): ManagedProcess =
        executeWithOptionallyStoredProcessor { init?.let { processor = it() } }

    public fun executeWithOptionallyStoredProcessor(init: Init<CommandLineExecutionOptionsContext>?): ManagedProcess {
        val options: CommandLineExecutionOptions = init?.let { CommandLineExecutionOptions(it) }
            ?: (CommandLineExecutionOptions(loggingOptions = LoggingOptions(commandLine.summary, koodies.text.ANSI.Colors.brightBlue, false)))
        return logger?.run {
            logging(
                caption = options.loggingOptions.caption ?: commandLine.summary,
                bordered = options.loggingOptions.bordered,
                formatter = options.loggingOptions.formatter ?: koodies.text.ANSI.Colors.brightBlue) {
                process(options.expectedExitValue, options.processTerminationCallback, processor ?: toProcessor())
            }
        } ?: run {
            process(options.expectedExitValue, options.processTerminationCallback, processor ?: Processors.consoleLoggingProcessor())
        }
    }

    private fun process(
        expectedExitValue: Int? = 0,
        processTerminationCallback: ProcessTerminationCallback? = null,
        processor: Processor<ManagedProcess>,
    ): ManagedProcess = process(commandLine, expectedExitValue, processTerminationCallback).process({ sync }, processor)
}

/**
 * Options used to [execute] a [CommandLine].
 */
public data class CommandLineExecutionOptions(
    val expectedExitValue: Int? = 0,
    val processTerminationCallback: ProcessTerminationCallback? = null,
    val loggingOptions: LoggingOptions = LoggingOptions(),
) {
    public companion object : BuilderTemplate<CommandLineExecutionOptionsContext, CommandLineExecutionOptions>() {
        public class CommandLineExecutionOptionsContext(override val captures: CapturesMap) : CapturingContext() {
            public val expectedExitValue: SkippableCapturingBuilderInterface<() -> Int?, Int?> by builder()
            public val processTerminationCallback: SkippableCapturingBuilderInterface<() -> (Throwable?) -> Unit, ((Throwable?) -> Unit)?> by builder()
            public val loggingOptions: SkippableCapturingBuilderInterface<LoggingOptionsContext.() -> Unit, LoggingOptions?> by LoggingOptions
        }

        override fun BuildContext.build(): CommandLineExecutionOptions = ::CommandLineExecutionOptionsContext{
            CommandLineExecutionOptions(::expectedExitValue.evalOrDefault(0),
                ::processTerminationCallback.evalOrNull(),
                ::loggingOptions.evalOrDefault { LoggingOptions() })
        }
    }
}

/**
 * Runs `this` [CommandLine] using `this` optional [RenderingLogger]
 * and built [CommandLineExecutionOptions].
 *
 * If a [Processor] is returned at the end of the [CommandLineExecutionOptions] build,
 * it will be used to process the process's [IO]. Otherwise the [IO] will be logged
 * either to the console or if present, `this` [RenderingLogger].
 */
public val CommandLine.execute: RenderingLogger?.((CommandLineExecutionOptionsContext.() -> Processor<ManagedProcess>?)?) -> ManagedProcess
    get() = { CommandLineLoggingExecution(this, this@execute).executeWithOptionalProcessor(it) }

/**
 * Runs `this` [CommandLine] using `this` optional [RenderingLogger]
 * and built [CommandLineExecutionOptions].
 *
 * If a [Processor] is returned at the end of the [CommandLineExecutionOptions] build,
 * it will be used to process the process's [IO]. Otherwise the [IO] will be logged
 * either to the console or if present, `this` [RenderingLogger].
 */
public val RenderingLogger.execute: CommandLine.((CommandLineExecutionOptionsContext.() -> Processor<ManagedProcess>?)?) -> ManagedProcess
    get() = { CommandLineLoggingExecution(this@execute, this).executeWithOptionalProcessor(it) }

/**
 * Returns (and possibly blocks until finished) the output of `this` [ManagedProcess].
 *
 * This method is idempotent.
 */
public fun ManagedProcess.output(): String = run {
    process({ sync }, noopProcessor())
    ioLog.logged.filter { it.type == IO.Type.OUT }.joinToString(LineSeparators.LF) { it.unformatted }
}

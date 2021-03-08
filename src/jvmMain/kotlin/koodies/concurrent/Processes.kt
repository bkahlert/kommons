package koodies.concurrent

import koodies.builder.Init
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.CommandLine.Companion.CommandLineContext
import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors
import koodies.concurrent.process.processSynchronously
import koodies.concurrent.process.toProcessor
import koodies.docker.DockerProcess
import koodies.docker.DockerRunCommandLine
import koodies.logging.LoggingOptions
import koodies.logging.RenderingLogger
import koodies.logging.logging2
import koodies.terminal.ANSI
import koodies.text.LineSeparators
import java.nio.file.Path

/* ALL PROCESS METHODS BELOW ALWAYS START THE PROCESS BUT DONT PROCESS */

/**
 * Creates a [DockerProcess] that executes this command line.
 */
public fun DockerRunCommandLine.toManagedProcess(expectedExitValue: Int?, processTerminationCallback: (() -> Unit)?): DockerProcess =
    DockerProcess.from(this, expectedExitValue, processTerminationCallback)

/**
 * Creates a [ManagedProcess] that executes this command line.
 */
public fun CommandLine.toManagedProcess(expectedExitValue: Int? = 0, processTerminationCallback: (() -> Unit)? = null): ManagedProcess =
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
    processTerminationCallback: (() -> Unit)? = null,
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
    processTerminationCallback: (() -> Unit)? = null,
): ManagedProcess = process(CommandLine(redirects, environment, this, command, arguments.toList()), expectedExitValue, processTerminationCallback)


/* ALL EXECUTE METHODS BELOW ALWAYS START THE COMMANDLINE/PROCESS AND AND PROCESS IT SYNCHRONOUSLY */

/**
 * Runs `this` [CommandLine] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of the created [ManagedProcess] will be processed by the specified [processor]
 * which defaults to [Processors.consoleLoggingProcessor] which prints all [IO] to the console.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun CommandLine.execute(
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    processor: Processor<ManagedProcess> = Processors.consoleLoggingProcessor(),
): ManagedProcess {
    return process(this, expectedExitValue, processTerminationCallback).processSynchronously(processor)
}

/**
 * Runs the specified [commandLine] with the specified [environment]
 * in [Locations.Temp] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of the created [ManagedProcess] will be processed by the specified [processor]
 * which defaults to [Processors.consoleLoggingProcessor] which prints all [IO] to the console.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
fun execute(
    commandLine: CommandLine,
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    processor: Processor<ManagedProcess> = Processors.consoleLoggingProcessor(),
): ManagedProcess = Locations.Temp.execute(commandLine, environment, expectedExitValue, processTerminationCallback, processor)


/**
 * Runs the specified [commandLine] with the specified [environment]
 * in `this` [Path] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of the created [ManagedProcess] will be processed by the specified [processor]
 * which defaults to [Processors.consoleLoggingProcessor] which prints all [IO] to the console.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
fun Path.execute(
    processor: Processor<ManagedProcess> = Processors.consoleLoggingProcessor(),
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    commandLine: Init<CommandLineContext>,
): ManagedProcess = execute(CommandLine(commandLine), environment, expectedExitValue, processTerminationCallback, processor)

/**
 * Runs the specified [commandLine] with the specified [environment]
 * in [Locations.Temp] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of the created [ManagedProcess] will be processed by the specified [processor]
 * which defaults to [Processors.consoleLoggingProcessor] which prints all [IO] to the console.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun execute(
    processor: Processor<ManagedProcess> = Processors.consoleLoggingProcessor(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    commandLine: Init<CommandLineContext>,
): ManagedProcess = execute(CommandLine(commandLine), environment, expectedExitValue, processTerminationCallback, processor)


/**
 * Runs the specified [commandLine] with the specified [environment]
 * in `this` [Path] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of the created [ManagedProcess] will be logged by the specified [logger]
 * which prints all [IO] to the console if `null`
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
fun Path.execute(
    logger: RenderingLogger?,
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    commandLine: Init<CommandLineContext>,
): ManagedProcess = execute(
    processor = logger.toProcessor(),
    environment = environment,
    expectedExitValue = expectedExitValue,
    processTerminationCallback = processTerminationCallback,
    commandLine = commandLine
)

/**
 * Runs the specified [commandLine] with the specified [environment]
 * in [Locations.Temp] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of the created [ManagedProcess] will be logged by the specified [logger]
 * which prints all [IO] to the console if `null`
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun execute(
    logger: RenderingLogger?,
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    commandLine: Init<CommandLineContext>,
): ManagedProcess = execute(
    processor = logger.toProcessor(),
    environment = environment,
    expectedExitValue = expectedExitValue,
    processTerminationCallback = processTerminationCallback,
    commandLine = commandLine
)

// TODO match signatures below

/**
 * Starts a new [ManagedProcess] that runs this command line.
 */
fun CommandLine.execute(expectedExitValue: Int? = 0, processTerminationCallback: (() -> Unit)? = null): ManagedProcess =
    process(this, expectedExitValue, processTerminationCallback)//.apply { start() }

/**
 * Starts a new [ManagedProcess] that runs this command line
 * and has it fully processed using `this` [RenderingLogger].
 */
fun CommandLine.execute(
    caption: String,
    bordered: Boolean = true,
    ansiCode: AnsiColorCode = ANSI.termColors.brightBlue,
    nonBlockingReader: Boolean = false,
    expectedExitValue: Int? = 0,
): Int = logging(caption = caption, bordered = bordered, ansiCode = ansiCode) {
    this@execute.execute(expectedExitValue).process(
        nonBlockingReader = nonBlockingReader,
        processInputStream = InputStream.nullInputStream(),
        processor = Processors.loggingProcessor(this)
    ).waitForTermination()
}


/**
 * Starts a new [ManagedProcess] that runs this command line
 * and has it fully processed using `this` [RenderingLogger].
 */
val RenderingLogger.execute: CommandLine.(
    caption: String,
    bordered: Boolean,
    ansiCode: AnsiColorCode?,
    nonBlockingReader: Boolean?,
    expectedExitValue: Int?,
) -> Int
    get() = { caption, bordered, ansiCode, nonBlockingReader, expectedExitValue ->
        this@execute.logging2(caption = caption, bordered = bordered, ansiCode = ansiCode ?: ANSI.termColors.brightBlue) {
            execute(expectedExitValue ?: 0).process(
                nonBlockingReader = nonBlockingReader ?: false,
                processInputStream = InputStream.nullInputStream(),
                processor = Processors.loggingProcessor(this)
            ).waitForTermination()
        }
    }


fun main() {
    // TODO refactor: alles auf logging2?
    // TODO refactor: process nach execute umbenenen und wie script dann die signatur

    val commandLine = CommandLine("echo", "test")

    commandLine.execute("command line logging context", true, ANSI.termColors.brightBlue, true, null)
    with(commandLine) {
        execute("command line logging context", true, ANSI.termColors.brightBlue, true, null)
    }
    logging2("existing logging context") {
        with(commandLine) {
            execute("command line logging context", true, ANSI.termColors.brightBlue, true, null)
        }
    }
    with(commandLine) {
        logging2("existing logging context", bordered = true, ansiCode = ANSI.termColors.brightMagenta) {
            logLine { "abc" }
            execute("command line logging context", true, ANSI.termColors.magenta, true, null)
        }
    }
    with(commandLine) {
        logging2("existing logging context", ansiCode = ANSI.termColors.brightBlue) {
            logLine { "abc" }
            execute("command line logging context", false, ANSI.termColors.blue, true, null)
        }
    }
    with(commandLine) {
        logging2("existing logging context", bordered = false, ansiCode = ANSI.termColors.brightMagenta) {
            logLine { "abc" }
            execute("command line logging context", true, ANSI.termColors.magenta, true, null)
        }
    }
    with(commandLine) {
        logging2("existing logging context", bordered = false, ansiCode = ANSI.termColors.brightBlue) {
            logLine { "abc" }
            execute("command line logging context", false, ANSI.termColors.blue, true, null)
        }
    }
}

/**
 * Returns (and possibly blocks until finished) the output of `this` [ManagedProcess].
 *
 * This method is idempotent.
 */
public fun ManagedProcess.output(): String = run {
    processSynchronously(Processors.noopProcessor())
    ioLog.logged.filter { it.type == IO.Type.OUT }.joinToString(LineSeparators.LF) { it.unformatted }
}

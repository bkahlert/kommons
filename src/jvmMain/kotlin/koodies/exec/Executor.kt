package koodies.exec

import io.opentelemetry.api.trace.Tracer
import koodies.exec.ProcessingMode.Companion.ProcessingModeContext
import koodies.text.ANSI.FilteringFormatter
import koodies.text.ANSI.Formatter
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lines
import koodies.text.columns
import koodies.tracing.rendering.BlockStyle
import koodies.tracing.rendering.ColumnsLayout
import koodies.tracing.rendering.Printer
import koodies.tracing.rendering.RendererProvider
import koodies.tracing.rendering.ReturnValue
import koodies.tracing.rendering.Settings
import koodies.tracing.rendering.Style
import java.nio.file.Path

/**
 * An executor allows to execute an [executable]
 * in three ways:
 * 1. [invoke] just executes the [executable] with no special handling.
 * 2. [logging] executes the [executable] and logs the execution with the configured [tracingOptions].
 * 3. [processing] executes the [executable] by passing the [Exec]'s [IO] to the configured [processor].
 */
public data class Executor<E : Exec>(

    /**
     * Instance of what will be executed as soon as [invoke]
     * is called.
     */
    private val executable: Executable<E>,

    /**
     * Whether standard error is redirected to standard output during execution.
     */
    private val redirectErrorStream: Boolean = false,

    /**
     * The environment to be exposed to the [Exec] during execution.
     */
    private val environment: Map<String, String> = emptyMap(),

    private val tracingOptions: TracingOptions = TracingOptions(attributes = setOfNotNull(
        executable.name?.let { ExecAttributes.NAME to it },
        ExecAttributes.EXECUTABLE to executable,
    )),

    /**
     * Mode that defines if the execution will be synchronous
     * or asynchronous including mode-specific options, such as if the
     * [Exec]'s [IO] is to be processed non-blocking
     * (default: synchronous execution).
     */
    private val processingMode: ProcessingMode = ProcessingMode { sync },

    /**
     * Processor used to interactively handle the [Exec]'s [IO]
     * (default: no specific handling; the [IO] is only processed to be made available
     * by [Exec.io]).
     */
    private val processor: Processor<E>? = null,
) {

    /**
     * Adds a new environment variable with the given [key] and [value].
     */
    public fun env(key: String, value: String): Executor<E> =
        copy(environment = environment.plus(key to value))

    /**
     * Executes the [executable] with the current configuration.
     *
     * @param workingDirectory the working directory to be used during execution
     * @param execTerminationCallback called the moment the [Exec] terminates—no matter if the [Exec] succeeds or fails
     */
    public operator fun invoke(
        workingDirectory: Path? = null,
        execTerminationCallback: ExecTerminationCallback? = null,
    ): E {
        val renderedSpanNameOverridingOptions = tracingOptions.copy(renderer = { default ->
            tracingOptions.renderer(copy(
                nameFormatter = {
                    val replacedSpanName = StringBuilder().apply {
                        executable.name?.also { append("$it$LF") }
                        val contentLines = executable.content.lines()
                        if ((contentLines.size <= 3) && (contentLines.all { it.columns <= 60 })) {
                            append(executable.content)
                        } else {
                            append(executable.toLink())
                        }
                    }
                    nameFormatter(replacedSpanName)
                }
            ), default)
        })
        return executable
            .toExec(redirectErrorStream, environment, workingDirectory, execTerminationCallback)
            .process(processingMode, renderedSpanNameOverridingOptions, processor ?: Processors.noopProcessor())
    }

    /**
     * Executes the [executable] by logging all [IO] using the given [renderer].
     *
     * @param workingDirectory the working directory to be used during execution
     * @param execTerminationCallback called the moment the [Exec] terminates—no matter if the [Exec] succeeds or fails
     * @param nameFormatter convenience way to set [Settings.nameFormatter]
     * @param contentFormatter convenience way to set [Settings.contentFormatter]
     * @param decorationFormatter convenience way to set [Settings.decorationFormatter]
     * @param returnValueTransform convenience way to set [Settings.returnValueTransform]
     * @param layout convenience way to set [Settings.layout]
     * @param blockStyle convenience way to set [Settings.blockStyle]
     * @param oneLineStyle convenience way to set [Settings.oneLineStyle]
     * @param printer convenience way to set [Settings.printer]
     * @param renderer used to render the execution (default: properly nested child renderer)
     */
    public fun logging(
        workingDirectory: Path? = null,
        execTerminationCallback: ExecTerminationCallback? = null,

        nameFormatter: FilteringFormatter? = null,
        contentFormatter: FilteringFormatter? = null,
        decorationFormatter: Formatter? = null,
        returnValueTransform: ((ReturnValue) -> ReturnValue?)? = null,
        layout: ColumnsLayout? = null,
        blockStyle: ((ColumnsLayout) -> BlockStyle)? = null,
        oneLineStyle: Style? = null,
        printer: Printer? = null,

        renderer: RendererProvider = { it(this) },
    ): E = copy(tracingOptions = tracingOptions.copy(renderer = { default ->
        renderer(copy(
            nameFormatter = nameFormatter ?: this.nameFormatter,
            contentFormatter = contentFormatter ?: this.contentFormatter,
            decorationFormatter = decorationFormatter ?: this.decorationFormatter,
            returnValueTransform = returnValueTransform ?: this.returnValueTransform,
            layout = layout ?: this.layout,
            blockStyle = blockStyle ?: this.blockStyle,
            oneLineStyle = oneLineStyle ?: this.oneLineStyle,
            printer = printer ?: this.printer,
        ), default)
    })).invoke(workingDirectory, execTerminationCallback)

    /**
     * Executes the [executable] by processing all [IO] using the given [processor].
     *
     * @param workingDirectory the working directory to be used during execution
     * @param execTerminationCallback called the moment the [Exec] terminates—no matter if the [Exec] succeeds or fails
     * @param nameFormatter convenience way to set [Settings.nameFormatter]
     * @param contentFormatter convenience way to set [Settings.contentFormatter]
     * @param decorationFormatter convenience way to set [Settings.decorationFormatter]
     * @param returnValueTransform convenience way to set [Settings.returnValueTransform]
     * @param layout convenience way to set [Settings.layout]
     * @param blockStyle convenience way to set [Settings.blockStyle]
     * @param oneLineStyle convenience way to set [Settings.oneLineStyle]
     * @param printer convenience way to set [Settings.printer]
     * @param renderer used to render the execution (default: properly nested child renderer)
     * @param processor used to process the [IO] of the execution
     */
    public fun processing(
        workingDirectory: Path? = null,
        execTerminationCallback: ExecTerminationCallback? = null,

        nameFormatter: FilteringFormatter? = null,
        contentFormatter: FilteringFormatter? = null,
        decorationFormatter: Formatter? = null,
        returnValueTransform: ((ReturnValue) -> ReturnValue?)? = null,
        layout: ColumnsLayout? = null,
        blockStyle: ((ColumnsLayout) -> BlockStyle)? = null,
        oneLineStyle: Style? = null,
        printer: Printer? = null,

        renderer: RendererProvider? = { it(this) },
        processor: Processor<E>,
    ): E = copy(
        tracingOptions = tracingOptions.copy(renderer = { default ->
            (renderer ?: tracingOptions.renderer)(copy(
                nameFormatter = nameFormatter ?: this.nameFormatter,
                contentFormatter = contentFormatter ?: this.contentFormatter,
                decorationFormatter = decorationFormatter ?: this.decorationFormatter,
                returnValueTransform = returnValueTransform ?: this.returnValueTransform,
                layout = layout ?: this.layout,
                blockStyle = blockStyle ?: this.blockStyle,
                oneLineStyle = oneLineStyle ?: this.oneLineStyle,
                printer = printer ?: this.printer,
            ), default)
        }),
        processor = processor,
    ).invoke(workingDirectory, execTerminationCallback)

    /**
     * Set the [mode] to [ProcessingMode.Synchronicity.Async].
     */
    public val async: Executor<E> get() = copy(processingMode = ProcessingMode { async })

    internal fun tracer(tracer: Tracer): Executor<E> =
        copy(tracingOptions = tracingOptions.copy(tracer = tracer))

    /**
     * Configures if the execution will be synchronous
     * or asynchronous including mode-specific options, such as if the
     * [Exec]'s [IO] is to be processed non-blocking
     * (default: synchronous execution).
     */
    public fun mode(processingModeInit: ProcessingModeContext.() -> ProcessingMode): Executor<E> =
        copy(processingMode = ProcessingMode(processingModeInit))
}

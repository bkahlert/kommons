package koodies.exec

import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.StatelessBuilder
import koodies.collections.synchronizedSetOf
import koodies.exec.Process.ExitState
import koodies.exec.ProcessingMode.Companion.ProcessingModeContext
import koodies.exec.ProcessingMode.Interactivity
import koodies.exec.ProcessingMode.Interactivity.Interactive
import koodies.exec.ProcessingMode.Interactivity.Interactive.Companion.InteractiveContext
import koodies.exec.ProcessingMode.Interactivity.NonInteractive
import koodies.exec.ProcessingMode.Synchronicity.Async
import koodies.exec.ProcessingMode.Synchronicity.Sync
import koodies.exec.Processors.noopProcessor
import koodies.jvm.completableFuture
import koodies.nio.NonBlockingLineReader
import koodies.nio.NonBlockingReader
import koodies.tracing.CurrentSpan
import koodies.tracing.rendering.RendererProvider
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import org.jline.utils.InputStreamReader as JlineInputStreamReader

/**
 * A function that processes the [IO] of an [Exec].
 */
public typealias Processor<E> = E.(IO) -> Unit

/**
 * All about processing processes.
 */
public object Processors {

    /**
     * A [Processor] that does nothing with the [IO].
     *
     * This processor is suited if the [Exec]'s input and output streams
     * should just be completely consumed—with the side effect of getting logged.
     */
    public fun <E : Exec> noopProcessor(): Processor<E> = { }
}

/**
 * Just consumes the [IO] / depletes the input and output streams
 * so they get logged.
 */
public inline fun <reified E : Exec> E.processSilently(): E =
    process(TracingOptions(null, RendererProviders.NOOP), { async }, noopProcessor())

private val asynchronouslyProcessed: MutableSet<Exec> = synchronizedSetOf()

/**
 * Contains if `this` [Exec] is or was asynchronously processed.
 */
public var Exec.async: Boolean
    get() = this in asynchronouslyProcessed
    private set(value) {
        if (value) asynchronouslyProcessed.add(this) else asynchronouslyProcessed.remove(this)
    }

/**
 * Options for the way the processing of an [Exec] is logged.
 */
public data class TracingOptions(

    /**
     * Name of what is being executed.
     */
    public val name: String? = null,

    /**
     * Renderer to use for logging.
     */
    public val renderer: RendererProvider = { it(this) },

    /**
     * Tracer to be used.
     */
    public val tracer: Tracer = koodies.tracing.Tracer,
) {
    public fun spanning(exec: Exec, block: CurrentSpan.() -> ExitState) {
        koodies.tracing.spanning(name ?: exec.commandLine.summary, renderer, tracer, block)
    }
}

public data class ProcessingMode(
    val synchronicity: Synchronicity,
    val interactivity: Interactivity,
) {

    public val isSync: Boolean get() = synchronicity == Sync

    public enum class Synchronicity { Sync, Async }

    public sealed class Interactivity {
        public class Interactive(public val nonBlocking: Boolean) : Interactivity() {
            public companion object :
                StatelessBuilder.PostProcessing<InteractiveContext, BooleanValue, Interactivity>(InteractiveContext, { Interactive(booleanValue()) }) {
                public object InteractiveContext {
                    public val nonBlocking: BooleanValue = BooleanValue { true }
                    public val blocking: BooleanValue = BooleanValue { false }
                }
            }
        }

        public class NonInteractive(public val execInputStream: InputStream?) : Interactivity()
    }

    public companion object : StatelessBuilder.Returning<ProcessingModeContext, ProcessingMode>(ProcessingModeContext) {
        public object ProcessingModeContext {
            public val sync: ProcessingMode = ProcessingMode(Sync, NonInteractive(null))
            public fun sync(interactivity: Interactivity): ProcessingMode = ProcessingMode(Sync, interactivity)
            public val async: ProcessingMode = ProcessingMode(Async, NonInteractive(null))
            public fun async(interactivity: Interactivity): ProcessingMode = ProcessingMode(Async, interactivity)
        }
    }
}

/**
 * Attaches to the [Exec.outputStream] and [Exec.errorStream]
 * of the specified [Exec] and passed all [IO] to the specified [processor].
 *
 * If no [processor] is specified, the output and the error stream will be
 * printed to the console.
 */
public fun <E : Exec> E.process(
    tracingOptions: TracingOptions,
    modeInit: ProcessingModeContext.() -> ProcessingMode,
    processor: Processor<E>,
): E = process(tracingOptions, ProcessingMode(modeInit), processor)

/**
 * Attaches to the [Exec.outputStream] and [Exec.errorStream]
 * of the specified [Exec] and passed all [IO] to the specified [processor].
 *
 * If no [processor] is specified, the output and the error stream will be
 * printed to the console.
 */
public fun <E : Exec> E.process(
    tracingOptions: TracingOptions = TracingOptions(),
    mode: ProcessingMode = ProcessingMode { sync },
    processor: Processor<E>,
): E = when (mode.synchronicity) {
    Sync -> processSynchronously(tracingOptions, mode.interactivity, processor)
    Async -> processAsynchronously(tracingOptions, mode.interactivity, processor)
}

/**
 * Attaches to the [Process.inputStream] and [Process.errorStream]
 * of the specified [Process] and passed all [IO] to the specified [processor]
 * **synchronously**.
 *
 * If no [processor] is specified a [Processors.eventRecordingProcessor] prints
 * all [IO] to the console.
 */
public fun <P : Exec> P.processSynchronously(
    tracingOptions: TracingOptions = TracingOptions(),
    interactivity: Interactivity = NonInteractive(null),
    processor: Processor<P> = noopProcessor(),
): P = apply {
    tracingOptions.spanning(this) {
        metaStream.subscribe { processor(this@apply, it) }

        val readers = listOf(
            NonBlockingLineReader(outputStream) { line ->
                val output = IO.Output typed line
                event(event = output)
                processor(this@apply, output)
            },
            NonBlockingLineReader(errorStream) { line ->
                val error = IO.Error typed line
                event(event = error)
                processor(this@apply, error)
            },
        )

        if (interactivity is NonInteractive && interactivity.execInputStream != null) {
            interactivity.execInputStream.copyTo(inputStream)
            inputStream.close()
        }
        while (readers.any { !it.done }) {
            readers.filter { !it.done }.forEach { ioReader ->
                ioReader.use { ioReader.read() }
            }
        }

        onExit.join()
    }
}

/**
 * Attaches to the [Exec.outputStream] and [Exec.errorStream]
 * of the specified [Exec] and passed all [IO] to the specified [processor].
 *
 * If no [processor] is specified, the output and the error stream will be
 * printed to the console.
 *
 * TODO try out NIO processing; or just readLines with keepDelimiters respectively EOF as additional line separator
 */
public fun <E : Exec> E.processAsynchronously(
    tracingOptions: TracingOptions = TracingOptions(),
    interactivity: Interactivity = NonInteractive(null),
    processor: Processor<E> = noopProcessor(),
): E = apply {
    val preparationMutex = Semaphore(0) // block until preparation has completed; Exec.onExit must not be called until callbacks are registered
    val exitStateMutex = Semaphore(0)
    val exitStateProcessedMutex = Semaphore(0)
    val threadPool = Context.taskWrapping(Executors.newCachedThreadPool())
    threadPool.completableFuture {
        tracingOptions.spanning(this) {
            async = true
            metaStream.subscribe { processor(this@apply, it) }

            val (execInputStream: InputStream?, nonBlockingReader: Boolean) = when (interactivity) {
                is Interactive -> null to interactivity.nonBlocking
                is NonInteractive -> interactivity.execInputStream to false
            }

            val inputProvider = execInputStream?.run {
                threadPool.completableFuture {
                    use { execInputStream ->
                        var bytesCopied: Long = 0
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytes = execInputStream.read(buffer)
                        while (bytes >= 0) {
                            inputStream.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            bytes = execInputStream.read(buffer)
                        }
                    }
                    inputStream.close()
                }.thenPropagateException("stdin")
            }

            val outputConsumer = threadPool.completableFuture {
                outputStream.readerForStream(nonBlockingReader).forEachLine { line ->
                    val output = IO.Output typed line
                    event(event = output)
                    processor(this@apply, output)
                }
            }.thenPropagateException("stdout")

            val errorConsumer = threadPool.completableFuture {
                errorStream.readerForStream(nonBlockingReader).forEachLine { line ->
                    val error = IO.Error typed line
                    event(event = error)
                    processor(this@apply, error)
                }
            }.thenPropagateException("stderr")

            addPreTerminationCallback {
                CompletableFuture.allOf(*listOfNotNull(inputProvider, outputConsumer, errorConsumer).toTypedArray()).join()
            }

            lateinit var exitState: ExitState
            addPostTerminationCallback {
                exitState = it
                exitStateMutex.release()
                exitStateProcessedMutex.acquire()
            }
            preparationMutex.release()
            exitStateMutex.acquire()
            exitState
        }
        exitStateProcessedMutex.release()
    }
    preparationMutex.acquire()
}

private fun InputStream.readerForStream(nonBlockingReader: Boolean): Reader =
    if (nonBlockingReader) NonBlockingReader(this, blockOnEmptyLine = true)
    else JlineInputStreamReader(this)

private fun CompletableFuture<*>.thenPropagateException(type: String): CompletableFuture<out Any?> = handle { value, exception ->
    if (exception != null) {
        if ((exception.cause is IOException) && exception.cause?.message?.contains("stream closed", ignoreCase = true) == true) value
        else throw RuntimeException("An error occurred while processing [$type].", exception)
    } else {
        value
    }
}

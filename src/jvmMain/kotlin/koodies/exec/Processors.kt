package koodies.exec

import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import koodies.builder.StatelessBuilder
import koodies.exec.IO.Error
import koodies.exec.IO.Output
import koodies.exec.Process.ExitState
import koodies.exec.ProcessingMode.Companion.ProcessingModeContext
import koodies.exec.ProcessingMode.Interactivity
import koodies.exec.ProcessingMode.Interactivity.Interactive
import koodies.exec.ProcessingMode.Interactivity.NonInteractive
import koodies.exec.ProcessingMode.Synchronicity.Async
import koodies.exec.ProcessingMode.Synchronicity.Sync
import koodies.exec.Processors.spanningProcessor
import koodies.jvm.completableFuture
import koodies.nio.NonBlockingLineReader
import koodies.nio.NonBlockingReader
import koodies.tracing.CurrentSpan
import koodies.tracing.Event
import koodies.tracing.Key.KeyValue
import koodies.tracing.RootRenderer
import koodies.tracing.SpanId
import koodies.tracing.TraceId
import koodies.tracing.rendering.RenderableAttributes
import koodies.tracing.rendering.Renderer
import koodies.tracing.rendering.RendererProvider
import koodies.tracing.spanning
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import org.jline.utils.InputStreamReader as JlineInputStreamReader

/**
 * A processor is a function that accepts an [Exec] and
 * an [ExitState] returning function that does the actual work
 * and expect a an [IO] processing callback.
 */
public typealias Processor<E> = (E, ((IO) -> Unit) -> ExitState) -> ExitState

/**
 * All about processing processes.
 */
public object Processors {

    /**
     * Returns a [Processor] that creates a [Span] using the specified [attributes] and [renderer]
     * while calling the specified [process] everytime [IO] was read.
     */
    public fun <E : Exec> spanningProcessor(
        vararg attributes: KeyValue<*, *>,
        renderer: RendererProvider = RendererProviders.NOOP,
        process: CurrentSpan.(E, IO) -> Unit = { _, io -> event(io as Event) },
    ): Processor<E> = { exec: E, block: ((IO) -> Unit) -> ExitState ->
        spanning(
            attributes.firstOrNull { it.key == ExecAttributes.NAME }?.value?.toString() ?: ExecAttributes.SPAN_NAME,
            *attributes,
            renderer = renderer,
            block = { block { process(exec, it) } },
        )
    }

    /**
     * Returns a [Processor] that calls the specified [process] everytime [IO] was read.
     */
    public fun <E : Exec> processingProcessor(
        renderer: RendererProvider = RendererProviders.NOOP,
        process: Renderer.(E, IO) -> Unit = { _, io -> event(io.name, RenderableAttributes.of(*io.attributes.toTypedArray())) },
    ): Processor<E> = { exec: E, block: ((IO) -> Unit) -> ExitState ->
        RootRenderer.childRenderer(renderer).run {
            start(TraceId.invalid, SpanId.invalid, exec.commandLine.name ?: exec.commandLine.content)
            val result: Result<ExitState> = kotlin.runCatching { block { process(exec, it) } }
            end(result)
            result.getOrThrow()
        }
    }
}

/**
 * Just consumes the [IO] / depletes the input and output streams
 * so they get logged.
 */
public inline fun <reified E : Exec> E.processSilently(): E =
    process(ProcessingMode { async }, processor = spanningProcessor())

public data class ProcessingMode(
    val synchronicity: Synchronicity,
    val interactivity: Interactivity,
) {

    public val isSync: Boolean get() = synchronicity == Sync

    public enum class Synchronicity { Sync, Async }

    public sealed class Interactivity {
        public class Interactive(public val nonBlocking: Boolean) : Interactivity()
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
    mode: ProcessingMode = ProcessingMode { sync },
    processor: Processor<E> = spanningProcessor(),
): E = when (mode.synchronicity) {
    Sync -> processSynchronously(mode.interactivity, processor)
    Async -> processAsynchronously(mode.interactivity, processor)
}

/**
 * Attaches to the [Process.inputStream] and [Process.errorStream]
 * of the specified [Process] and passed all [IO] to the specified [processor]
 * **synchronously**.
 *
 * If no [processor] is specified a [Processors.spanningProcessor] traces all [IO].
 */
public fun <E : Exec> E.processSynchronously(
    interactivity: Interactivity = NonInteractive(null),
    processor: Processor<E> = spanningProcessor(),
): E = apply {
    processor(this) { process ->
        metaStream.subscribe { process(it) }

        val readers = listOf(
            NonBlockingLineReader(outputStream) { line ->
                process(Output typed line)
            },
            NonBlockingLineReader(errorStream) { line ->
                process(Error typed line)
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
 * If no [processor] is specified a [Processors.spanningProcessor] traces all [IO].
 *
 * TODO try out NIO processing; or just readLines with keepDelimiters respectively EOF as additional line separator
 */
public fun <E : Exec> E.processAsynchronously(
    interactivity: Interactivity = NonInteractive(null),
    processor: Processor<E> = spanningProcessor(),
): E = apply {
    val preparationMutex = Semaphore(0) // block until preparation has completed; Exec.onExit must not be called until callbacks are registered
    val exitStateMutex = Semaphore(0)
    val exitStateProcessedMutex = Semaphore(0)
    val threadPool = Context.taskWrapping(Executors.newCachedThreadPool())
    threadPool.completableFuture {
        processor(this) { process ->
            metaStream.subscribe { process(it) }

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
                    process(Output typed line)
                }
            }.thenPropagateException("stdout")

            val errorConsumer = threadPool.completableFuture {
                errorStream.readerForStream(nonBlockingReader).forEachLine { line ->
                    process(Error typed line)
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

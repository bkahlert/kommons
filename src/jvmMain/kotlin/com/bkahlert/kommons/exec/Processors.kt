package com.bkahlert.kommons.exec

import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import com.bkahlert.kommons.exec.IO.Error
import com.bkahlert.kommons.exec.IO.Output
import com.bkahlert.kommons.exec.Process.ExitState
import com.bkahlert.kommons.exec.Processors.spanningProcessor
import com.bkahlert.kommons.nio.InputStreamReader
import com.bkahlert.kommons.nio.NonBlockingLineReader
import com.bkahlert.kommons.runtime.completableFuture
import com.bkahlert.kommons.tracing.Event
import com.bkahlert.kommons.tracing.Key.KeyValue
import com.bkahlert.kommons.tracing.RootRenderer
import com.bkahlert.kommons.tracing.SpanId
import com.bkahlert.kommons.tracing.SpanScope
import com.bkahlert.kommons.tracing.TraceId
import com.bkahlert.kommons.tracing.rendering.RenderableAttributes
import com.bkahlert.kommons.tracing.rendering.Renderer
import com.bkahlert.kommons.tracing.rendering.RendererProvider
import com.bkahlert.kommons.tracing.runSpanning
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

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
        process: SpanScope.(E, IO) -> Unit = { _, io -> event(io as Event) },
    ): Processor<E> = { exec: E, start: ((IO) -> Unit) -> ExitState ->
        runSpanning(
            attributes.firstOrNull { it.key == ExecAttributes.NAME }?.value?.toString() ?: ExecAttributes.SPAN_NAME,
            *attributes,
            renderer = renderer,
            block = { start { process(exec, it) } },
        )
    }

    /**
     * Returns a [Processor] that calls the specified [process] everytime [IO] was read.
     */
    public fun <E : Exec> processingProcessor(
        renderer: RendererProvider = RendererProviders.NOOP,
        process: Renderer.(E, IO) -> Unit = { _, io -> event(io.name, RenderableAttributes.of(*io.attributes.toTypedArray())) },
    ): Processor<E> = { exec: E, start: ((IO) -> Unit) -> ExitState ->
        RootRenderer.childRenderer(renderer).run {
            start(TraceId.invalid, SpanId.invalid, exec.commandLine.name ?: exec.commandLine.content)
            val result: Result<ExitState> = kotlin.runCatching { start { process(exec, it) } }
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
    process(ProcessingMode(async = true), processor = spanningProcessor())

/**
 * The way the [IO] of an [Exec] should be processed.
 */
public data class ProcessingMode(
    /**
     * Whether to process asynchronously.
     */
    public val async: Boolean = false,
    /**
     * Optional input stream to connect the [Exec] with.
     */
    public val inputStream: InputStream? = null,
)

/**
 * Attaches to the [Exec.outputStream] and [Exec.errorStream]
 * of the specified [Exec] and passed all [IO] to the specified [processor].
 *
 * If no [processor] is specified, the output and the error stream will be
 * printed to the console.
 */
public fun <E : Exec> E.process(
    mode: ProcessingMode = ProcessingMode(),
    processor: Processor<E> = spanningProcessor(),
): E = when (mode.async) {
    false -> processSynchronously(mode.inputStream, processor)
    true -> processAsynchronously(mode.inputStream, processor)
}

/**
 * Attaches to the [Process.inputStream] and [Process.errorStream]
 * of the specified [Process] and passed all [IO] to the specified [processor]
 * **synchronously**.
 *
 * If no [processor] is specified a [Processors.spanningProcessor] traces all [IO].
 */
public fun <E : Exec> E.processSynchronously(
    execInputStream: InputStream? = null,
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

        execInputStream?.also {
            it.copyTo(inputStream)
            it.close()
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
    execInputStream: InputStream? = null,
    processor: Processor<E> = spanningProcessor(),
): E = apply {
    val preparationMutex = Semaphore(0) // block until preparation has completed; Exec.onExit must not be called until callbacks are registered
    val exitStateMutex = Semaphore(0)
    val exitStateProcessedMutex = Semaphore(0)
    val threadPool = Context.taskWrapping(Executors.newCachedThreadPool())
    threadPool.completableFuture {
        processor(this) { process ->
            metaStream.subscribe { process(it) }

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
                InputStreamReader(outputStream).forEachLine { line ->
                    process(Output typed line)
                }
            }.thenPropagateException("stdout")

            val errorConsumer = threadPool.completableFuture {
                InputStreamReader(errorStream).forEachLine { line ->
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

private fun CompletableFuture<*>.thenPropagateException(type: String): CompletableFuture<out Any?> = handle { value, exception ->
    if (exception != null) {
        if ((exception.cause is IOException) && exception.cause?.message?.contains("stream closed", ignoreCase = true) == true) value
        else throw RuntimeException("An error occurred while processing [$type].", exception)
    } else {
        value
    }
}

package koodies.exec

import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.StatelessBuilder
import koodies.collections.synchronizedSetOf
import koodies.exec.ProcessingMode.Companion.ProcessingModeContext
import koodies.exec.ProcessingMode.Interactivity
import koodies.exec.ProcessingMode.Interactivity.Interactive
import koodies.exec.ProcessingMode.Interactivity.Interactive.Companion.InteractiveContext
import koodies.exec.ProcessingMode.Interactivity.NonInteractive
import koodies.exec.ProcessingMode.Synchronicity.Async
import koodies.exec.ProcessingMode.Synchronicity.Sync
import koodies.exec.Processors.ioProcessingThreadPool
import koodies.exec.Processors.noopProcessor
import koodies.jvm.completableFuture
import koodies.logging.BlockRenderingLogger
import koodies.logging.RenderingLogger
import koodies.logging.logReturnValue
import koodies.nio.NonBlockingLineReader
import koodies.nio.NonBlockingReader
import koodies.runtime.isDebugging
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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
     * Thread pool used for processing the [IO] of [Exec].
     */
    public var ioProcessingThreadPool: ExecutorService = Executors.newCachedThreadPool()

    /**
     * A [Processor] that prints the encountered [IO] using the specified [logger].
     */
    public fun <E : Exec> loggingProcessor(logger: RenderingLogger): Processor<E> = { io ->
        logger.logLine { io }
    }

    /**
     * A [Processor] that does nothing with the [IO].
     *
     * This processor is suited if the [Exec]'s input and output streams
     * should just be completely consumed—with the side effect of getting logged.
     */
    public fun <E : Exec> noopProcessor(): Processor<E> = { }
}

/**
 * Creates a [Processor] that processes [IO] by logging it using the `this` [RenderingLogger].
 *
 * Returns a [Processors.loggingProcessor] if `this` is `null`.
 */
public fun <E : Exec> E.terminationLoggingProcessor(logger: RenderingLogger = BlockRenderingLogger(toString())): Processor<E> {
    addPostTerminationCallback { exitState ->
        if (async) logger.logReturnValue(exitState)
    }
    return Processors.loggingProcessor(logger)
}

/**
 * Just consumes the [IO] / depletes the input and output streams
 * so they get logged.
 */
public inline fun <reified E : Exec> E.processSilently(): E =
    process({ async }, noopProcessor())

private val asynchronouslyProcessed: MutableSet<Exec> = synchronizedSetOf()

/**
 * Contains if `this` [Exec] is or was asynchronously processed.
 */
public var Exec.async: Boolean
    get() = this in asynchronouslyProcessed
    private set(value) {
        if (value) asynchronouslyProcessed.add(this) else asynchronouslyProcessed.remove(this)
    }

public data class ProcessingMode(val synchronicity: Synchronicity, val interactivity: Interactivity) {

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
        @ExecutionDsl
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
    mode: ProcessingModeContext.() -> ProcessingMode,
    processor: Processor<E>,
): E = process(ProcessingMode(mode), processor)

/**
 * Attaches to the [Exec.outputStream] and [Exec.errorStream]
 * of the specified [Exec] and passed all [IO] to the specified [processor].
 *
 * If no [processor] is specified, the output and the error stream will be
 * printed to the console.
 */
public fun <E : Exec> E.process(
    mode: ProcessingMode = ProcessingMode(Sync, NonInteractive(null)),
    processor: Processor<E>,
): E = when (mode.synchronicity) {
    Sync -> processSynchronously(mode.interactivity, processor)
    Async -> processAsynchronously(mode.interactivity, processor)
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
    interactivity: Interactivity = NonInteractive(null),
    processor: Processor<E> = noopProcessor(),
): E = apply {

    async = true
    metaStream.subscribe { processor(this, it) }

    val (execInputStream: InputStream?, nonBlockingReader: Boolean) = when (interactivity) {
        is Interactive -> null to interactivity.nonBlocking
        is NonInteractive -> interactivity.execInputStream to false
    }

    val inputProvider = execInputStream?.run {
        ioProcessingThreadPool.completableFuture {
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

    val outputConsumer = ioProcessingThreadPool.completableFuture {
        outputStream.readerForStream(nonBlockingReader).forEachLine { line ->
            processor(this, IO.Output typed line)
        }
    }.thenPropagateException("stdout")

    val errorConsumer = ioProcessingThreadPool.completableFuture {
        errorStream.readerForStream(nonBlockingReader).forEachLine { line ->
            processor(this, IO.Error typed line)
        }
    }.thenPropagateException("stderr")

    addPreTerminationCallback {
        CompletableFuture.allOf(*listOfNotNull(inputProvider, outputConsumer, errorConsumer).toTypedArray()).join()
    }

    ioProcessingThreadPool.completableFuture {
        kotlin.runCatching { onExit.join() }.onFailure { if (isDebugging) throw it }
    }
}

/**
 * Attaches to the [Process.inputStream] and [Process.errorStream]
 * of the specified [Process] and passed all [IO] to the specified [processor]
 * **synchronously**.
 *
 * If no [processor] is specified a [Processors.loggingProcessor] prints
 * all [IO] to the console.
 */
public fun <P : Exec> P.processSynchronously(
    interactivity: Interactivity = NonInteractive(null),
    processor: Processor<P> = terminationLoggingProcessor(),
): P {

    metaStream.subscribe { processor(this, it) }

    val readers = listOf(
        NonBlockingLineReader(outputStream) { line -> processor(this, IO.Output typed line) },
        NonBlockingLineReader(errorStream) { line -> processor(this, IO.Error typed line) },
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

    return this
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

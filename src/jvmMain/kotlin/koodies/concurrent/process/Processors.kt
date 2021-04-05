package koodies.concurrent.process

import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.StatelessBuilder
import koodies.collections.synchronizedSetOf
import koodies.concurrent.ExecutionDsl
import koodies.concurrent.completableFuture
import koodies.concurrent.process.ProcessingMode.Companion.ProcessingModeContext
import koodies.concurrent.process.ProcessingMode.Interactivity
import koodies.concurrent.process.ProcessingMode.Interactivity.Interactive
import koodies.concurrent.process.ProcessingMode.Interactivity.Interactive.Companion.InteractiveContext
import koodies.concurrent.process.ProcessingMode.Interactivity.NonInteractive
import koodies.concurrent.process.ProcessingMode.Synchronicity.Async
import koodies.concurrent.process.ProcessingMode.Synchronicity.Sync
import koodies.concurrent.process.Processors.ioProcessingThreadPool
import koodies.concurrent.process.Processors.noopProcessor
import koodies.logging.BlockRenderingLogger
import koodies.logging.RenderingLogger
import koodies.logging.logReturnValue
import koodies.nio.NonBlockingLineReader
import koodies.nio.NonBlockingReader
import koodies.runtime.Program
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.jline.utils.InputStreamReader as JlineInputStreamReader

/**
 * A function that processes the [IO] of a [Process].
 */
public typealias Processor<P> = P.(IO) -> Unit

/**
 * All about processing processes.
 */
public object Processors {

    /**
     * Thread pool used for processing the [IO] of [Process].
     */
    public var ioProcessingThreadPool: ExecutorService = Executors.newCachedThreadPool()

    /**
     * A [Processor] that prints the encountered [IO] using the specified [logger].
     */
    public fun <P : Process> loggingProcessor(logger: RenderingLogger): Processor<P> = { io ->
        logger.logLine { io }
    }

    /**
     * A [Processor] that does nothing with the [IO].
     *
     * This processor is suited if the process's input and output streams
     * should just be completely consumed—with the side effect of getting logged.
     */
    public fun <P : Process> noopProcessor(): Processor<P> = { }
}

/**
 * Creates a [Processor] that processes [IO] by logging it using the `this` [RenderingLogger].
 *
 * Returns a [Processors.loggingProcessor] if `this` is `null`.
 */
public fun <P : Process> P.terminationLoggingProcessor(logger: RenderingLogger = BlockRenderingLogger(toString())): Processor<P> {
    (this as? ManagedProcess)?.addPostTerminationCallback { terminated ->
        if (async) {
            logger.logReturnValue(terminated)
        }
    } ?: error("process can't be attached to")
    return Processors.loggingProcessor(logger)
}

/**
 * Just consumes the [IO] / depletes the input and output streams
 * so they get logged.
 */
public inline fun <reified P : ManagedProcess> P.processSilently(): P =
    process({ async }, noopProcessor())

private val asynchronouslyProcessed: MutableSet<Process> = synchronizedSetOf()

/**
 * Contains if `this` process is or was asynchronously processed.
 */
public var Process.async: Boolean
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

        public class NonInteractive(public val processInputStream: InputStream?) : Interactivity()
    }

    public companion object : StatelessBuilder.Returning<ProcessingModeContext, ProcessingMode>(ProcessingModeContext) {
        @ExecutionDsl
        public object ProcessingModeContext {
            public val sync: ProcessingMode = ProcessingMode(Sync, NonInteractive(null))
            public val async: ProcessingMode = ProcessingMode(Async, NonInteractive(null))
            public fun async(interactivity: Interactivity): ProcessingMode = ProcessingMode(Async, interactivity)
        }
    }
}

/**
 * Attaches to the [Process.inputStream] and [Process.errorStream]
 * of the specified [Process] and passed all [IO] to the specified [processor].
 *
 * If no [processor] is specified, the output and the error stream will be
 * printed to the console.
 *
 *
 */
public fun <P : ManagedProcess> P.process(
    mode: ProcessingModeContext.() -> ProcessingMode,
    processor: Processor<P>,
): P = process(ProcessingMode(mode), processor)

/**
 * Attaches to the [Process.inputStream] and [Process.errorStream]
 * of the specified [Process] and passed all [IO] to the specified [processor].
 *
 * If no [processor] is specified, the output and the error stream will be
 * printed to the console.
 *
 *
 */
public fun <P : ManagedProcess> P.process(
    mode: ProcessingMode = ProcessingMode(Sync, NonInteractive(null)),
    processor: Processor<P>,
): P = when (mode.synchronicity) {
    Sync -> processSynchronously(mode.interactivity, processor)
    Async -> processAsynchronously(mode.interactivity, processor)
}


/**
 * Attaches to the [Process.inputStream] and [Process.errorStream]
 * of the specified [Process] and passed all [IO] to the specified [processor].
 *
 * If no [processor] is specified, the output and the error stream will be
 * printed to the console.
 *
 * TOOD try out NIO processing; or just readLines with keepDelimiters respectively EOF as additional line separator
 */
public fun <P : ManagedProcess> P.processAsynchronously(
    interactivity: Interactivity = NonInteractive(null),
    processor: Processor<P> = noopProcessor(),
): P = apply {

    async = true
    metaStream.subscribe { processor(this, it) }

    val (processInputStream: InputStream?, nonBlockingReader) = when (interactivity) {
        is Interactive -> InputStream.nullInputStream() to interactivity.nonBlocking
        is NonInteractive -> interactivity.processInputStream to false
    }

    val inputProvider = if (processInputStream != null) {
        ioProcessingThreadPool.completableFuture {
            processInputStream.use {
                var bytesCopied: Long = 0
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytes = it.read(buffer)
                while (bytes >= 0) {
                    inputStream.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    bytes = it.read(buffer)
                }
            }
            inputStream.close()
        }.exceptionallyThrow("stdin")
    } else {
        null
    }

    val outputConsumer = ioProcessingThreadPool.completableFuture {
        outputStream.readerForStream(nonBlockingReader).forEachLine { line ->
            processor(this, IO.OUT typed line)
        }
    }.exceptionallyThrow("stdout")

    val errorConsumer = ioProcessingThreadPool.completableFuture {
        errorStream.readerForStream(nonBlockingReader).forEachLine { line ->
            processor(this, IO.ERR typed line)
        }
    }.exceptionallyThrow("stderr")

    addPreTerminationCallback {
        CompletableFuture.allOf(*listOfNotNull(inputProvider, outputConsumer, errorConsumer).toTypedArray()).join()
    }

    ioProcessingThreadPool.completableFuture {
        kotlin.runCatching { onExit.join() }
            .onFailure { if (Program.isDebugging) throw it }
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
public fun <P : ManagedProcess> P.processSynchronously(
    interactivity: Interactivity = NonInteractive(null),
    processor: Processor<P> = terminationLoggingProcessor(),
): P {

    metaStream.subscribe { processor(this, it) }

    val readers = listOf(
        NonBlockingLineReader(outputStream) { line -> processor(this, IO.OUT typed line) },
        NonBlockingLineReader(errorStream) { line -> processor(this, IO.ERR typed line) },
    )

    if (interactivity is NonInteractive && interactivity.processInputStream != null) {
        interactivity.processInputStream.copyTo(inputStream)
        inputStream.close()
    }
    while (readers.any { !it.done }) {
        readers.filter { !it.done }.forEach { ioReader ->
            ioReader.read()
        }
    }

    onExit.join()

    return this
}

private fun InputStream.readerForStream(nonBlockingReader: Boolean): Reader =
    if (nonBlockingReader) NonBlockingReader(this, blockOnEmptyLine = true)
    else JlineInputStreamReader(this)

private fun CompletableFuture<*>.exceptionallyThrow(type: String): CompletableFuture<out Any?> = handle { value, exception ->
    if (exception != null) {
        if ((exception.cause is IOException) && exception.cause?.message?.contains("stream closed", ignoreCase = true) == true) value
        else throw RuntimeException("An error occurred while processing ［$type］.", exception)
    } else {
        value
    }
}

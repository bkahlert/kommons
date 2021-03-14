package koodies.concurrent.process

import koodies.Exceptions
import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.StatelessBuilder
import koodies.concurrent.completableFuture
import koodies.concurrent.process.ProcessingMode.Companion.ProcessingModeContext
import koodies.concurrent.process.ProcessingMode.Interactivity
import koodies.concurrent.process.ProcessingMode.Interactivity.Interactive
import koodies.concurrent.process.ProcessingMode.Interactivity.Interactive.Companion.InteractiveContext
import koodies.concurrent.process.ProcessingMode.Interactivity.NonInteractive
import koodies.concurrent.process.ProcessingMode.Synchronicity.Async
import koodies.concurrent.process.ProcessingMode.Synchronicity.Sync
import koodies.concurrent.process.Processors.consoleLoggingProcessor
import koodies.concurrent.process.Processors.ioProcessingThreadPool
import koodies.concurrent.process.Processors.noopProcessor
import koodies.logging.BlockRenderingLogger
import koodies.logging.RenderingLogger
import koodies.nio.NonBlockingLineReader
import koodies.nio.NonBlockingReader
import java.io.InputStream
import java.io.Reader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
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
        when (io.type) {
            IO.Type.META -> logger.logLine { io }
            IO.Type.IN -> logger.logLine { io }
            IO.Type.OUT -> logger.logLine { io }
            IO.Type.ERR -> logger.logLine { io.formatted }
        }
    }

    /**
     * A [Processor] that prints all [IO] to the console.
     */
    public fun <P : Process> consoleLoggingProcessor(): Processor<P> {
        return object : (P, IO) -> Unit {
            private lateinit var process: P
            private val logger by lazy { BlockRenderingLogger(process.toString()) }
            private val loggingProcessor by lazy { loggingProcessor<P>(logger) }
            override fun invoke(process: P, io: IO) {
                this.process = process
                loggingProcessor.invoke(process, io)
            }
        }
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
 * Returns a [Processors.consoleLoggingProcessor] if `this` is `null`.
 */
public fun <P : Process> RenderingLogger?.toProcessor(): Processor<P> =
    this?.let { Processors.loggingProcessor(it) } ?: consoleLoggingProcessor()

/**
 * Just consumes the [IO] / depletes the input and output streams
 * so they get logged.
 */
public inline fun <reified P : ManagedProcess> P.processSilently(): P =
    process({ sync }, noopProcessor())


public data class ProcessingMode(val synchronicity: Synchronicity, val interactivity: Interactivity) {

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
    processor: Processor<P> = consoleLoggingProcessor(),
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
    interactivity: Interactivity,
    processor: Processor<P> = noopProcessor(),
): P = apply {

    inputCallback = { line -> // not guaranteed to be a line -> TODO buffer until it is one
        processor(this, line.type typed line.trim())
    }

    val (processInputStream, nonBlockingReader) = when (interactivity) {
        is Interactive -> InputStream.nullInputStream() to interactivity.nonBlocking
        is NonInteractive -> (interactivity.processInputStream ?: InputStream.nullInputStream()) to false
    }

    val inputProvider = ioProcessingThreadPool.completableFuture {
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
    }.exceptionallyThrow("stdin")

    val outputConsumer = ioProcessingThreadPool.completableFuture {
        outputStream.readerForStream(nonBlockingReader).forEachLine { line ->
            processor(this, IO.Type.OUT typed line)
        }
    }.exceptionallyThrow("stdout")

    val errorConsumer = ioProcessingThreadPool.completableFuture {
        errorStream.readerForStream(nonBlockingReader).forEachLine { line ->
            processor(this, IO.Type.ERR typed line)
        }
    }.exceptionallyThrow("stderr")

    externalSync = CompletableFuture.allOf(inputProvider, outputConsumer, errorConsumer)
}

/**
 * Attaches to the [Process.inputStream] and [Process.errorStream]
 * of the specified [Process] and passed all [IO] to the specified [processor]
 * **synchronously**.
 *
 * If no [processor] is specified a [Processors.consoleLoggingProcessor] prints
 * all [IO] to the console.
 */
public fun <P : ManagedProcess> P.processSynchronously(
    interactivity: Interactivity,
    processor: Processor<P> = consoleLoggingProcessor(),
): P = apply {

    if (interactivity is Interactive && !interactivity.nonBlocking) {
        // TOOD try out NIO processing; or just readLines with keepDelimiters respectively EOF as additional line separator
        throw Exceptions.ISE("Non-blocking synchronous processing is not yet supported.")
    }

    if (interactivity is NonInteractive && interactivity.processInputStream != null) {
        throw Exceptions.ISE("Input stream provided synchronous processing is not yet supported.")
    }

    val metaAndInputIO = mutableListOf<IO>()
    val metaAndInputIOLock = ReentrantLock()

    inputCallback = { line -> // not guaranteed to be a line -> TODO buffer until it is one
        metaAndInputIOLock.withLock { metaAndInputIO.add(line) }
    }

    val readers = listOf(
        NonBlockingLineReader(outputStream) { line -> processor(this, IO.Type.OUT typed line) },
        NonBlockingLineReader(errorStream) { line -> processor(this, IO.Type.ERR typed line) },
    )

    // hacky since that code assumes there is meta logging, IO and finally some meta
    // (which is true at the time of writing); otherwise the termination confirmation might slip in between
    metaAndInputIOLock.withLock {
        while (metaAndInputIO.isNotEmpty()) { // coming from 👆
            val line = metaAndInputIO.removeFirst()
            processor(this, line.type typed line.trim())
        }
    }
    while (readers.any { !it.done }) {
        readers.filter { !it.done }.forEach { ioReader ->
            ioReader.read()
        }
    }
    metaAndInputIOLock.withLock {
        while (metaAndInputIO.isNotEmpty()) {  // coming from 👆
            val line = metaAndInputIO.removeFirst()
            processor(this, line.type typed line.trim())
        }
    }

    onExit.join()
}

private fun InputStream.readerForStream(nonBlockingReader: Boolean): Reader =
    if (nonBlockingReader) NonBlockingReader(this, blockOnEmptyLine = true)
    else JlineInputStreamReader(this)

private fun CompletableFuture<*>.exceptionallyThrow(type: String) = exceptionally {
    throw RuntimeException("An error occurred while processing ［$type］.", it)
}

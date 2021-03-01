package koodies.concurrent.process

import koodies.concurrent.completableFuture
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
typealias Processor<P> = P.(IO) -> Unit

/**
 * All about processing processes.
 */
object Processors {
    /**
     * Thread pool used for processing the [IO] of [Process].
     */
    var ioProcessingThreadPool: ExecutorService = Executors.newCachedThreadPool()

    /**
     * A [Processor] that prints the encountered [IO] using the specified [logger].
     */
    fun <P : Process> loggingProcessor(logger: RenderingLogger): Processor<P> = { io ->
        when (io.type) {
            IO.Type.META -> logger.logLine { io }
            IO.Type.IN -> logger.logLine { io }
            IO.Type.OUT -> logger.logLine { io }
            IO.Type.ERR -> logger.logLine { "Unfortunately an error occurred: ${io.formatted}" }
        }
    }

    /**
     * A [Processor] that prints all [IO] to the console.
     */
    fun <P : Process> consoleLoggingProcessor(): Processor<P> {
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
    fun <P : Process> noopProcessor(): Processor<P> = { }
}

/**
 * Creates a [Processor] that processes [IO] by logging it using the `this` [RenderingLogger].
 *
 * Returns a [Processors.consoleLoggingProcessor] if `this` is `null`.
 */
fun <P : Process> RenderingLogger?.toProcessor(): Processor<P> =
    this?.let { Processors.loggingProcessor(it) } ?: consoleLoggingProcessor()

/**
 * Just consumes the [IO] / depletes the input and output streams
 * so they get logged.
 */
inline fun <reified P : ManagedProcess> P.processSilently(): P =
    process(false, InputStream.nullInputStream(), noopProcessor())

/**
 * Attaches to the [Process.inputStream] and [Process.errorStream]
 * of the specified [Process] and passed all [IO] to the specified [processor].
 *
 * If no [processor] is specified a [Processors.consoleLoggingProcessor] prints
 * all [IO] to the console.
 *
 * TOOD try out NIO processing; or just readLines with keepDelimiters respectively EOF as additional line separator
 */
fun <P : ManagedProcess> P.process(processor: Processor<P> = consoleLoggingProcessor()): P =
    process(true, InputStream.nullInputStream(), processor)

/**
 * Attaches to the [Process.inputStream] and [Process.errorStream]
 * of the specified [Process] and passed all [IO] to the specified [processor].
 *
 * If no [processor] is specified, the output and the error stream will be
 * printed to the console.
 *
 * TOOD try out NIO processing; or just readLines with keepDelimiters respectively EOF as additional line separator
 */
fun <P : ManagedProcess> P.process(
    nonBlockingReader: Boolean,
    processInputStream: InputStream = InputStream.nullInputStream(),
    processor: Processor<P> = noopProcessor(),
): P = apply {

    inputCallback = { line -> // not guaranteed to be a line -> TODO buffer until it is one
        processor(this, line.type typed line.trim())
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
fun <P : ManagedProcess> P.processSynchronously(
    processor: Processor<P> = consoleLoggingProcessor(),
): P = apply {
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

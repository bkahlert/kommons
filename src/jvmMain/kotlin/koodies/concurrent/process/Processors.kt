package koodies.concurrent.process

import koodies.concurrent.completableFuture
import koodies.concurrent.process.Processors.ioProcessingThreadPool
import koodies.concurrent.process.Processors.noopProcessor
import koodies.logging.RenderingLogger
import koodies.nio.NonBlockingReader
import koodies.text.LineSeparators.hasTrailingLineSeparator
import koodies.text.LineSeparators.lines
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.toByteArray
import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.Reader
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
     * A [Processor] that prints the encountered [IO] to the console.
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
     * A [Processor] that does nothing with the [IO].
     *
     * This processor is suited if the process's input and output streams
     * should just be completely consumed—with the side effect of getting logged.
     */
    fun <P : Process> noopProcessor(): Processor<P> =
        { _ -> }
}

/**
 * Just consumes the [IO] / depletes the input and output streams
 * so they get logged.
 */
inline fun <reified P : ManagedProcess> P.silentlyProcess(): ManagedProcess =
    process(false, InputStream.nullInputStream(), noopProcessor())


/**
 * Attaches to the [Process.outputStream] and [Process.errorStream]
 * of the specified [Process] and passed all [IO] to the specified [processor].
 *
 * If no [processor] is specified, the output and the error stream will be
 * printed to the console.
 *
 * TOOD try out NIO processing; or just readLines with keepDelimiters respectively EOF as additional line separator
 */
fun <P : ManagedProcess> P.process(processor: Processor<P>): P =
    process(true, InputStream.nullInputStream(), processor)

/**
 * Attaches to the [Process.outputStream] and [Process.errorStream]
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
): P {

    return apply {

//        val metaConsumer = metaStream. // TODO meta and info reading

        val inputProvider = ioProcessingThreadPool.completableFuture {
            processInputStream.use {
                var bytesCopied: Long = 0
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytes = it.read(buffer)
                while (bytes >= 0) {
                    outputStream.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    bytes = it.read(buffer)
                }
            }
        }.exceptionallyThrow("stdin")

        val outputConsumer = ioProcessingThreadPool.completableFuture {
            inputStream.readerForStream(nonBlockingReader).forEachLine { line ->
                processor(this@process, IO.Type.OUT typed line)
            }
        }.exceptionallyThrow("stdout")

        val errorConsumer = ioProcessingThreadPool.completableFuture {
            errorStream.readerForStream(nonBlockingReader).forEachLine { line ->
                processor(this@process, IO.Type.ERR typed line)
            }
        }.exceptionallyThrow("stderr")

        this@process.externalSync = CompletableFuture.allOf(inputProvider, outputConsumer, errorConsumer)
    }
}


/**
 * Attaches to the [Process.outputStream] and [Process.errorStream]
 * of the specified [Process] and passed all [IO] to the specified [processor]
 * **synchronously**.
 *
 * If no [processor] is specified, the output and the error stream will be
 * printed to the console.
 */
fun <P : ManagedProcess> P.processSynchronously(
//    processInputStream: InputStream = InputStream.nullInputStream(),
    processor: Processor<P> = noopProcessor(),
): P = apply {

    val ioProcessors = listOf(
//        NonBlockingInputStreamProcessor(processInputStream, outputStream),
        NonBlockingInputStreamLineProcessor(inputStream) { line -> processor(this, IO.Type.OUT typed line) },
        NonBlockingInputStreamLineProcessor(errorStream) { line -> processor(this, IO.Type.ERR typed line) },
    )

    while (ioProcessors.any { !it.finished }) {
        ioProcessors.filter { !it.finished }.forEach { ioReader ->
            ioReader.process()
        }
    }
}

internal interface NonBlockingProcessor {
    val finished: Boolean
    fun process()
}

internal open class NonBlockingInputStreamProcessor(
    inputStream: InputStream,
    outputStream: OutputStream,
) : NonBlockingProcessor {
    private val readBuffer: ByteBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
    private val readChannel: ReadableByteChannel = Channels.newChannel(inputStream)
    private val writeChannel: WritableByteChannel = Channels.newChannel(outputStream)

    private var closed = false
    override val finished: Boolean get() = closed

    override fun process() {
        while (true) {
            kotlin.runCatching {
                when (readChannel.read(readBuffer)) {
                    -1 -> {
                        closed = true
                        readChannelRead()
                        return
                    }
                    0 -> {
                        readChannelRead()
                        return
                    }
                    else -> {
                        readBuffer.flip()
                        writeChannel.write(readBuffer)
                        readBuffer.clear()
                        readChannelRead()
                    }
                }
            }.onFailure {
                closed = true
                readChannelRead()
                return
            }
        }
    }

    protected open fun readChannelRead() {}
}

internal class NonBlockingInputStreamLineProcessor(
    inputStream: InputStream,
    private val lineBuffer: ByteArrayOutputStream = ByteArrayOutputStream(),
    private val lineProcessor: (String) -> Unit,
) : NonBlockingInputStreamProcessor(inputStream, lineBuffer) {

    override fun readChannelRead() {
        val fullyRead: StringBuilder = StringBuilder()
        val read = lineBuffer.toString(Charsets.UTF_8)
        read.lines(keepDelimiters = true, ignoreTrailingSeparator = true)
            .filter { line -> line.hasTrailingLineSeparator }
            .forEach { line ->
                fullyRead.append(line)
                lineProcessor(line.withoutTrailingLineSeparator)
            }
        lineBuffer.toByteArray().apply {
            lineBuffer.reset()
            lineBuffer.write(drop(fullyRead.toByteArray().size).toByteArray())
        }
    }

    override fun toString(): String =
        "Reader(lineBuffer=$lineBuffer)"
}

private fun InputStream.readerForStream(nonBlockingReader: Boolean): Reader =
    if (nonBlockingReader) NonBlockingReader(this, blockOnEmptyLine = true)
    else InputStreamReader(this)

private fun CompletableFuture<*>.exceptionallyThrow(type: String) = exceptionally {
    throw RuntimeException("An error occurred while processing ［$type］.", it)
}

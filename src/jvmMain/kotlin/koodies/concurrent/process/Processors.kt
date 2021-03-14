package koodies.concurrent.process

import koodies.builder.BuilderTemplate
import koodies.builder.Init
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.concurrent.completableFuture
import koodies.concurrent.process.ProcessingOptions.Mode.Asynchronous
import koodies.concurrent.process.ProcessingOptions.Mode.Synchronous
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
public fun <P : ManagedProcess> P.process(processor: Processor<P> = consoleLoggingProcessor()): P =
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
public fun <P : ManagedProcess> P.process(
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
public fun <P : ManagedProcess> P.processSynchronously(
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


public data class ProcessingOptions<P : ManagedProcess>(
    val mode: Mode,
    val processInputStream: InputStream = InputStream.nullInputStream(),
    val processor: Processor<P>,
) {

    public enum class Mode { Synchronous, Asynchronous }

    public companion object {
        public operator fun <P : ManagedProcess> invoke(init: Init<Builder<P>.ProcessingOptionsContext>): ProcessingOptions<P> =
            Builder<P>().invoke(init)

        public class Builder<P : ManagedProcess> : BuilderTemplate<Builder<P>.ProcessingOptionsContext, ProcessingOptions<P>>() {

            public inner class ProcessingOptionsContext(override val captures: CapturesMap) : CapturingContext() {
                public val mode: SkippableCapturingBuilderInterface<() -> Mode, Mode> by builder<Mode>() default Asynchronous
                public val processInputStream: SkippableCapturingBuilderInterface<() -> InputStream, InputStream> by builder<InputStream>() default InputStream.nullInputStream()
                public val processor: SkippableCapturingBuilderInterface<() -> P.(IO) -> Unit, P.(IO) -> Unit> by builder<Processor<P>>() default noopProcessor()
            }

            override fun BuildContext.build(): ProcessingOptions<P> = ::ProcessingOptionsContext {
                ProcessingOptions(::mode.eval(), ::processInputStream.eval(), ::processor.eval())
            }
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
 * TOOD try out NIO processing; or just readLines with keepDelimiters respectively EOF as additional line separator
 */
public fun <P : ManagedProcess> P.process(options: ProcessingOptions<P>): P =
    with(options) {
        when (mode) {
            Synchronous -> processSynchronously(processor)
            Asynchronous -> process(nonBlockingReader = true, processInputStream, processor)
        }
    }

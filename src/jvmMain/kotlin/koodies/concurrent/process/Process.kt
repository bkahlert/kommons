package koodies.concurrent.process

import koodies.concurrent.process.UserInput.enter
import koodies.debug.asEmoji
import koodies.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import java.lang.Process as JavaProcess


/**
 * Platform independent representation of a running program.
 */
public interface Process {
    /**
     * Logs information about this process.
     */
    public fun metaLog(metaMessage: String): Unit = metaStream.enter(metaMessage, delay = Duration.ZERO)

    /**
     * Stream of this program's meta logs.
     */
    public val metaStream: OutputStream

    /**
     * This program's input stream, that is,
     * the stream you can write to in order
     * to provide input for this program.
     *
     * *In contrast to the extremely misleading naming used in the Java world,
     * this property is named after its purpose—not its type.*
     */
    public val inputStream: OutputStream

    /**
     * This program's output stream, that is,
     * the stream you can read in order to find
     * out what [IO] this process produces.
     *
     * *In contrast to the extremely misleading naming used in the Java world,
     * this property is named after its purpose—not its type.*
     */
    public val outputStream: InputStream

    /**
     * This program's error stream, that is,
     * the stream you can read in order to find
     * out what goes wrong.
     */
    public val errorStream: InputStream

    /**
     * The identifier of this process.
     */
    public val pid: Long

    /**
     * Starts the program represented by this process.
     */
    public fun start(): Process

    /**
     * Returns whether [start] was called.
     *
     * Contrary to [alive] this property will never return `false` once [start] was called.
     */
    public val started: Boolean

    /**
     * Returns whether the program represented by this process
     * is currently running.
     *
     * Contrary to [started] this property reflects the actual running state of
     * the program represented by this process.
     */
    public val alive: Boolean

    /**
     * Returns the exit code of the program represented by process process once
     * it terminates. If the program has not terminated yet, it throws an
     * [IllegalStateException].
     */
    public val exitValue: Int

    /**
     * A completable future that returns an instances of this process once
     * the program represented by this process terminated.
     */
    public val onExit: CompletableFuture<out Process>

    /**
     * Blocking method that waits until the program represented by this process
     * terminates and returns its [exitValue].
     */
    public fun waitFor(): Int = onExit.join().exitValue

    /**
     * Blocking method that waits until the program represented by this process
     * terminates and returns its [exitValue].
     */
    public fun waitForTermination(): Int = onExit.thenApply { process -> process.exitValue }.join()

    /**
     * Gracefully attempts to stop the execution of the program represented by this process.
     */
    public fun stop(): Process

    /**
     * Forcefully stops the execution of the program represented by this process.
     */
    public fun kill(): Process
}

/**
 * A process that delegates to the [JavaProcess] provided by the specified [processProvider].
 */
public abstract class DelegatingProcess(private val processProvider: Process.() -> JavaProcess) : Process {
    override val metaStream: OutputStream by lazy { ByteArrayOutputStream() }
    override val inputStream: OutputStream by lazy { javaProcess.outputStream }
    override val outputStream: InputStream by lazy { javaProcess.inputStream }
    override val errorStream: InputStream by lazy { javaProcess.errorStream }
    override val pid: Long by lazy { javaProcess.pid() }

    /**
     * The Java process this process delegates to.
     */
    protected val javaProcess: JavaProcess by lazy { this.processProvider().also { _started = true } }
    override fun start(): Process = this.also { javaProcess.pid() }
    private var _started: Boolean = false
    override val started: Boolean get() = _started
    override val alive: Boolean get() = javaProcess.isAlive
    override val exitValue: Int get() = javaProcess.exitValue()
    abstract override val onExit: CompletableFuture<Process>
    override fun waitFor(): Int = onExit.join().exitValue
    override fun stop(): Process = also { javaProcess.destroy() }
    override fun kill(): Process = also { javaProcess.destroyForcibly() }

    protected open val preparedToString: StringBuilder /* = java.lang.StringBuilder */ = StringBuilder().apply { append(" started=${started}") }
    override fun toString(): String {
        val delegateString =
            if (started) "$javaProcess; result=${onExit.isCompletedExceptionally.not().asEmoji}"
            else "not yet initialized"
        return "${this::class.simpleName}[delegate=$delegateString;$preparedToString]"
    }
}

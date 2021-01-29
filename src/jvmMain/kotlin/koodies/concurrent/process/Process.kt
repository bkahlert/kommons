package koodies.concurrent.process

import koodies.concurrent.process.UserInput.enter
import koodies.debug.asEmoji
import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import java.lang.Process as JavaProcess

internal open class TeeOutputStream(source: OutputStream, branch: OutputStream, vararg branches: OutputStream) :
    org.apache.commons.io.output.TeeOutputStream(source,
        if (branches.isEmpty()) branch
        else branches.fold(branch) { teeOutputStream, outputStream -> TeeOutputStream(teeOutputStream, outputStream) }
    )

internal class TeeInputStream(inputStream: InputStream, branch: OutputStream) : org.apache.commons.io.input.TeeInputStream(inputStream, branch, false)

/**
 * Platform independent representation of a process.
 */
interface Process {
    fun metaLog(metaMessage: String) = metaStream.enter(metaMessage, delay = Duration.ZERO)
    val metaStream: OutputStream
    val outputStream: OutputStream
    val inputStream: InputStream
    val errorStream: InputStream
    val pid: Long
    fun start(): Process
    val started: Boolean
    val alive: Boolean
    val exitValue: Int
    val onExit: CompletableFuture<out Process>
    fun waitFor(): Int = onExit.join().exitValue
    fun waitForTermination(): Int = onExit.thenApply { process -> process.exitValue }.join()
    fun stop(): Process
    fun kill(): Process
}

abstract class DelegatingProcess(private val processFactory: Process.() -> JavaProcess) : Process {
    override val metaStream: OutputStream by lazy { ByteArrayOutputStream() }
    override val outputStream: OutputStream by lazy { javaProcess.outputStream }
    override val inputStream: InputStream by lazy { javaProcess.inputStream }
    override val errorStream: InputStream by lazy { javaProcess.errorStream }
    override val pid: Long by lazy { javaProcess.pid() }
    protected val javaProcess: JavaProcess by lazy { this.processFactory().also { _started = true } }
    override fun start(): Process = this.also { javaProcess.pid() }
    private var _started: Boolean = false
    override val started: Boolean get() = _started
    override val alive: Boolean get() = javaProcess.isAlive
    override val exitValue: Int get() = javaProcess.exitValue()
    abstract override val onExit: CompletableFuture<Process>
    override fun waitFor(): Int = onExit.join().exitValue
    override fun stop(): Process = also { javaProcess.destroy() }
    override fun kill(): Process = also { javaProcess.destroyForcibly() }

    protected open val preparedToString = StringBuilder().apply { append(" started=${started}") }
    override fun toString(): String {
        val delegateString =
            if (started) "$javaProcess; result=${onExit.isCompletedExceptionally.not().asEmoji}"
            else "not yet initialized"
        return "${this::class.simpleName}[delegate=$delegateString;$preparedToString]"
    }
}

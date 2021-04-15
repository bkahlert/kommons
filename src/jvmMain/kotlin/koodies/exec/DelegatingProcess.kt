package koodies.concurrent.process

import koodies.debug.asEmoji
import koodies.exec.MetaStream
import koodies.exec.Process.ExitState
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture

/**
 * A process that delegates to the [JavaProcess] provided by the specified [processProvider].
 */
@Deprecated("remove")
public abstract class DelegatingProcess(private val processProvider: koodies.exec.Process.() -> Process) : koodies.exec.Process {
    override val metaStream: MetaStream = MetaStream()
    override val inputStream: OutputStream by lazy { javaProcess.outputStream }
    override val outputStream: InputStream by lazy { javaProcess.inputStream }
    override val errorStream: InputStream by lazy { javaProcess.errorStream }
    override val pid: Long by lazy { javaProcess.pid() }

    /**
     * The Java process this process delegates to.
     */
    protected val javaProcess: Process by lazy { processProvider().also { _started = true } }

    override fun start(): koodies.exec.Process = also { javaProcess.pid() }
    private var _started: Boolean = false
    override val started: Boolean get() = _started
    override val alive: Boolean get() = started && javaProcess.isAlive
    override val exitValue: Int get() = javaProcess.exitValue()
    abstract override val onExit: CompletableFuture<out ExitState>
    override fun waitFor(): ExitState = exitState ?: onExit.join()
    override fun stop(): koodies.exec.Process = also { javaProcess.destroy() }
    override fun kill(): koodies.exec.Process = also { javaProcess.destroyForcibly() }

    override fun toString(): String {
        val delegateString =
            if (started) "${javaProcess.toString().replaceFirst('[', '(').dropLast(1) + ")"}, successful=${successful.asEmoji}"
            else "not yet started"
        return "${this::class.simpleName ?: "object"}(delegate=$delegateString, started=${started.asEmoji})"
    }
}

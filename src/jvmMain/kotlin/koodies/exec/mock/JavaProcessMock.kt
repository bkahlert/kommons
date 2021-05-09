package koodies.exec.mock

import koodies.exec.mock.SlowInputStream.Companion.slowInputStream
import koodies.io.ByteArrayOutputStream
import koodies.io.TeeOutputStream
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.InMemoryLogger
import koodies.logging.MutedRenderingLogger
import koodies.text.LineSeparators.LF
import koodies.time.busyWait
import java.io.InputStream
import java.io.InputStream.nullInputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import java.lang.Process as JavaProcess

/**
 * [JavaProcess] mock to ease testing.
 */
public open class JavaProcessMock(
    public var logger: FixedWidthRenderingLogger,
    private var outputStream: OutputStream = ByteArrayOutputStream(),
    private val inputStream: InputStream = nullInputStream(),
    private val errorStream: InputStream = nullInputStream(),
    private val exitDelay: Duration = Duration.ZERO,
    private val exitCode: JavaProcessMock.() -> Int = { 0 },
) : JavaProcess() {

    private val pid: Long = 12345L
    override fun pid(): Long = pid

    private val completeOutputSequence = ByteArrayOutputStream()
    private val unprocessedOutputSequence = outputStream

    init {
        outputStream = object : TeeOutputStream(completeOutputSequence, unprocessedOutputSequence) {
            override fun toString(): String = completeOutputSequence.toString(Charsets.UTF_8)
        }
    }

    public companion object {

        public val RUNNING_PROCESS: JavaProcessMock
            get() = JavaProcessMock(MutedRenderingLogger(), exitCode = { 0 }, exitDelay = seconds(10))
        public val SUCCEEDING_PROCESS: JavaProcessMock
            get() = JavaProcessMock(MutedRenderingLogger(), inputStream = "line 1${LF}line 2$LF".byteInputStream(), exitCode = { 0 })
        public val FAILING_PROCESS: JavaProcessMock
            get() = JavaProcessMock(MutedRenderingLogger(), errorStream = "error 1${LF}error 2$LF".byteInputStream(), exitCode = { 42 })

        public fun InMemoryLogger.processMock(
            outputStream: OutputStream = ByteArrayOutputStream(),
            inputStream: InputStream = nullInputStream(),
            exitDelay: Duration = Duration.ZERO,
            exitCode: JavaProcessMock.() -> Int = { 0 },
        ): JavaProcessMock = JavaProcessMock(this, outputStream, inputStream, nullInputStream(), exitDelay, exitCode)

        public fun InMemoryLogger.withSlowInput(
            vararg inputs: String,
            baseDelayPerInput: Duration = seconds(1),
            echoInput: Boolean,
            exitDelay: Duration = Duration.ZERO,
            exitCode: JavaProcessMock.() -> Int = { 0 },
        ): JavaProcessMock {
            val outputStream = ByteArrayOutputStream()
            val slowInputStream = slowInputStream(
                baseDelayPerInput = baseDelayPerInput,
                byteArrayOutputStream = outputStream,
                echoInput = echoInput,
                inputs = inputs,
            )
            return processMock(outputStream, slowInputStream, exitDelay, exitCode)
        }

        public fun InMemoryLogger.withIndividuallySlowInput(
            vararg inputs: Pair<Duration, String>,
            echoInput: Boolean,
            baseDelayPerInput: Duration = seconds(1),
            exitDelay: Duration = Duration.ZERO,
            exitCode: JavaProcessMock.() -> Int = { 0 },
        ): JavaProcessMock {
            val outputStream = ByteArrayOutputStream()
            val slowInputStream = slowInputStream(
                baseDelayPerInput = baseDelayPerInput,
                byteArrayOutputStream = outputStream,
                echoInput = echoInput,
                inputs = inputs,
            )
            return processMock(
                outputStream = outputStream,
                inputStream = slowInputStream,
                exitDelay = exitDelay,
                exitCode = exitCode,
            )
        }
    }

    override fun getOutputStream(): OutputStream = outputStream
    override fun getInputStream(): InputStream = inputStream
    override fun getErrorStream(): InputStream = errorStream
    override fun waitFor(): Int {
        exitDelay.busyWait()
        return exitCode()
    }

    override fun waitFor(timeout: Long, unit: TimeUnit): Boolean {
        exitDelay.busyWait()
        return Duration.milliseconds(MILLISECONDS.convert(timeout, unit)) >= exitDelay
    }

    override fun exitValue(): Int {
        exitDelay.busyWait()
        return exitCode()
    }

    override fun onExit(): CompletableFuture<java.lang.Process> {
        return super.onExit().thenApply { process ->
            exitDelay.busyWait()
            process
        }
    }

    override fun isAlive(): Boolean = when (inputStream) {
        is SlowInputStream -> inputStream.unreadCount != 0
        else -> exitDelay > Duration.ZERO
    }

    public fun start(name: String? = null): ExecMock = ExecMock(this, name)

    override fun destroy(): Unit = Unit

    public val received: String get() = completeOutputSequence.toString(Charsets.UTF_8)

    override fun toString(): String =
        StringBuilder("Process[pid=").append(pid)
            .append(", exitValue=").append(if (!isAlive) exitCode() else "\"not exited\"")
            .append("]").toString()
}

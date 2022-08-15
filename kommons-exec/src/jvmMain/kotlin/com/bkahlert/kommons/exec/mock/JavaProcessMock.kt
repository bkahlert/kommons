package com.bkahlert.kommons.exec.mock

import com.bkahlert.kommons.Now
import com.bkahlert.kommons.exec.mock.SlowInputStream.Companion.slowInputStream
import com.bkahlert.kommons.io.ByteArrayOutputStream
import com.bkahlert.kommons.io.TeeOutputStream
import com.bkahlert.kommons.minus
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.time.busyWait
import java.io.InputStream
import java.io.InputStream.nullInputStream
import java.io.OutputStream
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import java.lang.Process as JavaProcess

/**
 * [JavaProcess] mock to ease testing.
 */
public open class JavaProcessMock(
    private var outputStream: OutputStream = ByteArrayOutputStream(),
    private val inputStream: InputStream = nullInputStream(),
    private val errorStream: InputStream = nullInputStream(),
    private val exitDelay: Duration = Duration.ZERO,
    private val exitCode: JavaProcessMock.() -> Int = { 0 },
) : JavaProcess() {

    private val startTimestamp: Instant = Now
    private fun busyWait() = 10.milliseconds.busyWait(11.milliseconds)

    override fun toHandle(): ProcessHandle = ProcessHandleMock(this)

    private val pid: Long = 12345L
    override fun pid(): Long = pid

    private val completeOutputSequence = ByteArrayOutputStream()
    private val unprocessedOutputSequence = outputStream

    init {
        outputStream = object : TeeOutputStream(completeOutputSequence, unprocessedOutputSequence) {
            override fun toString(): String = completeOutputSequence.toString(Charsets.UTF_8)
        }
    }

    override fun getOutputStream(): OutputStream = outputStream
    override fun getInputStream(): InputStream = inputStream
    override fun getErrorStream(): InputStream = errorStream
    override fun waitFor(): Int {
        while (isAlive) {
            busyWait()
        }
        return exitCode()
    }

    override fun waitFor(timeout: Long, unit: TimeUnit): Boolean {
        val start = Now
        val waitAtMost = unit.toMillis(timeout).milliseconds
        while (isAlive && Now.minus(start) < waitAtMost) {
            busyWait()
        }
        return !isAlive
    }

    override fun exitValue(): Int {
        if (isAlive) throw IllegalStateException("process has not terminated")
        return exitCode()
    }

    override fun isAlive(): Boolean = when (inputStream) {
        is SlowInputStream -> inputStream.unreadCount != 0
        else -> Now.minus(startTimestamp) < exitDelay
    }

    public fun start(name: String? = null): ExecMock = ExecMock(this, name)

    override fun destroy(): Unit = Unit

    public val received: String get() = completeOutputSequence.toString(Charsets.UTF_8)

    override fun toString(): String =
        StringBuilder("Process[pid=").append(pid)
            .append(", exitValue=").append(if (!isAlive) exitCode() else "\"not exited\"")
            .append("]").toString()


    public companion object {

        public val RUNNING_PROCESS: JavaProcessMock
            get() = JavaProcessMock(exitCode = { 0 }, exitDelay = 10.seconds)
        public val SUCCEEDING_PROCESS: JavaProcessMock
            get() = JavaProcessMock(inputStream = "line 1${LF}line 2$LF".byteInputStream(), exitCode = { 0 })
        public val FAILING_PROCESS: JavaProcessMock
            get() = JavaProcessMock(errorStream = "error 1${LF}error 2$LF".byteInputStream(), exitCode = { 42 })

        public fun processMock(
            outputStream: OutputStream = ByteArrayOutputStream(),
            inputStream: InputStream = nullInputStream(),
            exitDelay: Duration = Duration.ZERO,
            exitCode: JavaProcessMock.() -> Int = { 0 },
        ): JavaProcessMock = JavaProcessMock(outputStream, inputStream, nullInputStream(), exitDelay, exitCode)

        public fun withSlowInput(
            vararg inputs: String,
            baseDelayPerInput: Duration = 1.seconds,
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

        public fun withIndividuallySlowInput(
            vararg inputs: Pair<Duration, String>,
            echoInput: Boolean,
            baseDelayPerInput: Duration = 1.seconds,
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
}
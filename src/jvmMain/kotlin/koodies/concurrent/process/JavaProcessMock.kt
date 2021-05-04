package koodies.concurrent.process

import koodies.concurrent.process.SlowInputStream.Companion.slowInputStream
import koodies.debug.asEmoji
import koodies.debug.debug
import koodies.exec.CommandLine
import koodies.exec.Exec
import koodies.exec.MetaStream
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.Failure
import koodies.exec.Process.ExitState.Success
import koodies.exec.Process.ProcessState
import koodies.exec.Process.ProcessState.Running
import koodies.io.ByteArrayOutputStream
import koodies.io.RedirectingOutputStream
import koodies.io.TeeOutputStream
import koodies.io.path.Locations
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.InMemoryLogger
import koodies.logging.MutedRenderingLogger
import koodies.logging.RenderingLogger
import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.Semantics.Symbols
import koodies.text.takeUnlessEmpty
import koodies.time.Now
import koodies.time.busyWait
import koodies.time.sleep
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.seconds
import java.lang.Process as JavaProcess

public open class JavaProcessMock(
    public var logger: RenderingLogger,
    private var outputStream: OutputStream = ByteArrayOutputStream(),
    private val inputStream: InputStream = InputStream.nullInputStream(),
    private val exitDelay: Duration = Duration.ZERO,
    private val exitCode: JavaProcessMock.() -> Int = { 0 },
) : JavaProcess() {

    private val pid: Long = Random.nextLong()
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
            get() = InMemoryLogger().withUnclosedWarningDisabled.withSlowInput("this",
                "process",
                "processed",
                "slowly",
                echoInput = true)
        public val SUCCEEDED_PROCESS: JavaProcessMock get() = JavaProcessMock(MutedRenderingLogger(), exitCode = { 0 })
        public val FAILED_PROCESS: JavaProcessMock get() = JavaProcessMock(MutedRenderingLogger(), exitCode = { 1 })

        public fun InMemoryLogger.processMock(
            outputStream: OutputStream = ByteArrayOutputStream(),
            inputStream: InputStream = InputStream.nullInputStream(),
            exitDelay: Duration = Duration.ZERO,
            exitCode: JavaProcessMock.() -> Int = { 0 },
        ): JavaProcessMock = JavaProcessMock(this, outputStream, inputStream, exitDelay, exitCode)

        public fun InMemoryLogger.withSlowInput(
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

        public fun InMemoryLogger.withIndividuallySlowInput(
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

    override fun getOutputStream(): OutputStream = outputStream
    override fun getInputStream(): InputStream = inputStream
    override fun getErrorStream(): InputStream = InputStream.nullInputStream()
    override fun waitFor(): Int {
        exitDelay.busyWait()
        return exitCode()
    }

    override fun waitFor(timeout: Long, unit: TimeUnit): Boolean {
        exitDelay.busyWait()
        return TimeUnit.MILLISECONDS.convert(timeout, unit).milliseconds >= exitDelay
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
}

@Deprecated("use Exec")
public open class ExecMock(public val processMock: JavaProcessMock, public val name: String? = null) : Exec {

    private var javaProcess: java.lang.Process = processMock
    override val pid: Long get() = javaProcess.pid()

    override fun waitFor(): ExitState = exitState ?: onExit.join()
    override fun stop(): Exec = also { javaProcess.destroy() }
    override fun kill(): Exec = also { javaProcess.destroyForcibly() }

    public var logger: RenderingLogger = processMock.logger

    override val workingDirectory: Path? = Locations.Temp
    override val commandLine: CommandLine = CommandLine("echo", "just a mock")

    override var state: ProcessState = Running(12345L)
        protected set

    override var exitState: ExitState? = null
        protected set

    private val ioLog: IOLog by lazy { IOLog() }
    override val io: IOSequence<IO> get() = IOSequence(ioLog)
    override val metaStream: MetaStream = MetaStream()
    override val inputStream: OutputStream by lazy {
        TeeOutputStream(
            RedirectingOutputStream {
                // ugly hack; IN logs are just there and the processor is just notified;
                // whereas OUT and ERR have to be processed first, are delayed and don't show in right order
                // therefore we delay here
                1.milliseconds.sleep { io.input + it }
            },
            javaProcess.outputStream,
        )
    }
    override val outputStream: InputStream by lazy { javaProcess.inputStream }
    override val errorStream: InputStream by lazy { javaProcess.errorStream }

    private val preTerminationCallbacks = mutableListOf<Exec.() -> Unit>()
    override fun addPreTerminationCallback(callback: Exec.() -> Unit): Exec = also { preTerminationCallbacks.add(callback) }

    private val postTerminationCallbacks = mutableListOf<Exec.(ExitState) -> Unit>()
    override fun addPostTerminationCallback(callback: Exec.(ExitState) -> Unit): Exec = also { postTerminationCallbacks.add(callback) }

    private val cachedOnExit: CompletableFuture<out ExitState> by lazy<CompletableFuture<out ExitState>> {
        preTerminationCallbacks.runCatching { this }.exceptionOrNull()
        processMock.onExit().thenApply {
            Success(12345L, io).also { state = it }
                .also { term -> postTerminationCallbacks.forEach { it(term) } }
        }
    }

    override val successful: Boolean? get() = kotlin.runCatching { javaProcess.exitValue() }.fold({ true }, { null })
    override val onExit: CompletableFuture<out ExitState> get() = cachedOnExit

    override fun toString(): String {
        val delegateString = "${javaProcess.toString().replaceFirst('[', '(').dropLast(1) + ")"}, successful=${successful?.asEmoji ?: Symbols.Computation}"
        val string = "${this::class.simpleName ?: "object"}(delegate=$delegateString)".substringBeforeLast(")")
        return string.takeUnless { name != null } ?: string.substringBeforeLast(")") + ", name=$name)"
    }

    public companion object {
        public val RUNNING_MANAGED_PROCESS: ExecMock
            get() = object : ExecMock(JavaProcessMock.RUNNING_PROCESS) {
                override var state: ProcessState = Running(12345L)
                override val onExit: CompletableFuture<out ExitState> get() = completedFuture(Any() as ExitState)
                override val successful: Boolean? = null
            }
        public val SUCCEEDED_MANAGED_PROCESS: ExecMock
            get() = object : ExecMock(JavaProcessMock.SUCCEEDED_PROCESS) {
                override var state: ProcessState = Success(12345L, IOSequence(sequenceOf(IO.Output typed "line 1", IO.Output typed "line 2")))
                override val onExit: CompletableFuture<out ExitState> = completedFuture(state as ExitState)
                override val successful: Boolean = true
            }
        public val FAILED_MANAGED_PROCESS: ExecMock
            get() = object : ExecMock(JavaProcessMock.FAILED_PROCESS) {
                override var state: ProcessState =
                    Failure(42, 12345L, emptyList(), null, IOSequence(sequenceOf(IO.Error typed "error 1", IO.Error typed "error 2")))
                override val onExit: CompletableFuture<out ExitState> = completedFuture(state as ExitState)
                override val successful: Boolean? = false
            }
    }
}


public class SlowInputStream(
    public val baseDelayPerInput: Duration,
    public val byteArrayOutputStream: ByteArrayOutputStream? = null,
    public val echoInput: Boolean = false,
    public var logger: FixedWidthRenderingLogger,
    vararg inputs: Pair<Duration, String>,
) : InputStream() {
    public constructor(
        inputs: List<String>,
        baseDelayPerInput: Duration,
        byteArrayOutputStream: ByteArrayOutputStream? = null,
        echoInput: Boolean = false,
        logger: FixedWidthRenderingLogger,
    ) : this(
        baseDelayPerInput = baseDelayPerInput,
        byteArrayOutputStream = byteArrayOutputStream,
        echoInput = echoInput,
        logger = logger,
        inputs = inputs.map { Duration.ZERO to it }.toTypedArray(),
    )

    public companion object {
        public fun prompt(): Pair<Duration, String> = Duration.INFINITE to ""
        public fun InMemoryLogger.slowInputStream(
            baseDelayPerInput: Duration,
            vararg inputs: Pair<Duration, String>,
            byteArrayOutputStream: ByteArrayOutputStream? = null,
            echoInput: Boolean = false,
        ): SlowInputStream = SlowInputStream(baseDelayPerInput, byteArrayOutputStream, echoInput, this, inputs = inputs)

        public fun InMemoryLogger.slowInputStream(
            baseDelayPerInput: Duration,
            vararg inputs: String,
            byteArrayOutputStream: ByteArrayOutputStream? = null,
            echoInput: Boolean = false,
        ): SlowInputStream =
            SlowInputStream(baseDelayPerInput, byteArrayOutputStream, echoInput, this, inputs = inputs.map { Duration.ZERO to it }.toTypedArray())
    }

    public val terminated: Boolean get() = unreadCount == 0 || !processAlive
    private var closed = false
    private var processAlive: Boolean = true
    private var blockUntil: Long = System.currentTimeMillis()
    private val unread: MutableList<Pair<Duration, MutableList<Byte>>> =
        inputs.map { it.first to it.second.toByteArray().toMutableList() }.toMutableList()
    public val unreadCount: Int get() = unread.map { it.second.size }.sum()
    private val originalCountLength = "$unreadCount".length
    private val blockedByPrompt get() = unread.isNotEmpty() && unread.first().first == Duration.INFINITE
    private val Int.padded get() = this.toString().padStart(originalCountLength)

    private val inputs = mutableListOf<String>()
    public fun processInput(logger: FixedWidthRenderingLogger): Boolean = logger.logging("✏️") {
        byteArrayOutputStream?.apply {
            toString(Charsets.UTF_8).takeUnlessEmpty()?.let { newInput ->
                inputs.add(newInput)
                logLine { "new input added; buffer is $inputs" }
                reset()
            }
        }
        if (inputs.isNotEmpty()) {
            if (blockedByPrompt) {
                val input = inputs.first()
                logLine { input.debug }
                if (echoInput) unread[0] = Duration.ZERO to input.map { it.toByte() }.toMutableList()
                else unread.removeFirst()
                logLine { "unblocked prompt" }
            }
        } else {
            if (blockedByPrompt) {
                logLine { "blocked by prompt" }
            } else {
                logLine { "no input and no prompt" }
            }
        }
        blockedByPrompt
    }

    private fun handleAndReturnBlockingState(): Boolean = processInput(logger)

    override fun available(): Int = logger.logging("available") {
        if (closed) {
            throw IOException("Closed.")
        }

        if (handleAndReturnBlockingState()) {
            logLine { "prompt is blocking" }
            return@logging 0
        }
        val yetBlocked = blockUntil - System.currentTimeMillis()
        if (yetBlocked > 0) {
            logLine { "${yetBlocked.milliseconds} to wait for next chunk" }
            return@logging 0
        }
        if (terminated) {
            logLine { "Backing buffer is depleted ➜ EOF reached." }
            return@logging 0
        }

        val currentDelayedWord = unread.first()
        if (currentDelayedWord.first > Duration.ZERO) {
            val delay = currentDelayedWord.first
            blockUntil = System.currentTimeMillis() + delay.toLongMilliseconds()
            unread[0] = Duration.ZERO to currentDelayedWord.second
            logLine { "$delay to wait for next chunk (just started)" }
            return@logging 0
        }

        currentDelayedWord.second.size
    }

    override fun read(): Int = logger.logging("read") {
        if (closed) {
            throw IOException("Closed.")
        }

        while (handleAndReturnBlockingState()) {
            logLine { "prompt is blocking" }
            10.milliseconds.sleep()
        }

        logLine { "${unreadCount.padded.ansi.yellow} bytes unread" }

        if (terminated) {
            logLine { "Backing buffer is depleted ➜ EOF reached." }
            return@logging -1
        }

        val yetBlocked = blockUntil - System.currentTimeMillis()
        if (yetBlocked > 0) {
            logging(Now.emoji) {
                logLine { "blocking for the remaining ${yetBlocked.milliseconds}..." }
                Thread.sleep(yetBlocked)
            }
        }

        val currentWord: MutableList<Byte> = unread.let {
            val currentLine: Pair<Duration, MutableList<Byte>> = it.first()
            val delay = currentLine.first
            if (delay > Duration.ZERO) {
                logging(Now.emoji) {
                    logLine { "output delayed by $delay..." }
                    Thread.sleep(delay.toLongMilliseconds())
                    unread[0] = Duration.ZERO to currentLine.second
                }
            }
            currentLine.second
        }
        logLine { "— available ${currentWord.debug.ansi.magenta}" }
        val currentByte = currentWord.removeFirst()
        logLine { "— current: $currentByte/${currentByte.toChar()}" }

        if (currentWord.isEmpty()) {
            unread.removeFirst()
            blockUntil = System.currentTimeMillis() + baseDelayPerInput.toLongMilliseconds()
            logLine { "— empty; waiting time for next chunk is $baseDelayPerInput" }
        }
        currentByte.toInt()
    }

    /**
     * Tries to behave exactly like [BufferedInputStream.read].
     */
    private fun read1(b: ByteArray, off: Int, len: Int): Int {
        val avail = available()
        val cnt = if (avail < len) avail else len
        (0 until cnt).map { i ->
            b[off + i] = read().toByte()
        }
        return cnt
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (terminated) {
            return -1
        }

        if (off or len or off + len or b.size - (off + len) < 0) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return 0
        }

        var n = 0
        while (true) {
            val readCount = read1(b, off + n, len - n)
            if (readCount <= 0) return if (n == 0) readCount else n
            n += readCount
            if (n >= len) return n
            // if not closed but no bytes available, return
            if (!closed && available() <= 0) return n
        }
    }

    public fun input(text: String): Unit = logger.logging("input") {
        if (handleAndReturnBlockingState()) {
            logLine { "Input received: $text" }
            unread.removeFirst()
        }
    }

    override fun close() {
        closed = true
    }

    override fun toString(): String = "$unreadCount bytes left"
}

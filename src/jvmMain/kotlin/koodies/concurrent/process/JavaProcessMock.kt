package koodies.concurrent.process

import koodies.concurrent.process.IO.ERR
import koodies.concurrent.process.SlowInputStream.Companion.slowInputStream
import koodies.debug.asEmoji
import koodies.debug.debug
import koodies.exec.Exec
import koodies.exec.MetaStream
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.Failure
import koodies.exec.Process.ExitState.Success
import koodies.exec.Process.ProcessState
import koodies.exec.Process.ProcessState.Prepared
import koodies.exec.Process.ProcessState.Running
import koodies.io.ByteArrayOutputStream
import koodies.io.RedirectingOutputStream
import koodies.io.TeeOutputStream
import koodies.io.path.Locations
import koodies.logging.InMemoryLogger
import koodies.logging.MutedRenderingLogger
import koodies.logging.RenderingLogger
import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.takeUnlessEmpty
import koodies.time.Now
import koodies.time.sleep
import koodies.tracing.MiniTracer
import koodies.tracing.miniTrace
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.seconds
import java.lang.Process as JavaProcess

public open class JavaProcessMock(
    public var logger: RenderingLogger,
    private var outputStream: OutputStream = ByteArrayOutputStream(),
    private val inputStream: InputStream = InputStream.nullInputStream(),
    private val processExit: JavaProcessMock.() -> ProcessExitMock,
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
                echoInput = true) { ProcessExitMock.immediateSuccess() }
        public val SUCCEEDED_PROCESS: JavaProcessMock get() = JavaProcessMock(MutedRenderingLogger()) { ProcessExitMock.immediateSuccess() }
        public val FAILED_PROCESS: JavaProcessMock get() = JavaProcessMock(MutedRenderingLogger()) { ProcessExitMock.immediateExit(-1) }

        public fun InMemoryLogger.processMock(
            outputStream: OutputStream = ByteArrayOutputStream(),
            inputStream: InputStream = InputStream.nullInputStream(),
            processExit: JavaProcessMock.() -> ProcessExitMock,
        ): JavaProcessMock = JavaProcessMock(this, outputStream, inputStream, processExit)

        public fun InMemoryLogger.withSlowInput(
            vararg inputs: String,
            baseDelayPerInput: Duration = 1.seconds,
            echoInput: Boolean,
            processExit: JavaProcessMock.() -> ProcessExitMock,
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
                processExit = processExit,
            )
        }

        public fun InMemoryLogger.withIndividuallySlowInput(
            vararg inputs: Pair<Duration, String>,
            echoInput: Boolean,
            baseDelayPerInput: Duration = 1.seconds,
            processExit: JavaProcessMock.() -> ProcessExitMock,
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
                processExit = processExit,
            )
        }
    }

    override fun getOutputStream(): OutputStream = outputStream
    override fun getInputStream(): InputStream = inputStream
    override fun getErrorStream(): InputStream = InputStream.nullInputStream()
    override fun waitFor(): Int = logger.miniTrace(::waitFor) {
        this@JavaProcessMock.processExit()()
    }

    override fun exitValue(): Int = logger.miniTrace(::exitValue) {
        this@JavaProcessMock.processExit()()
    }

    override fun onExit(): CompletableFuture<java.lang.Process> {
        return super.onExit().thenApply { process ->
            processExit()()
            process
        }
    }

    override fun isAlive(): Boolean {
        return logger.miniTrace(::isAlive) {
            when (inputStream) {
                is SlowInputStream -> inputStream.unreadCount != 0
                else -> processExit().delay > Duration.ZERO
            }
        }
    }

    public fun start(name: String? = null): ExecMock = ExecMock(this, name)

    override fun destroy(): Unit = logger.miniTrace(::destroy) { }

    public val received: String get() = completeOutputSequence.toString(Charsets.UTF_8)
}

public open class ExecMock(public val processMock: JavaProcessMock, public val name: String? = null) : Exec {

    override val pid: Long by lazy { startImplicitly().pid() }
    private var javaProcess: java.lang.Process? = null
    private val startLock = ReentrantLock()
    override fun start(): Exec {
        if (javaProcess != null) return this
        startLock.withLock {
            if (javaProcess != null) return this
            javaProcess = processMock
            state = Running(12345L)
        }
        return this
    }

    private fun startImplicitly(): java.lang.Process = run { start(); javaProcess!! }

    override val exitValue: Int get() = javaProcess?.exitValue() ?: throw IllegalStateException("Process not running.")
    override fun waitFor(): ExitState = exitState ?: onExit.join()
    override fun stop(): Exec = also { javaProcess?.destroy() }
    override fun kill(): Exec = also { javaProcess?.destroyForcibly() }

    public var logger: RenderingLogger = processMock.logger

    override val workingDirectory: Path = Locations.Temp

    override var state: ProcessState = Prepared()
        protected set

    override var exitState: ExitState? = null
        protected set

    override val io: IOLog by lazy { IOLog() }
    override val metaStream: MetaStream = MetaStream()
    override val inputStream: OutputStream by lazy {
        TeeOutputStream(
            RedirectingOutputStream {
                // ugly hack; IN logs are just there and the processor is just notified;
                // whereas OUT and ERR have to be processed first, are delayed and don't show in right order
                // therefore we delay here
                1.milliseconds.sleep { io.input + it }
            },
            startImplicitly().outputStream,
        )
    }
    override val outputStream: InputStream by lazy { startImplicitly().inputStream }
    override val errorStream: InputStream by lazy { startImplicitly().errorStream }

    private val preTerminationCallbacks = mutableListOf<Exec.() -> Unit>()
    override fun addPreTerminationCallback(callback: Exec.() -> Unit): Exec = also { preTerminationCallbacks.add(callback) }

    private val postTerminationCallbacks = mutableListOf<Exec.(ExitState) -> Unit>()
    override fun addPostTerminationCallback(callback: Exec.(ExitState) -> Unit): Exec = also { postTerminationCallbacks.add(callback) }

    private val cachedOnExit: CompletableFuture<out ExitState> by lazy<CompletableFuture<out ExitState>> {
        val p = start()
        preTerminationCallbacks.runCatching { p }.exceptionOrNull()
        processMock.onExit().thenApply {
            Success(12345L, io.toList()).also { state = it }
                .also { term -> postTerminationCallbacks.forEach { p.it(term) } }
        }
    }

    override val successful: Boolean? get() = kotlin.runCatching { startImplicitly().exitValue() }.fold({ true }, { null })
    override val onExit: CompletableFuture<out ExitState> get() = cachedOnExit

    override fun toString(): String {
        val delegateString =
            if (javaProcess != null) "${javaProcess.toString().replaceFirst('[', '(').dropLast(1) + ")"}, successful=${successful.asEmoji}"
            else "not yet started"
        val string = "${this::class.simpleName ?: "object"}(delegate=$delegateString, started=${(javaProcess != null).asEmoji})"
        return string.takeUnless { name != null } ?: string.substringBeforeLast(")") + ", name=$name)"
    }

    public companion object {
        public val PREPARED_MANAGED_PROCESS: ExecMock
            get() = object : ExecMock(JavaProcessMock.SUCCEEDED_PROCESS) {
                override var state: ProcessState = Prepared()
                override val onExit: CompletableFuture<out ExitState> get() = completedFuture(Any() as ExitState)
                override val successful: Boolean? = null
            }
        public val RUNNING_MANAGED_PROCESS: ExecMock
            get() = object : ExecMock(JavaProcessMock.RUNNING_PROCESS) {
                override var state: ProcessState = Running(12345L)
                override val onExit: CompletableFuture<out ExitState> get() = completedFuture(Any() as ExitState)
                override val successful: Boolean? = null
            }
        public val SUCCEEDED_MANAGED_PROCESS: ExecMock
            get() = object : ExecMock(JavaProcessMock.SUCCEEDED_PROCESS) {
                override var state: ProcessState = Success(12345L, listOf(IO.OUT typed "line 1", IO.OUT typed "line 2"))
                override val onExit: CompletableFuture<out ExitState> = completedFuture(state as ExitState)
                override val successful: Boolean = true
            }
        public val FAILED_MANAGED_PROCESS: ExecMock
            get() = object : ExecMock(JavaProcessMock.FAILED_PROCESS) {
                override var state: ProcessState =
                    Failure(42, 12345L, emptyList(), null, listOf(ERR typed "error 1", ERR typed "error 2"))
                override val onExit: CompletableFuture<out ExitState> = completedFuture(state as ExitState)
                override val successful: Boolean? = false
            }
    }
}


public class SlowInputStream(
    public val baseDelayPerInput: Duration,
    public val byteArrayOutputStream: ByteArrayOutputStream? = null,
    public val echoInput: Boolean = false,
    public var logger: RenderingLogger,
    vararg inputs: Pair<Duration, String>,
) : InputStream() {
    public constructor(
        inputs: List<String>,
        baseDelayPerInput: Duration,
        byteArrayOutputStream: ByteArrayOutputStream? = null,
        echoInput: Boolean = false,
        logger: RenderingLogger,
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
    public fun processInput(logger: MiniTracer): Boolean = logger.microTrace("✏️") {
        byteArrayOutputStream?.apply {
            toString(Charsets.UTF_8).takeUnlessEmpty()?.let { newInput ->
                inputs.add(newInput)
                trace("new input added; buffer is $inputs")
                reset()
            }
        }
        if (inputs.isNotEmpty()) {
            if (blockedByPrompt) {
                val input = inputs.first()
                trace(input.debug)
                if (echoInput) unread[0] = Duration.ZERO to input.map { it.toByte() }.toMutableList()
                else unread.removeFirst()
                trace("unblocked prompt")
            }
        } else {
            if (blockedByPrompt) {
                trace("blocked by prompt")
            } else {
                trace("no input and no prompt")
            }
        }
        blockedByPrompt
    }

    private fun handleAndReturnBlockingState(): Boolean = logger.miniTrace(::handleAndReturnBlockingState) { processInput(this) }

    override fun available(): Int = logger.miniTrace(::available) {
        if (closed) {
            throw IOException("Closed.")
        }

        if (handleAndReturnBlockingState()) {
            trace("prompt is blocking")
            return@miniTrace 0
        }
        val yetBlocked = blockUntil - System.currentTimeMillis()
        if (yetBlocked > 0) {
            trace("${yetBlocked.milliseconds} to wait for next chunk")
            return@miniTrace 0
        }
        if (terminated) {
            trace("Backing buffer is depleted ➜ EOF reached.")
            return@miniTrace 0
        }

        val currentDelayedWord = unread.first()
        if (currentDelayedWord.first > Duration.ZERO) {
            val delay = currentDelayedWord.first
            blockUntil = System.currentTimeMillis() + delay.toLongMilliseconds()
            unread[0] = Duration.ZERO to currentDelayedWord.second
            trace("$delay to wait for next chunk (just started)")
            return@miniTrace 0
        }

        currentDelayedWord.second.size
    }

    override fun read(): Int = logger.miniTrace(::read) {
        if (closed) {
            throw IOException("Closed.")
        }

        while (handleAndReturnBlockingState()) {
            trace("prompt is blocking")
            10.milliseconds.sleep()
        }

        trace("${unreadCount.padded.ansi.yellow} bytes unread")

        if (terminated) {
            trace("Backing buffer is depleted ➜ EOF reached.")
            return@miniTrace -1
        }

        val yetBlocked = blockUntil - System.currentTimeMillis()
        if (yetBlocked > 0) {
            microTrace<Unit>(Now.emoji) {
                trace("blocking for the remaining ${yetBlocked.milliseconds}...")
                Thread.sleep(yetBlocked)
            }
        }

        val currentWord: MutableList<Byte> = unread.let {
            val currentLine: Pair<Duration, MutableList<Byte>> = it.first()
            val delay = currentLine.first
            if (delay > Duration.ZERO) {
                this.microTrace<Unit>(Now.emoji) {
                    trace("output delayed by $delay...")
                    Thread.sleep(delay.toLongMilliseconds())
                    unread[0] = Duration.ZERO to currentLine.second
                }
            }
            currentLine.second
        }
        trace("— available ${currentWord.debug.ansi.magenta}")
        val currentByte = currentWord.removeFirst()
        trace("— current: $currentByte/${currentByte.toChar()}")

        if (currentWord.isEmpty()) {
            unread.removeFirst()
            blockUntil = System.currentTimeMillis() + baseDelayPerInput.toLongMilliseconds()
            trace("— empty; waiting time for next chunk is $baseDelayPerInput")
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

    public fun input(text: String): Unit = logger.miniTrace(::input) {
        if (handleAndReturnBlockingState()) {
            trace("Input received: $text")
            unread.removeFirst()
        }
    }

    override fun close() {
        closed = true
    }

    override fun toString(): String = "$unreadCount bytes left"
}

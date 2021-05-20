package koodies.exec.mock

import koodies.debug.debug
import koodies.io.ByteArrayOutputStream
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.InMemoryLogger
import koodies.text.Semantics.formattedAs
import koodies.text.takeUnlessEmpty
import koodies.time.seconds
import koodies.time.sleep
import koodies.unit.bytes
import koodies.unit.milli
import java.io.IOException
import java.io.InputStream
import kotlin.time.Duration

public class SlowInputStream(
    public val baseDelayPerInput: Duration,
    public val byteArrayOutputStream: ByteArrayOutputStream? = null,
    public val echoInput: Boolean = false,
    public var logger: FixedWidthRenderingLogger,
    vararg inputs: Pair<Duration, String>,
) : InputStream() {

    public companion object {
        private val fiftyMillis = 50.milli.seconds

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
    private val blockedByPrompt get() = unread.isNotEmpty() && unread.first().first == Duration.INFINITE

    private val inputs = mutableListOf<String>()
    public fun processInput(logger: FixedWidthRenderingLogger): Boolean = logger.compactLogging("✏️ processing input") {
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
                if (echoInput) unread[0] = Duration.ZERO to input.map { it.code.toByte() }.toMutableList()
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

    override fun available(): Int = logger.compactLogging("available") {
        if (closed) {
            throw IOException("Closed.")
        }

        if (handleAndReturnBlockingState()) {
            logLine { "prompt is blocking" }
            fiftyMillis.sleep()
            return@compactLogging 0
        }
        val yetBlocked = blockUntil - System.currentTimeMillis()
        if (yetBlocked > 0) {
            val delay = yetBlocked.milli.seconds
            logLine { "$delay to wait for next chunk" }
            ((delay / 2).takeIf { it > fiftyMillis } ?: fiftyMillis).sleep()
            return@compactLogging 0
        }
        if (terminated) {
            logLine { "Backing buffer is depleted ➜ EOF reached." }
            return@compactLogging 0
        }

        val currentDelayedWord = unread.first()
        if (currentDelayedWord.first > Duration.ZERO) {
            val delay = currentDelayedWord.first
            blockUntil = System.currentTimeMillis() + delay.inWholeMilliseconds
            unread[0] = Duration.ZERO to currentDelayedWord.second
            logLine { "$delay to wait for next chunk (just started)" }
            ((delay / 2).takeIf { it > fiftyMillis } ?: fiftyMillis).sleep()
            return@compactLogging 0
        }

        currentDelayedWord.second.size
    }

    override fun read(): Int = logger.compactLogging("read") {
        if (closed) {
            throw IOException("Closed.")
        }

        while (handleAndReturnBlockingState()) {
            logLine { "prompt is blocking" }
            fiftyMillis.sleep()
        }

        logLine { "${unreadCount.bytes.formattedAs.debug} unread" }

        if (terminated) {
            logLine { "Backing buffer is depleted ➜ EOF reached." }
            return@compactLogging -1
        }

        val yetBlocked = blockUntil - System.currentTimeMillis()
        if (yetBlocked > 0) {
            logLine { "blocking for the remaining ${yetBlocked.milli.seconds}…" }
            yetBlocked.milli.seconds.sleep()
        }

        val currentWord: MutableList<Byte> = unread.let {
            val currentLine: Pair<Duration, MutableList<Byte>> = it.first()
            val delay = currentLine.first
            if (delay > Duration.ZERO) {
                logLine { "output delayed by $delay…" }
                delay.sleep()
                unread[0] = Duration.ZERO to currentLine.second
            }
            currentLine.second
        }
        logLine { "available:${currentWord.debug.formattedAs.debug}" }
        val currentByte = currentWord.removeFirst()
        logLine { "current:${currentByte.debug.formattedAs.debug}" }

        if (currentWord.isEmpty()) {
            unread.removeFirst()
            val delay = baseDelayPerInput
            blockUntil = System.currentTimeMillis() + delay.inWholeMilliseconds
            logLine { "— empty; waiting time for next chunk is $baseDelayPerInput" }
            ((delay / 2).takeIf { it > fiftyMillis } ?: fiftyMillis).sleep()
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

    override fun toString(): String = "${unreadCount.bytes} left"
}

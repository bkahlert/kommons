package com.bkahlert.kommons.exec.mock

import io.opentelemetry.api.trace.Span
import com.bkahlert.kommons.debug.debug
import com.bkahlert.kommons.io.ByteArrayOutputStream
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.takeUnlessEmpty
import com.bkahlert.kommons.time.seconds
import com.bkahlert.kommons.time.sleep
import com.bkahlert.kommons.tracing.SpanScope
import com.bkahlert.kommons.tracing.rendering.spanningLine
import com.bkahlert.kommons.tracing.runSpanning
import com.bkahlert.kommons.tracing.spanId
import com.bkahlert.kommons.unit.bytes
import com.bkahlert.kommons.unit.milli
import java.io.IOException
import java.io.InputStream
import kotlin.time.Duration

public class SlowInputStream(
    public val baseDelayPerInput: Duration,
    public val byteArrayOutputStream: ByteArrayOutputStream? = null,
    public val echoInput: Boolean = false,
    vararg inputs: Pair<Duration, String>,
) : InputStream() {

    public companion object {
        private val fiftyMillis = 50.milli.seconds

        public fun prompt(): Pair<Duration, String> = Duration.INFINITE to ""
        public fun slowInputStream(
            baseDelayPerInput: Duration,
            vararg inputs: Pair<Duration, String>,
            byteArrayOutputStream: ByteArrayOutputStream? = null,
            echoInput: Boolean = false,
        ): SlowInputStream = SlowInputStream(baseDelayPerInput, byteArrayOutputStream, echoInput, inputs = inputs)

        public fun slowInputStream(
            baseDelayPerInput: Duration,
            vararg inputs: String,
            byteArrayOutputStream: ByteArrayOutputStream? = null,
            echoInput: Boolean = false,
        ): SlowInputStream =
            SlowInputStream(baseDelayPerInput, byteArrayOutputStream, echoInput, inputs = inputs.map { Duration.ZERO to it }.toTypedArray())
    }

    /**
     * [SpanScope] to use to create nested spans.
     */
    public var parentSpan: Span = Span.current()

    public val terminated: Boolean get() = unreadCount == 0 || !processAlive
    private var closed = false
    private var processAlive: Boolean = true
    private var blockUntil: Long = System.currentTimeMillis()
    private val unread: MutableList<Pair<Duration, MutableList<Byte>>> =
        inputs.map { it.first to it.second.toByteArray().toMutableList() }.toMutableList()
    public val unreadCount: Int get() = unread.map { it.second.size }.sum()
    private val blockedByPrompt get() = unread.isNotEmpty() && unread.first().first == Duration.INFINITE

    private val inputs = mutableListOf<String>()
    public fun processInput(): Boolean = parentSpan.spanningWithDisabledPrinterOnIllegalSpan("✏️ processing input") {
        byteArrayOutputStream?.apply {
            toString(Charsets.UTF_8).takeUnlessEmpty()?.let { newInput ->
                inputs.add(newInput)
                log("new input added; buffer is $inputs")
                reset()
            }
        }
        if (inputs.isNotEmpty()) {
            if (blockedByPrompt) {
                val input = inputs.first()
                log(input.debug)
                if (echoInput) unread[0] = Duration.ZERO to input.map { it.code.toByte() }.toMutableList()
                else unread.removeFirst()
                log("unblocked prompt")
            }
        } else {
            if (blockedByPrompt) {
                log("blocked by prompt")
            } else {
                log("no input and no prompt")
            }
        }
        blockedByPrompt
    }

    private fun handleAndReturnBlockingState(): Boolean = processInput()

    override fun available(): Int = parentSpan.spanningWithDisabledPrinterOnIllegalSpan("available") {
        if (closed) {
            throw IOException("Closed.")
        }

        if (handleAndReturnBlockingState()) {
            log("prompt is blocking")
            fiftyMillis.sleep()
            return@spanningWithDisabledPrinterOnIllegalSpan 0
        }
        val yetBlocked = blockUntil - System.currentTimeMillis()
        if (yetBlocked > 0) {
            val delay = yetBlocked.milli.seconds
            log("$delay to wait for next chunk")
            ((delay / 2).takeIf { it > fiftyMillis } ?: fiftyMillis).sleep()
            return@spanningWithDisabledPrinterOnIllegalSpan 0
        }
        if (terminated) {
            log("Backing buffer is depleted ➜ EOF reached.")
            return@spanningWithDisabledPrinterOnIllegalSpan 0
        }

        val currentDelayedWord = unread.first()
        if (currentDelayedWord.first > Duration.ZERO) {
            val delay = currentDelayedWord.first
            blockUntil = System.currentTimeMillis() + delay.inWholeMilliseconds
            unread[0] = Duration.ZERO to currentDelayedWord.second
            log("$delay to wait for next chunk (just started)")
            ((delay / 2).takeIf { it > fiftyMillis } ?: fiftyMillis).sleep()
            return@spanningWithDisabledPrinterOnIllegalSpan 0
        }

        currentDelayedWord.second.size
    }

    override fun read(): Int = parentSpan.spanningWithDisabledPrinterOnIllegalSpan("read") {
        if (closed) {
            throw IOException("Closed.")
        }

        while (handleAndReturnBlockingState()) {
            log("prompt is blocking")
            fiftyMillis.sleep()
        }

        log("${unreadCount.bytes.formattedAs.debug} unread")

        if (terminated) {
            log("Backing buffer is depleted ➜ EOF reached.")
            return@spanningWithDisabledPrinterOnIllegalSpan -1
        }

        val yetBlocked = blockUntil - System.currentTimeMillis()
        if (yetBlocked > 0) {
            log("blocking for the remaining ${yetBlocked.milli.seconds}…")
            yetBlocked.milli.seconds.sleep()
        }

        val currentWord: MutableList<Byte> = unread.let {
            val currentLine: Pair<Duration, MutableList<Byte>> = it.first()
            val delay = currentLine.first
            if (delay > Duration.ZERO) {
                log("output delayed by $delay…")
                delay.sleep()
                unread[0] = Duration.ZERO to currentLine.second
            }
            currentLine.second
        }
        log("available:${currentWord.debug.formattedAs.debug}")
        val currentByte = currentWord.removeFirst()
        log("current:${currentByte.debug.formattedAs.debug}")

        if (currentWord.isEmpty()) {
            unread.removeFirst()
            val delay = baseDelayPerInput
            blockUntil = System.currentTimeMillis() + delay.inWholeMilliseconds
            log("empty".formattedAs.warning + " waiting time for next chunk is $baseDelayPerInput")
            ((delay / 2).takeIf { it > fiftyMillis } ?: fiftyMillis).sleep()
        }
        currentByte.toInt()
    }

    /**
     * Tries to behave exactly like [BufferedInputStream.read].
     */
    private fun read1(b: ByteArray, off: Int, len: Int): Int {
        val available = available()
        val count = if (available < len) available else len
        (0 until count).map { i ->
            b[off + i] = read().toByte()
        }
        return count
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
            n += readCount
            if (n >= len) return n
            if ((closed || terminated) && available() <= 0) return n
        }
    }

    public fun input(text: String): Unit = runSpanning("input") {
        if (handleAndReturnBlockingState()) {
            log("Input received: $text")
            unread.removeFirst()
        }
    }

    override fun close() {
        closed = true
    }

    override fun toString(): String = "${unreadCount.bytes} left"
}

private fun <R> Span?.spanningWithDisabledPrinterOnIllegalSpan(name: String, block: SpanScope.() -> R): R =
    this?.takeIf { it.spanId.valid }?.makeCurrent()?.use {
        spanningLine(name, block = block)
    } ?: Span.getInvalid().makeCurrent().use {
        spanningLine(name, printer = {}, block = block)
    }

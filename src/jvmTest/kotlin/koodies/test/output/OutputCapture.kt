package koodies.test.output

import koodies.exec.IO
import koodies.exec.IO.Error
import koodies.exec.IO.Output
import koodies.test.CapturedOutput
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class OutputCapture(private val print: Boolean) : CapturedOutput {

    private val lock = ReentrantLock()
    private val systemCaptures: ArrayDeque<SystemCapture> = ArrayDeque()
    fun push() = lock.withLock { systemCaptures.addLast(SystemCapture(print)) }
    fun pop() = lock.withLock { systemCaptures.removeLast().release() }

    val isCapturing: Boolean get() = systemCaptures.isNotEmpty()

    override val all: String get() = getFilteredCapture<IO>()
    override val out: String get() = getFilteredCapture<Output>()
    override val err: String get() = getFilteredCapture<Error>()

    /**
     * Resets the current capture session, clearing its captured output.
     */
    fun reset() = systemCaptures.firstOrNull()?.reset()

    private inline fun <reified T : IO> getFilteredCapture(): String = lock.withLock {
        check(systemCaptures.isNotEmpty()) { "No system captures found. Please check your output capture registration." }
        val builder = StringBuilder()
        systemCaptures.forEach { systemCapture ->
            systemCapture.useCapturedStrings { io -> if (io is T) builder.append(io) }
        }
        builder.toString()
    }

    override fun hashCode(): Int = all.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        return if (other is CapturedOutput || other is CharSequence) {
            all == other.toString()
        } else false
    }

    override val length: Int get() = all.length
    override fun get(index: Int): Char = all[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = all.subSequence(startIndex, endIndex)
    override fun toString(): String = all
}

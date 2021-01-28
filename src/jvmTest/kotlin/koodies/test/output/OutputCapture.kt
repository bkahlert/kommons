package koodies.test.output

import koodies.collections.withNegativeIndices
import koodies.concurrent.process.IO
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class OutputCapture : CapturedOutput {
    companion object {
        fun splitOutput(output: String): List<String> = output.lines().dropLastWhile { it.isBlank() }
    }

    private val lock = ReentrantLock()
    private val systemCaptures: ArrayDeque<SystemCapture> = ArrayDeque()
    fun push() = lock.withLock { systemCaptures.addLast(SystemCapture()) }
    fun pop() = lock.withLock { systemCaptures.removeLast().release() }

    val isCapturing: Boolean get() = systemCaptures.isNotEmpty()

    override val all: String get() = getFilteredCapture { true }
    override val allLines: List<String> by withNegativeIndices { splitOutput(all) }

    override val out: String get() = getFilteredCapture { other: IO.Type? -> IO.Type.OUT == other }
    override val outLines: List<String> by withNegativeIndices { splitOutput(out) }

    override val err: String get() = getFilteredCapture { other: IO.Type? -> IO.Type.ERR == other }
    override val errLines: List<String> by withNegativeIndices { splitOutput(err) }

    /**
     * Resets the current capture session, clearing its captured output.
     */
    fun reset() = systemCaptures.firstOrNull()?.reset()

    private fun getFilteredCapture(filter: (IO.Type) -> Boolean): String = lock.withLock {
        check(!this.systemCaptures.isEmpty()) { "No system captures found. Please check your output capture registration." }
        val builder = StringBuilder()
        for (systemCapture in systemCaptures) {
            systemCapture.append(builder, filter)
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

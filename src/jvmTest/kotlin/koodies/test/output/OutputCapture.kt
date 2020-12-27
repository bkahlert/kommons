package koodies.test.output

import koodies.collections.withNegativeIndices


class OutputCapture : CapturedOutput {
    companion object {
        fun splitOutput(output: String): List<String> = output.lines().dropLastWhile { it.isBlank() }
    }

    private val monitor = Any()
    private val systemCaptures: ArrayDeque<SystemCapture> = ArrayDeque()
    fun push() {
        synchronized(monitor) { systemCaptures.addLast(SystemCapture()) }
    }

    fun pop() {
        synchronized(monitor) { systemCaptures.removeLast().release() }
    }

    val isCapturing: Boolean get() = systemCaptures.isNotEmpty()

    override val all: String get() = getFilteredCapture { true }
    override val allLines: List<String> by withNegativeIndices { splitOutput(all) }

    override val out: String get() = getFilteredCapture { other: Type? -> Type.OUT == other }
    override val outLines: List<String> by withNegativeIndices { splitOutput(out) }

    override val err: String get() = getFilteredCapture { other: Type? -> Type.ERR == other }
    override val errLines: List<String> by withNegativeIndices { splitOutput(err) }

    /**
     * Resets the current capture session, clearing its captured output.
     */
    fun reset() = systemCaptures.lastOrNull()?.reset()

    private fun getFilteredCapture(filter: (Type) -> Boolean): String {
        return synchronized(monitor) {
            check(!this.systemCaptures.isEmpty()) { "No system captures found. Please check your output capture registration." }
            val builder = StringBuilder()
            for (systemCapture in systemCaptures) {
                systemCapture.append(builder, filter)
            }
            builder.toString()
        }
    }

    /**
     * Types of content that can be captured.
     */
    @Deprecated("Use Output instead")
    enum class Type {
        OUT, ERR
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

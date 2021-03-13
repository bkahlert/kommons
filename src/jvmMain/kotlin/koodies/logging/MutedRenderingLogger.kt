package koodies.logging

/**
 * A logger that can be used if no logging is needed.
 */
public open class MutedRenderingLogger : BlockRenderingLogger("", log = {}) {

    override fun logText(block: () -> CharSequence): Unit = Unit
    override fun logLine(block: () -> CharSequence): Unit = Unit
    override fun logStatus(items: List<HasStatus>, block: () -> CharSequence): Unit = Unit
    override fun logException(block: () -> Throwable): Unit = Unit
    override fun <R> logResult(block: () -> Result<R>): R = block().getOrThrow()
    override fun toString(): String = "MutedBlockRenderingLogger"
}

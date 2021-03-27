package koodies.logging

import koodies.asString

/**
 * A logger that can be used if no logging is needed.
 */
public open class MutedRenderingLogger(
    parent: RenderingLogger? = null,
) : BlockRenderingLogger("", parent, log = {}) {

    init {
        withUnclosedWarningDisabled
    }

    override fun logText(block: () -> CharSequence): Unit = Unit
    override fun logLine(block: () -> CharSequence): Unit = Unit
    override fun logStatus(items: List<HasStatus>, block: () -> CharSequence): Unit = Unit
    override fun <R> logResult(block: () -> Result<R>): R {
        closed = true
        return block().getOrThrow()
    }

    override fun logException(block: () -> Throwable): Unit {
        closed = true
    }

    override fun toString(): String = asString {
        ::parent to parent?.caption
    }
}

package koodies.tracing.rendering

import koodies.exception.toCompactString
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.SmartRenderingLogger
import koodies.logging.logResult
import koodies.text.Semantics.formattedAs
import koodies.toBaseName
import koodies.tracing.Span.State.Ended
import koodies.tracing.Span.State.Ended.Failed
import koodies.tracing.Span.State.Ended.Succeeded
import koodies.tracing.Span.State.Started

public interface Renderer {
    public fun start(name: CharSequence, started: Started)
    public fun event(name: CharSequence, description: CharSequence, attributes: Map<CharSequence, CharSequence> = emptyMap())
    public fun exception(exception: Throwable, attributes: Map<CharSequence, CharSequence> = emptyMap())
    public fun spanning(name: CharSequence): Renderer
    public fun end(ended: Ended)

    public companion object {
        public val NOOP: Renderer = object : Renderer {
            override fun start(name: CharSequence, started: Started): Unit = Unit
            override fun event(name: CharSequence, description: CharSequence, attributes: Map<CharSequence, CharSequence>): Unit = Unit
            override fun exception(exception: Throwable, attributes: Map<CharSequence, CharSequence>): Unit = Unit
            override fun spanning(name: CharSequence): Renderer = this
            override fun end(ended: Ended): Unit = Unit
        }
    }
}

/**
 * Renders an event using the given [description] and optional [attributes].
 *
 * Attributes with a `null` value are removed and rendered using the provided [transform],
 * that calls [CharSequence.toString] by default.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T : Renderer> T.event(
    description: CharSequence,
    vararg attributes: Pair<CharSequence, Any?>,
    transform: Any.() -> CharSequence = { toString() },
): Unit = event(description.toBaseName(), description,
    *attributes.mapNotNull { (key, value) -> value?.let { key to it.transform() } }.toMap())

/**
 * Renders the given [exception] using the optional [attributes].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T : Renderer> T.exception(
    exception: Throwable,
    vararg attributes: Pair<CharSequence, Any?>,
    transform: Any.() -> CharSequence = { toString() },
): Unit = exception(exception,
    *attributes.mapNotNull { (key, value) -> value?.let { key to it.transform() } }.toMap())

public fun <T : FixedWidthRenderingLogger> T.toRenderer(): Renderer {
    return object : Renderer {

        private lateinit var name: CharSequence
        override fun start(name: CharSequence, started: Started) {
            this.name = name
        }

        override fun event(name: CharSequence, description: CharSequence, attributes: Map<CharSequence, CharSequence>) {
            logLine { description }
        }

        override fun exception(exception: Throwable, attributes: Map<CharSequence, CharSequence>) {
            logLine { exception.toCompactString().formattedAs.error }
        }

        override fun spanning(name: CharSequence): Renderer {
            return SmartRenderingLogger(
                name,
                this@toRenderer,
                { logText { it } },
                this@toRenderer.contentFormatter,
                this@toRenderer.decorationFormatter,
                this@toRenderer.returnValueFormatter,
                this@toRenderer.border,
                statusInformationColumn,
                statusInformationPadding,
                statusInformationColumns,
                prefix,
            ).toRenderer()
        }

        override fun end(ended: Ended) {
            when (ended) {
                is Succeeded -> ended.value?.let { logResult(it) } ?: logResult()
                is Failed -> logResult(ended.exception)
            }
        }
    }
}

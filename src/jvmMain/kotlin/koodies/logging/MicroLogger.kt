package koodies.logging

import koodies.text.GraphemeCluster
import kotlin.properties.Delegates.vetoable
import kotlin.reflect.KProperty

public abstract class MicroLogger(private val symbol: GraphemeCluster? = null) : RenderingLogger {

    public var strings: List<String>? by vetoable(listOf(),
        onChange = { _: KProperty<*>, oldValue: List<String>?, _: List<String>? -> oldValue != null })

    public abstract fun render(block: () -> CharSequence)

    override fun render(trailingNewline: Boolean, block: () -> CharSequence) {
        strings = strings?.plus("${block()}")
    }

    override fun logStatus(items: List<HasStatus>, block: () -> CharSequence) {
        strings = strings?.plus(block().lines().joinToString(", "))
        if (items.isNotEmpty()) strings =
            strings?.plus(items.renderStatus().lines().size.let { "($it)" })
    }

    override fun <R> logResult(block: () -> Result<R>): R {
        val returnValue = super.logResult(block)
        render { strings?.joinToString(prefix = "(" + (symbol?.let { "$it " } ?: ""), separator = " ˃ ", postfix = ")") ?: "" }
        return returnValue
    }
}

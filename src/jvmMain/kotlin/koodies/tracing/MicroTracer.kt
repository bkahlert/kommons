package koodies.tracing

import koodies.text.Grapheme

public interface MicroTracer {
    public fun trace(input: String)
}

public fun MicroTracer?.trace(input: String): Unit = this?.trace(input) ?: Unit

public class SimpleMicroTracer(private val symbol: Grapheme) : MicroTracer {
    private val traces = mutableListOf<String>()
    override fun trace(input: String) {
        traces.add(input)
    }

    public fun render(): String = traces.joinToString(prefix = "($symbol ", separator = " ˃ ", postfix = ")")
}

public fun <R> MiniTracer?.microTrace(symbol: Grapheme, block: MicroTracer?.() -> R): R {
    val simpleMicroTracer = SimpleMicroTracer(symbol)
    val returnValue: R = simpleMicroTracer.run(block)
    this?.trace(simpleMicroTracer.render())
    return returnValue
}

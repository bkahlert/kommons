package koodies.tracing

import koodies.text.Grapheme

interface MicroTracer {
    fun trace(input: String)
}

fun MicroTracer?.trace(input: String) = this?.trace(input)

class SimpleMicroTracer(private val symbol: Grapheme) : MicroTracer {
    private val traces = mutableListOf<String>()
    override fun trace(input: String) {
        traces.add(input)
    }

    fun render(): String = traces.joinToString(prefix = "($symbol ", separator = " ˃ ", postfix = ")")
}

fun <R> MiniTracer?.microTrace(symbol: Grapheme, block: MicroTracer?.() -> R): R {
    val simpleMicroTracer = SimpleMicroTracer(symbol)
    val returnValue: R = simpleMicroTracer.run(block)
    this?.trace(simpleMicroTracer.render())
    return returnValue
}

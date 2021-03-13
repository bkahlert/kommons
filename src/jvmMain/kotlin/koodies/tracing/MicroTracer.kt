package koodies.tracing

import koodies.text.GraphemeCluster

public interface MicroTracer {
    public fun trace(input: String)
}

public fun MicroTracer.trace(input: String): Unit = this?.trace(input)

public fun trace(input: String): Unit = Unit

public class SimpleMicroTracer(private val symbol: GraphemeCluster) : MicroTracer {
    private val traces = mutableListOf<String>()
    override fun trace(input: String) {
        traces.add(input)
    }

    public fun render(): String = traces.joinToString(prefix = "($symbol ", separator = " ˃ ", postfix = ")")
}

public fun <R> microTrace(symbol: GraphemeCluster, block: MicroTracer?.() -> R): R =
    SimpleMicroTracer(symbol).run(block)

package koodies.tracing

import koodies.builder.buildMap
import koodies.text.ANSI.ansiRemoved

public interface Event {
    public val name: String
    public val attributes: Map<String, String>
}

public fun Span.record(event: Event): Unit =
    event(event.name.ansiRemoved, *event.attributes.map { (key, value) -> key.ansiRemoved to value.ansiRemoved }.toTypedArray())

public open class OpenTelemetryEvent(
    name: CharSequence,
    description: CharSequence?,
    vararg attributes: Pair<CharSequence, Any>,
) : Event {
    public constructor(name: CharSequence, vararg attributes: Pair<CharSequence, Any>) : this(name, null, *attributes)

    override val name: String = name.ansiRemoved
    override val attributes: Map<String, String> = buildMap {
        description?.also { put("description", it.ansiRemoved) }
        attributes.forEach { (key, value) -> put(key.ansiRemoved, value.toString().ansiRemoved) }
    }
}

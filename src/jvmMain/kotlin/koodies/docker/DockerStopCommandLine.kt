package koodies.docker

import koodies.builder.buildArray
import koodies.text.Semantics.FieldDelimiters
import koodies.text.Semantics.formattedAs
import koodies.text.spaced
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * [DockerCommandLine] that stops the specified [containers].
 */
public open class DockerStopCommandLine(
    /**
     * Containers to be stopped.
     */
    public vararg val containers: String,
    /**
     * [Duration] to wait for stop before killing it. Timeouts only support a resolution of 1 second.
     * Fractions are rounded according to [roundToInt].
     */
    public val time: Duration?,
) : DockerCommandLine(
    dockerCommand = "stop",
    arguments = buildArray {
        time?.also { +"--time" + "${time.inWholeSeconds}" }
        addAll(containers)
    },
    name = "Stopping ${containers.joinToString(FieldDelimiters.FIELD.spaced) { it.formattedAs.input }}"
)

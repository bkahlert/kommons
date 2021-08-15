package com.bkahlert.kommons.docker

import com.bkahlert.kommons.builder.buildArray
import com.bkahlert.kommons.text.Semantics.FieldDelimiters
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.spaced
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

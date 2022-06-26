package com.bkahlert.kommons.docker

import com.bkahlert.kommons.startSpaced
import com.bkahlert.kommons.text.Semantics.formattedAs

/**
 * [DockerCommandLine] that lists locally available instances of [DockerContainer].
 */
public open class DockerPsCommandLine(
    /**
     * Show all images (default hides intermediate images)
     */
    public val all: Boolean = false,
    /**
     * Filter output based on conditions provided
     */
    public vararg val filters: Pair<String, String>,
) : DockerCommandLine(
    dockerCommand = "ps",
    arguments = buildList {
        if (all) add("--all")
        filters.forEach { (key, value) -> add("--filter"); add("$key=$value") }
        add("--no-trunc")
        add("--format")
        add("{{.Names}}\t{{.State}}\t{{.Status}}")
    },
    name = kotlin.run {
        val allString = if (all) "all".formattedAs.warning.startSpaced else ""
        val filterString = if (filters.isNotEmpty()) " matching ${filters.toMap()}" else ""
        "Listing$allString containers$filterString"
    },
) {
    public constructor(all: Boolean, exactName: String) : this(all, "name" to "^$exactName${'$'}")
}

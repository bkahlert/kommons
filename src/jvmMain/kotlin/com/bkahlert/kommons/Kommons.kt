package com.bkahlert.kommons

import com.bkahlert.kommons.io.path.selfCleaning
import java.nio.file.Path
import java.util.Properties
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Entrypoint for library-internal functionality.
 */
public object Kommons {

    /**
     * Directory in which library-specific data can be stored.
     */
    internal val InternalTemp: Path by SystemLocations.Temp.resolve("kommons").selfCleaning(30.days, 1000)

    /**
     * Directory in which Exec-specific data can be stored.
     */
    internal val ExecTemp: Path by InternalTemp.resolve("exec").selfCleaning(1.hours, 1000)

    /**
     * Directory in which files can be stored.
     */
    internal val FilesTemp: Path by InternalTemp.resolve("files").selfCleaning(10.minutes, 20)

    private val buildProperties: Properties by lazy {
        ClassPath("build.properties").useInputStream { Properties().apply { load(it) } }
    }

    /**
     * Name of this library.
     */
    public val name: String
        get() = buildProperties["name"]?.toString() ?: error("Cannot find name in build properties")

    /**
     * Group name of this library.
     */
    public val group: String
        get() = buildProperties["group"]?.toString() ?: error("Cannot find group in build properties")

    /**
     * Version of this library.
     */
    public val version: SemanticVersion
        get() = SemanticVersion.parse(buildProperties["version"]?.toString() ?: error("Cannot find version in build properties"))
}

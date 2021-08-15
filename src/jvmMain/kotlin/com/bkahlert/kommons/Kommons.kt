package com.bkahlert.kommons

import com.bkahlert.kommons.io.path.Locations
import com.bkahlert.kommons.io.path.selfCleaning
import com.bkahlert.kommons.io.useRequiredClassPath
import com.bkahlert.kommons.time.days
import com.bkahlert.kommons.time.hours
import com.bkahlert.kommons.time.minutes
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.inputStream

/**
 * Entrypoint for library-internal functionality.
 */
public object Kommons : Locations {

    /**
     * Directory in which library-specific data can be stored.
     */
    internal val internalTemp: Path by temp.resolve("kommons").selfCleaning(30.days, 1000)

    /**
     * Directory in which Exec-specific data can be stored.
     */
    internal val execTemp: Path by internalTemp.resolve("exec").selfCleaning(1.hours, 1000)

    /**
     * Directory in which files can be stored.
     */
    internal val filesTemp: Path by internalTemp.resolve("files").selfCleaning(10.minutes, 20)

    private val buildProperties: Properties by lazy {
        useRequiredClassPath("build.properties") { Properties().apply { load(it.inputStream()) } }
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
    public val version: SemVer
        get() = SemVer.parse(buildProperties["version"]?.toString() ?: error("Cannot find version in build properties"))
}

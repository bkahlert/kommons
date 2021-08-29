package com.bkahlert.kommons

import com.bkahlert.kommons.io.path.Locations
import com.bkahlert.kommons.io.path.SelfCleaningDirectory.CleanUpMode.OnStart
import com.bkahlert.kommons.io.path.selfCleaning
import com.bkahlert.kommons.text.ANSI.FilteringFormatter
import com.bkahlert.kommons.text.ANSI.Text
import com.bkahlert.kommons.text.joinLinesToString
import com.bkahlert.kommons.text.styling.draw
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Entrypoint for library-internal functionality.
 */
object TestKommons : Locations {

    /**
     * Directory in which all artifacts of a test run are stored.
     */
    val TestRoot: Path by Locations.temp.resolve("kommons-test").selfCleaning(Duration.ZERO, 0, cleanUpMode = OnStart)
}

fun printTestExecutionStatus(vararg lines: CharSequence, formatBorder: Text.() -> CharSequence?) {
    lines
        .joinLinesToString()
        .draw.border.rounded(padding = 2, margin = 0, formatter = FilteringFormatter.fromScratch(formatBorder))
        .also { println(it) }
}

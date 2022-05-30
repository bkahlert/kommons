package com.bkahlert.kommons

import com.bkahlert.kommons.io.path.Locations
import com.bkahlert.kommons.io.path.SelfCleaningDirectory.CleanUpMode.OnStart
import com.bkahlert.kommons.io.path.selfCleaning
import com.bkahlert.kommons.test.HtmlFixture
import com.bkahlert.kommons.text.ANSI.FilteringFormatter
import com.bkahlert.kommons.text.ANSI.Text
import com.bkahlert.kommons.text.joinLinesToString
import com.bkahlert.kommons.text.styling.draw
import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Entrypoint for library-internal functionality.
 */
object TestKommons : Locations by Locations.Default {

    /**
     * Directory in which all artifacts of a test run are stored.
     */
    val TestRoot: Path by Temp.resolve("kommons-test").selfCleaning(Duration.ZERO, 0, cleanUpMode = OnStart)
}

/**
 * Prints the given [lines] enclosed in a border that is formatted using [formatBorder].
 */
fun printTestExecutionStatus(vararg lines: CharSequence, formatBorder: Text.() -> CharSequence?) {
    lines
        .joinLinesToString()
        .draw.border.rounded(padding = 2, margin = 0, formatter = FilteringFormatter.fromScratch(formatBorder))
        .also { println(it) }
}

/**
 * Starts a short-lived webserver on the provided [port] that serves [responseText]
 * at `/` and runs the specified [block].
 */
fun <R> http(
    port: Int = 8000,
    responseText: String = HtmlFixture.text,
    block: () -> R,
): R {
    val engine = embeddedServer(Netty, port = port) {
        routing {
            get("/") {
                call.respondText(responseText)
            }
        }
    }.start(wait = false)
    val result = runCatching(block)
    engine.stop(0L, 0L)
    return result.getOrThrow()
}

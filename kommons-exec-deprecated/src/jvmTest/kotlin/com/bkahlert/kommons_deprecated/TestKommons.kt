package com.bkahlert.kommons_deprecated

import com.bkahlert.kommons.SystemLocations
import com.bkahlert.kommons.test.fixtures.HtmlDocumentFixture
import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons_deprecated.io.path.SelfCleaningDirectory.CleanUpMode.OnStart
import com.bkahlert.kommons_deprecated.io.path.selfCleaning
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
object TestKommons {

    /**
     * Directory in which all artifacts of a test run are stored.
     */
    val TestRoot: Path by SystemLocations.Temp.resolve("kommons-test").selfCleaning(Duration.ZERO, 0, cleanUpMode = OnStart)
}

/**
 * Prints the given [lines] enclosed in a border that is formatted using [formatBorder].
 */
fun printTestExecutionStatus(vararg lines: CharSequence) {
    lines
        .joinToString(LineSeparators.Default)
        .also { println(it) }
}

/**
 * Starts a short-lived webserver on the provided [port] that serves [responseText]
 * at `/` and runs the specified [block].
 */
fun <R> http(
    port: Int = 8000,
    responseText: String = HtmlDocumentFixture.contents,
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
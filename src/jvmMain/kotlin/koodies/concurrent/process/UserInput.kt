package koodies.concurrent.process

import koodies.text.LineSeparators.CRLF
import koodies.text.withSuffix
import java.io.BufferedWriter
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.milliseconds

/**
 * Provides extension functions to simulate user input.
 */
object UserInput {

    /**
     * Write the given [input] strings with a slight delay between
     * each input on the [Process]'s [InputStream].
     */
    fun Process.enter(vararg input: String, delay: Duration = 10.milliseconds): Unit =
        inputStream.enter(*input, delay = delay)


    /**
     * Write the given [input] strings with a slight delay between
     * each input on the [Process]'s [InputStream].
     */
    fun Process.input(vararg input: String, delay: Duration = 10.milliseconds): Unit =
        inputStream.enter(*input, delay = delay)

    /**
     * Write the given [input] strings with a slight delay between
     * each input on the [Process]'s [InputStream].
     */
    fun OutputStream.enter(vararg input: String, delay: Duration = 10.milliseconds) {
        val stdin = BufferedWriter(OutputStreamWriter(this))
        input.forEach {
            TimeUnit.MILLISECONDS.sleep(delay.toLongMilliseconds())
            stdin.write(it.withSuffix(CRLF))
            stdin.flush()
        }
    }
}

package koodies.nio

import koodies.asString
import koodies.io.ByteArrayOutputStream
import koodies.text.LineSeparators
import koodies.text.LineSeparators.hasTrailingLineSeparator
import koodies.text.LineSeparators.lines
import koodies.text.LineSeparators.removeTrailingLineSeparator
import koodies.text.toByteArray
import java.io.InputStream

/**
 * A non-blocking line reader that reads its data from the specified [inputStream]
 * and calls the specified [lineProcessor] whenever a line of text was successfully
 * read. That is, a sequence of bytes ending with one of the known [LineSeparators].
 */
public open class NonBlockingLineReader(
    inputStream: InputStream,
    private val lineBuffer: ByteArrayOutputStream = ByteArrayOutputStream(),
    private val lineProcessor: (String) -> Unit,
) : NonBlockingPipe(inputStream, lineBuffer) {

    override fun readChannelRead() {
        val fullyRead: StringBuilder = StringBuilder()
        val read = lineBuffer.toString(Charsets.UTF_8)

        read.lines(keepDelimiters = true)
            .let { if (it.isNotEmpty() && it.last().isEmpty()) it.dropLast(1) else it }
            .filter { line -> line.hasTrailingLineSeparator || done }
            .forEach { line ->
                fullyRead.append(line)
                lineProcessor(line.removeTrailingLineSeparator)
            }
        lineBuffer.toByteArray().apply {
            lineBuffer.reset()
            lineBuffer.write(drop(fullyRead.toByteArray().size).toByteArray())
        }
    }

    override fun toString(): String = asString {
        ::lineBuffer.name to lineBuffer
        ::lineProcessor.name to lineProcessor
        ::inputStream.name to inputStream
        ::outputStream.name to outputStream
    }

    override fun close() {
        if (!done) {
            super.close()
            if (lineBuffer.size() > 0) readChannelRead()
        }
    }
}

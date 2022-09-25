package com.bkahlert.kommons.exec.io

import com.bkahlert.kommons.debug.asString
import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons.text.LineSeparators.endsWithLineSeparator
import com.bkahlert.kommons.text.LineSeparators.lines
import com.bkahlert.kommons.text.LineSeparators.removeTrailingLineSeparator
import java.io.InputStream

/**
 * A non-blocking line reader that reads its data from the specified [inputStream]
 * and calls the specified [lineProcessor] whenever a line of text was successfully
 * read. That is, a sequence of bytes ending with one of the known [LineSeparators].
 */
public open class NonBlockingLineReader(
    inputStream: InputStream,
    private val lineBuffer: ByteArrayOutputStream,
    private val lineProcessor: (String) -> Unit,
) : NonBlockingPipe(inputStream, lineBuffer) {
    public constructor(inputStream: InputStream, lineProcessor: (String) -> Unit) : this(inputStream, ByteArrayOutputStream(), lineProcessor)

    override fun readChannelRead() {
        val fullyRead: StringBuilder = StringBuilder()
        val read = lineBuffer.toString(Charsets.UTF_8)

        read.lines(keepDelimiters = true)
            .let { if (it.isNotEmpty() && it.last().isEmpty()) it.dropLast(1) else it }
            .filter { line -> line.endsWithLineSeparator() || done }
            .forEach { line ->
                fullyRead.append(line)
                lineProcessor(line.removeTrailingLineSeparator())
            }
        lineBuffer.toByteArray().apply {
            lineBuffer.reset()
            lineBuffer.write(drop(fullyRead.toString().toByteArray().size).toByteArray())
        }
    }


    override fun toString(): String = asString {
        put(::lineBuffer.name, lineBuffer)
        put(::lineProcessor.name, lineProcessor)
        put(::inputStream.name, inputStream)
        put(::outputStream.name, outputStream)
    }

    override fun close() {
        if (!done) {
            super.close()
            if (lineBuffer.size() > 0) readChannelRead()
        }
    }
}

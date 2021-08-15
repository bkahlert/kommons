package com.bkahlert.kommons.nio

import com.bkahlert.kommons.asString
import com.bkahlert.kommons.io.ByteArrayOutputStream
import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons.text.LineSeparators.hasTrailingLineSeparator
import com.bkahlert.kommons.text.LineSeparators.lines
import com.bkahlert.kommons.text.LineSeparators.trailingLineSeparatorRemoved
import com.bkahlert.kommons.text.toByteArray
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
            .filter { line -> line.hasTrailingLineSeparator || done }
            .forEach { line ->
                fullyRead.append(line)
                lineProcessor(line.trailingLineSeparatorRemoved)
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

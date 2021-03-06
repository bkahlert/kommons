package koodies.nio

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel

/**
 * A non-blocking pipe that reads data from the specified [inputStream]
 * and writes them to the specified [outputStream].
 *
 * Additionally [readChannelRead] is called whenever an attempt to
 * read data was made.
 *
 * In contrast to a blocking pipe this implementation does not need
 * multiple threads. On every [read] attempt, as much data as available
 * and as can be buffered is read. [done] automatically flips to `true`
 * as soon the the [inputStream] is depleted / closed.
 */
public open class NonBlockingPipe(
    public val inputStream: InputStream,
    public val outputStream: OutputStream,
) {
    private val readBuffer: ByteBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
    private val readChannel: ReadableByteChannel = Channels.newChannel(inputStream)
    private val writeChannel: WritableByteChannel = Channels.newChannel(outputStream)

    private var closed = false
    public val done: Boolean get() = closed

    public fun read() {
        while (true) {
            kotlin.runCatching {
                when (readChannel.read(readBuffer)) {
                    -1 -> {
                        closed = true
                        readChannelRead()
                        return
                    }
                    0 -> {
                        readChannelRead()
                        return
                    }
                    else -> {
                        readBuffer.flip()
                        writeChannel.write(readBuffer)
                        readBuffer.clear()
                        readChannelRead()
                    }
                }
            }.onFailure {
                closed = true
                readChannelRead()
                return
            }
        }
    }

    protected open fun readChannelRead(): Unit = Unit
}

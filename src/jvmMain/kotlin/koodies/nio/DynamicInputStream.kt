package koodies.nio

import koodies.io.ProxyInputStream
import java.nio.channels.Channels

/**
 * A readable (non-blocking) input stream of which the data are dynamically
 * provided by [yield].
 *
 * The implementation is mainly provided by [DynamicReadableByteChannel].
 *
 * @see DynamicReadableByteChannel
 */
public class DynamicInputStream private constructor(private val channel: DynamicReadableByteChannel) :
    ProxyInputStream(Channels.newInputStream(channel)) {

    public constructor() : this(DynamicReadableByteChannel())

    override fun available(): Int = channel.available

    override fun toString(): String = "DynamicInputStream${channel.toString().substring("DynamicReadableByteChannel".length)}"

    public fun yield(bytes: ByteArray): Unit = channel.yield(bytes)
}

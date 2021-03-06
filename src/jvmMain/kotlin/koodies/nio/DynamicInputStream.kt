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

    private val toStringStart by lazy { DynamicReadableByteChannel::class.simpleName?.length ?: 0 }
    private val toStringPrefix by lazy { DynamicInputStream::class.simpleName ?: "" }
    override fun toString(): String = toStringPrefix + channel.toString().substring(toStringStart)

    public fun yield(bytes: ByteArray): Unit = channel.yield(bytes)
}

package koodies.net

import java.net.InetAddress

fun IPAddress.toInetAddress(): InetAddress = InetAddress.getByAddress(bytes.toByteArray())

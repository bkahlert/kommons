package koodies.net

import java.net.InetAddress

fun Ip4Address.toInetAddress(): InetAddress = InetAddress.getByAddress(bytes)

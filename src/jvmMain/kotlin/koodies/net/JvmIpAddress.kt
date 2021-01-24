package koodies.net

import java.net.InetAddress

fun IPv4Address.toInetAddress(): InetAddress = InetAddress.getByAddress(bytes)

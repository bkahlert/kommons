package com.bkahlert.kommons.net

import java.net.InetAddress

public fun IPAddress.toInetAddress(): InetAddress = InetAddress.getByAddress(bytes.toByteArray())

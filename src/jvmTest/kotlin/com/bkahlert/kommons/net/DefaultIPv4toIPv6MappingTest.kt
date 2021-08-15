package com.bkahlert.kommons.net

import com.bkahlert.kommons.net.DefaultIPv4toIPv6Mapping.toIPv4Address
import com.bkahlert.kommons.net.DefaultIPv4toIPv6Mapping.toIPv6Address
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

class DefaultIPv4toIPv6MappingTest {

    private val ipv4 = IPv4Address.parse("192.168.16.1")
    private val ipv6 = IPv6Address.parse("::ffff:192.168.16.1")

    @Test
    fun `should convert IPv4 to IPv6 address`() {
        expectThat(ipv4.toIPv6Address()).isEqualTo(ipv6)
    }

    @Test
    fun `should convert IPv6 to IPv4 address`() {
        expectThat(ipv6.toIPv4Address()).isEqualTo(ipv4)
    }

    @TestFactory
    fun `should throw on mapping non-mappable IP6 address`() {
        expectCatching { IPv6Address.RANGE.endInclusive.toIPv4Address() }.isFailure().isA<IllegalArgumentException>()
    }
}

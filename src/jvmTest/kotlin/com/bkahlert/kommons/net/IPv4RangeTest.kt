package com.bkahlert.kommons.net

import com.bkahlert.kommons.test.testEachOld
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo

class IPv4RangeTest {

    private val ip1 = IPv4Address.parse("172.186.78.250")
    private val ip2 = IPv4Address.parse("192.168.0.1")
    private val range = ip1..ip2

    @TestFactory
    fun `should be parsable`() = testEachOld(
        "172.186.78.250..192.168.0.1",
        "172.186.78.250 ..192.168.0.1",
        "172.186.78.250.. 192.168.0.1",
        " 172.186.78.250..192.168.0.1",
        "172.186.78.250..192.168.0.1 ",
    ) { rangeString: String ->
        expecting { rangeString.toIPv4Range() } that { range }
        expecting { ip4RangeOf(rangeString) } that { range }
    }

    @TestFactory
    fun `should have start`() = testEachOld(
        range,
        IPv4Range.from(ip1, ip2),
    ) {
        expecting { start } that { isEqualTo(ip1) }
    }

    @TestFactory
    fun `should have inclusive end`() = testEachOld(
        range,
        IPv4Range.from(ip1, ip2),
    ) {
        expecting { endInclusive } that { isEqualTo(ip2) }
    }

    @TestFactory
    fun `should be representable`() = testEachOld(
        range,
        IPv4Range.from(ip1, ip2),
    ) {
        expecting { toString() } that { isEqualTo("172.186.78.250..192.168.0.1") }
    }
}

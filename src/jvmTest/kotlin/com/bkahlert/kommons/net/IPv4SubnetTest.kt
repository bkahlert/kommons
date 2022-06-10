package com.bkahlert.kommons.net

import com.bkahlert.kommons.bigIntegerOfDecimalString
import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.test.toStringIsEqualTo
import com.bkahlert.kommons.too
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo

class IPv4SubnetTest {

    private val ip = IPv4Address.parse("172.186.78.250")

    @TestFactory
    fun `should be parsable`() = testEachOld(
        "0.0.0.0/0" to "0.0.0.0..255.255.255.255",
        "172.186.0.0/15" to "172.186.0.0..172.187.255.255",
        "172.186.0.0/16" to "172.186.0.0..172.186.255.255",
        "172.186.0.0/17" to "172.186.0.0..172.186.127.255",
        "172.186.78.250/32" to "172.186.78.250..172.186.78.250",
    ) { (subnetString, rangeString) ->
        expecting { subnetString.toIPv4Subnet() } that { isEqualTo(rangeString.toIPv4Subnet()) }
        expecting { ip4SubnetOf(subnetString) } that { isEqualTo(ip4SubnetOf(rangeString)) }
    }

    @TestFactory
    fun `should have mask`() = testEachOld(
        0 to "0",
        15 to "fffe0000",
        16 to "ffff0000",
        17 to "ffff8000",
        32 to "ffffffff",
    ) { (length, mask) ->
        with { IPv4Subnet.from(ip, length) }.then {
            expecting { this.mask.toString(16) } that { isEqualTo(mask) }
        }
    }

    @TestFactory
    fun `should have start matching network address`() = testEachOld(
        0 to "0.0.0.0",
        15 to "172.186.0.0",
        16 to "172.186.0.0",
        17 to "172.186.0.0",
        32 to "172.186.78.250",
    ) { (length, networkAddress) ->
        with { IPv4Subnet.from(ip, length) }.then {
            expecting { this.start } that { toStringIsEqualTo(networkAddress) }
            expecting { this.networkAddress } that { toStringIsEqualTo(networkAddress) }
        }
    }

    @TestFactory
    fun `should have inclusive end matching broadcast address`() = testEachOld(
        0 to "255.255.255.255",
        15 to "172.187.255.255",
        16 to "172.186.255.255",
        17 to "172.186.127.255",
        32 to "172.186.78.250",
    ) { (length, broadcastAddress) ->
        with { IPv4Subnet.from(ip, length) }.then {
            expecting { this.endInclusive } that { toStringIsEqualTo(broadcastAddress) }
            expecting { this.broadcastAddress } that { toStringIsEqualTo(broadcastAddress) }
        }
    }

    @TestFactory
    fun `should have usable host count`() = testEachOld(
        0 to "4294967296" too "4294967294",
        15 to "131072" too "131070",
        16 to "65536" too "65534",
        17 to "32768" too "32766",
        32 to "1" too "1",
    ) { (length, hostCountString, usableHostCountString) ->
        val hostCount = bigIntegerOfDecimalString(hostCountString)
        val usableHostCount = bigIntegerOfDecimalString(usableHostCountString)
        with { IPv4Subnet.from(ip, length) }.then {
            expecting { this.hostCount } that { isEqualTo(hostCount) }
            expecting { this.usableHostCount } that { isEqualTo(usableHostCount) }
        }
    }

    @TestFactory
    fun `should have first and usable host`() = testEachOld(
        0 to "0.0.0.1" too "255.255.255.254",
        15 to "172.186.0.1" too "172.187.255.254",
        16 to "172.186.0.1" too "172.186.255.254",
        17 to "172.186.0.1" too "172.186.127.254",
        32 to "172.186.78.250" too "172.186.78.250",
    ) { (length, firstUsableHost, lastUsableHost) ->
        with { IPv4Subnet.from(ip, length) }.then {
            expecting { this.firstUsableHost } that { toStringIsEqualTo(firstUsableHost) }
            expecting { this.lastUsableHost } that { toStringIsEqualTo(lastUsableHost) }
        }
    }

    @TestFactory
    fun `should be representable`() = testEachOld(
        0 to "0.0.0.0/0",
        15 to "172.186.0.0/15",
        16 to "172.186.0.0/16",
        17 to "172.186.0.0/17",
        32 to "172.186.78.250/32",
    ) { (length, representation) ->
        expecting { IPv4Subnet.from(ip, length) } that { toStringIsEqualTo(representation) }
    }

    @TestFactory
    fun `should be instantiatable by div`() = testEachOld(
        0 to "0.0.0.0/0",
        15 to "172.186.0.0/15",
        16 to "172.186.0.0/16",
        17 to "172.186.0.0/17",
        32 to "172.186.78.250/32",
    ) { (length, representation) ->
        expecting { ip / length } that { toStringIsEqualTo(representation) }
    }
}

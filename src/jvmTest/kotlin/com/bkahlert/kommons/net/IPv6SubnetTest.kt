package com.bkahlert.kommons.net

import com.bkahlert.kommons.bigIntegerOfDecimalString
import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.test.toStringIsEqualTo
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo

class IPv6SubnetTest {

    private val ip = IPv6Address.parse("abba:4efa:abba:4efa:abba:4efa:abba:4efa")

    @TestFactory
    fun `should be parsable`() = testEachOld(
        "::/0" to "::..ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
        "abba:4efa:abba:4efa::/63" to "abba:4efa:abba:4efa::..abba:4efa:abba:4efb:ffff:ffff:ffff:ffff",
        "abba:4efa:abba:4efa::/64" to "abba:4efa:abba:4efa::..abba:4efa:abba:4efa:ffff:ffff:ffff:ffff",
        "abba:4efa:abba:4efa:8000::/65" to "abba:4efa:abba:4efa:8000::..abba:4efa:abba:4efa:ffff:ffff:ffff:ffff",
        "abba:4efa:abba:4efa:abba:4efa:abba:4efa/128" to "abba:4efa:abba:4efa:abba:4efa:abba:4efa..abba:4efa:abba:4efa:abba:4efa:abba:4efa",
    ) { (subnetString, rangeString) ->
        expecting { subnetString.toIPv6Subnet() } that { isEqualTo(rangeString.toIPv6Subnet()) }
        expecting { ip6SubnetOf(subnetString) } that { isEqualTo(ip6SubnetOf(rangeString)) }
    }

    @TestFactory
    fun `should have mask`() = testEachOld(
        0 to "0",
        63 to "fffffffffffffffe0000000000000000",
        64 to "ffffffffffffffff0000000000000000",
        65 to "ffffffffffffffff8000000000000000",
        128 to "ffffffffffffffffffffffffffffffff",
    ) { (length, mask) ->
        with { IPv6Subnet.from(ip, length) }.then {
            expecting { this.mask.toString(16) } that { isEqualTo(mask) }
        }
    }

    @TestFactory
    fun `should have start matching network address`() = testEachOld(
        0 to "::",
        63 to "abba:4efa:abba:4efa::",
        64 to "abba:4efa:abba:4efa::",
        65 to "abba:4efa:abba:4efa:8000::",
        128 to "abba:4efa:abba:4efa:abba:4efa:abba:4efa",
    ) { (length, networkAddress) ->
        with { IPv6Subnet.from(ip, length) }.then {
            expecting { this.start } that { toStringIsEqualTo(networkAddress) }
            expecting { this.networkAddress } that { toStringIsEqualTo(networkAddress) }
        }
    }

    @TestFactory
    fun `should have inclusive end matching broadcast address`() = testEachOld(
        0 to "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
        63 to "abba:4efa:abba:4efb:ffff:ffff:ffff:ffff",
        64 to "abba:4efa:abba:4efa:ffff:ffff:ffff:ffff",
        65 to "abba:4efa:abba:4efa:ffff:ffff:ffff:ffff",
        128 to "abba:4efa:abba:4efa:abba:4efa:abba:4efa",
    ) { (length, broadcastAddress) ->
        with { IPv6Subnet.from(ip, length) }.then {
            expecting { this.endInclusive } that { toStringIsEqualTo(broadcastAddress) }
            expecting { this.broadcastAddress } that { toStringIsEqualTo(broadcastAddress) }
        }
    }

    @TestFactory
    fun `should have usable host count`() = testEachOld(
        Triple(0, "340282366920938463463374607431768211456", "340282366920938463463374607431768211454"),
        Triple(63, "36893488147419103232", "36893488147419103230"),
        Triple(64, "18446744073709551616", "18446744073709551614"),
        Triple(65, "9223372036854775808", "9223372036854775806"),
        Triple(128, "1", "1"),
    ) { (length, hostCountString, usableHostCountString) ->
        val hostCount = bigIntegerOfDecimalString(hostCountString)
        val usableHostCount = bigIntegerOfDecimalString(usableHostCountString)
        with { IPv6Subnet.from(ip, length) }.then {
            expecting { this.hostCount } that { isEqualTo(hostCount) }
            expecting { this.usableHostCount } that { isEqualTo(usableHostCount) }
        }
    }

    @TestFactory
    fun `should have first and usable host`() = testEachOld(
        Triple(0, "::1", "ffff:ffff:ffff:ffff:ffff:ffff:ffff:fffe"),
        Triple(63, "abba:4efa:abba:4efa:::1", "abba:4efa:abba:4efb:ffff:ffff:ffff:fffe"),
        Triple(64, "abba:4efa:abba:4efa:::1", "abba:4efa:abba:4efa:ffff:ffff:ffff:fffe"),
        Triple(65, "abba:4efa:abba:4efa:8000:::1", "abba:4efa:abba:4efa:ffff:ffff:ffff:fffe"),
        Triple(128, "abba:4efa:abba:4efa:abba:4efa:abba:4efa", "abba:4efa:abba:4efa:abba:4efa:abba:4efa"),
    ) { (length, firstUsableHost, lastUsableHost) ->
        with { IPv6Subnet.from(ip, length) }.then {
            expecting { this.firstUsableHost } that { toStringIsEqualTo(firstUsableHost) }
            expecting { this.lastUsableHost } that { toStringIsEqualTo(lastUsableHost) }
        }
    }

    @TestFactory
    fun `should be representable`() = testEachOld(
        0 to "::/0",
        63 to "abba:4efa:abba:4efa::/63",
        64 to "abba:4efa:abba:4efa::/64",
        65 to "abba:4efa:abba:4efa:8000::/65",
        128 to "abba:4efa:abba:4efa:abba:4efa:abba:4efa/128",
    ) { (length, representation) ->
        expecting { IPv6Subnet.from(ip, length) } that { toStringIsEqualTo(representation) }
    }

    @TestFactory
    fun `should be instantiatable by div`() = testEachOld(
        0 to "::/0",
        63 to "abba:4efa:abba:4efa::/63",
        64 to "abba:4efa:abba:4efa::/64",
        65 to "abba:4efa:abba:4efa:8000::/65",
        128 to "abba:4efa:abba:4efa:abba:4efa:abba:4efa/128",
    ) { (length, representation) ->
        expecting { ip / length } that { toStringIsEqualTo(representation) }
    }
}

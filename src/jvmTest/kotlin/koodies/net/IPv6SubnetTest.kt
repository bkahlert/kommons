package koodies.net

import koodies.collections.to
import koodies.net.IPv6Subnet.Companion.div
import koodies.number.bigIntegerOfDecimalString
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class IPv6SubnetTest {

    private val ip = IPv6Address.parse("abba:4efa:abba:4efa:abba:4efa:abba:4efa")

    @TestFactory
    fun `should have mask`() = testEach(
        0 to "0",
        63 to "fffffffffffffffe0000000000000000",
        64 to "ffffffffffffffff0000000000000000",
        65 to "ffffffffffffffff8000000000000000",
        128 to "ffffffffffffffffffffffffffffffff",
    ) { (length, mask) ->
        with { IPv6Subnet.from(ip, length) }.then {
            expect { this.mask.toString(16) }.that { isEqualTo(mask) }
        }
    }

    @TestFactory
    fun `should have start matching network address`() = testEach(
        0 to "::",
        63 to "abba:4efa:abba:4efa::",
        64 to "abba:4efa:abba:4efa::",
        65 to "abba:4efa:abba:4efa:8000::",
        128 to "abba:4efa:abba:4efa:abba:4efa:abba:4efa",
    ) { (length, networkAddress) ->
        with { IPv6Subnet.from(ip, length) }.then {
            expect { this.start }.that { toStringIsEqualTo(networkAddress) }
            expect { this.networkAddress }.that { toStringIsEqualTo(networkAddress) }
        }
    }

    @TestFactory
    fun `should have inclusive end matching broadcast address`() = testEach(
        0 to "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
        63 to "abba:4efa:abba:4efb:ffff:ffff:ffff:ffff",
        64 to "abba:4efa:abba:4efa:ffff:ffff:ffff:ffff",
        65 to "abba:4efa:abba:4efa:ffff:ffff:ffff:ffff",
        128 to "abba:4efa:abba:4efa:abba:4efa:abba:4efa",
    ) { (length, broadcastAddress) ->
        with { IPv6Subnet.from(ip, length) }.then {
            expect { this.endInclusive }.that { toStringIsEqualTo(broadcastAddress) }
            expect { this.broadcastAddress }.that { toStringIsEqualTo(broadcastAddress) }
        }
    }

    @TestFactory
    fun `should have usable host count`() = testEach(
        0 to "340282366920938463463374607431768211456" to "340282366920938463463374607431768211454",
        63 to "36893488147419103232" to "36893488147419103230",
        64 to "18446744073709551616" to "18446744073709551614",
        65 to "9223372036854775808" to "9223372036854775806",
        128 to "1" to "1",
    ) { (length, hostCountString, usableHostCountString) ->
        val hostCount = bigIntegerOfDecimalString(hostCountString)
        val usableHostCount = bigIntegerOfDecimalString(usableHostCountString)
        with { IPv6Subnet.from(ip, length) }.then {
            expect { this.hostCount }.that { isEqualTo(hostCount) }
            expect { this.usableHostCount }.that { isEqualTo(usableHostCount) }
        }
    }

    @TestFactory
    fun `should have first and usable host`() = testEach(
        0 to "::1" to "ffff:ffff:ffff:ffff:ffff:ffff:ffff:fffe",
        63 to "abba:4efa:abba:4efa:::1" to "abba:4efa:abba:4efb:ffff:ffff:ffff:fffe",
        64 to "abba:4efa:abba:4efa:::1" to "abba:4efa:abba:4efa:ffff:ffff:ffff:fffe",
        65 to "abba:4efa:abba:4efa:8000:::1" to "abba:4efa:abba:4efa:ffff:ffff:ffff:fffe",
        128 to "abba:4efa:abba:4efa:abba:4efa:abba:4efa" to "abba:4efa:abba:4efa:abba:4efa:abba:4efa",
    ) { (length, firstUsableHost, lastUsableHost) ->
        with { IPv6Subnet.from(ip, length) }.then {
            expect { this.firstUsableHost }.that { toStringIsEqualTo(firstUsableHost) }
            expect { this.lastUsableHost }.that { toStringIsEqualTo(lastUsableHost) }
        }
    }

    @TestFactory
    fun `should be representable`() = testEach(
        0 to "::/0",
        63 to "abba:4efa:abba:4efa::/63",
        64 to "abba:4efa:abba:4efa::/64",
        65 to "abba:4efa:abba:4efa:8000::/65",
        128 to "abba:4efa:abba:4efa:abba:4efa:abba:4efa/128",
    ) { (length, representation) ->
        expect { IPv6Subnet.from(ip, length) }.that { toStringIsEqualTo(representation) }
    }

    @TestFactory
    fun `should be instantiatable by div`() = testEach(
        0 to "::/0",
        63 to "abba:4efa:abba:4efa::/63",
        64 to "abba:4efa:abba:4efa::/64",
        65 to "abba:4efa:abba:4efa:8000::/65",
        128 to "abba:4efa:abba:4efa:abba:4efa:abba:4efa/128",
    ) { (length, representation) ->
        expect { ip / length }.that { toStringIsEqualTo(representation) }
    }
}

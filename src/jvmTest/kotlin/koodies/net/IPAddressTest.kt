package koodies.net

import koodies.test.isFailure
import koodies.test.testEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class IPAddressTest {

    @Nested
    inner class Parse {

        @TestFactory
        fun `should parse IPv4 address`() =
            listOf(
                "192.168.16.1",
                "192.168.016.1",
                "192.168.016.001",
            ).testEach { ip ->
                expect { ip.toAnyIp() }.that { isEqualTo(IPv4Address.parse("192.168.16.1")) }
            }

        @TestFactory
        fun `should parse IPv6 address`() =
            listOf(
                "::ffff:c0a8:1001",
                "0:0::ffff:192.168.016.001",
                "0:0:0:0:0:ffff:c0a8:1001",
            ).testEach { ip ->
                expect { ip.toAnyIp() }.that { isEqualTo(IPv6Address.parse("::ffff:c0a8:1001")) }
            }

        @TestFactory
        fun `should throw on invalid IP address`() =
            listOf(
                { "-1.168.16.1".toAnyIp() },
                { IPv4Address.parse("192.168.16.x") },
                { "-0:0:0::ffff:c0a8:1001".toAnyIp() },
                { IPv6Address.parse("0:0:0::xxxx:c0a8:1001") },
            ).testEach { ip ->
                expectThrowing { ip() }.that { isFailure<IllegalArgumentException>() }
            }
    }
}

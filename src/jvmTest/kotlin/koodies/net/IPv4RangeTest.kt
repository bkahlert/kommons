package koodies.net

import koodies.test.testEach
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class IPv4RangeTest {

    private val ip1 = IPv4Address.parse("172.186.78.250")
    private val ip2 = IPv4Address.parse("192.168.0.1")
    private val range = ip1..ip2

    @TestFactory
    fun `should be parsable`() = testEach(
        "172.186.78.250..192.168.0.1",
        "172.186.78.250 ..192.168.0.1",
        "172.186.78.250.. 192.168.0.1",
        " 172.186.78.250..192.168.0.1",
        "172.186.78.250..192.168.0.1 ",
    ) { rangeString: String ->
        expect { rangeString.toIPv4Range() }.that { range }
        expect { ip4RangeOf(rangeString) }.that { range }
    }

    @TestFactory
    fun `should have start`() = testEach(
        range,
        IPv4Range.from(ip1, ip2),
    ) {
        expect { start }.that { isEqualTo(ip1) }
    }

    @TestFactory
    fun `should have inclusive end`() = testEach(
        range,
        IPv4Range.from(ip1, ip2),
    ) {
        expect { endInclusive }.that { isEqualTo(ip2) }
    }

    @TestFactory
    fun `should be representable`() = testEach(
        range,
        IPv4Range.from(ip1, ip2),
    ) {
        expect { toString() }.that { isEqualTo("172.186.78.250..192.168.0.1") }
    }
}


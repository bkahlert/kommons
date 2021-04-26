package koodies.net

import koodies.test.testEach
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class IPSubnetTest {

    @TestFactory
    fun `should be parsable`() = testEach(
        "0.0.0.0/0" to "0.0.0.0..255.255.255.255",
        "172.186.0.0/15" to "172.186.0.0..172.187.255.255",
        "172.186.0.0/16" to "172.186.0.0..172.186.255.255",
        "172.186.0.0/17" to "172.186.0.0..172.186.127.255",
        "172.186.78.250/32" to "172.186.78.250..172.186.78.250",
        "::/0" to "::..ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
        "abba:4efa:abba:4efa::/63" to "abba:4efa:abba:4efa::..abba:4efa:abba:4efb:ffff:ffff:ffff:ffff",
        "abba:4efa:abba:4efa::/64" to "abba:4efa:abba:4efa::..abba:4efa:abba:4efa:ffff:ffff:ffff:ffff",
        "abba:4efa:abba:4efa:8000::/65" to "abba:4efa:abba:4efa:8000::..abba:4efa:abba:4efa:ffff:ffff:ffff:ffff",
        "abba:4efa:abba:4efa:abba:4efa:abba:4efa/128" to "abba:4efa:abba:4efa:abba:4efa:abba:4efa..abba:4efa:abba:4efa:abba:4efa:abba:4efa",
    ) { (subnetString, rangeString) ->
        expecting { subnetString.toIPSubnet() } that { isEqualTo(rangeString.toIPSubnet()) }
        expecting { ipSubnetOf(subnetString) } that { isEqualTo(ipSubnetOf(rangeString)) }
    }
}

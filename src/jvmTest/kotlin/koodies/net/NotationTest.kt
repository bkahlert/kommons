package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger
import koodies.number.bigIntegerOfDecimalString
import koodies.test.testEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class NotationTest {

    @Nested
    inner class IPv4Notation {

        @TestFactory
        fun `should format as conventional representation`() = testEach(
            IPv4Address.parse("192.168.16.1") to "192.168.16.1",
            IPv4Address.LOOPBACK to "127.0.0.1",
            IPv4Address.RANGE.start to "0.0.0.0",
            IPv4Address.RANGE.endInclusive to "255.255.255.255",
        ) { (ip, conventionalRepresentation) ->
            expect { ip.conventionalRepresentation }.that { isEqualTo(conventionalRepresentation) }
        }
    }


    @Nested
    inner class IPv6Notation {

        @TestFactory
        fun `should format as full representation`() = testEach(
            IPv6Address.parse("::ffff:c0a8:1001") to "0000:0000:0000:0000:0000:ffff:c0a8:1001",
            IPv6Address.LOOPBACK to "0000:0000:0000:0000:0000:0000:0000:0001",
            IPv6Address.RANGE.start to "0000:0000:0000:0000:0000:0000:0000:0000",
            IPv6Address.RANGE.endInclusive to "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
        ) { (ip, fullRepresentation) ->
            expect { ip.fullRepresentation }.that { isEqualTo(fullRepresentation) }
        }

        @TestFactory
        fun `should format as conventional representation`() = testEach(
            IPv6Address.parse("::ffff:c0a8:1001") to "0:0:0:0:0:ffff:c0a8:1001",
            IPv6Address.LOOPBACK to "0:0:0:0:0:0:0:1",
            IPv6Address.RANGE.start to "0:0:0:0:0:0:0:0",
            IPv6Address.RANGE.endInclusive to "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
        ) { (ip, conventionalRepresentation) ->
            expect { ip.conventionalRepresentation }.that { isEqualTo(conventionalRepresentation) }
        }

        @TestFactory
        fun `should format as compressed representation`() = testEach(
            IPv6Address.parse("::ffff:c0a8:1001") to "::ffff:c0a8:1001",
            IPv6Address.LOOPBACK to "::1",
            IPv6Address.RANGE.start to "::",
            IPv6Address.RANGE.endInclusive to "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
        ) { (ip, compressedRepresentation) ->
            expect { ip.compressedRepresentation }.that { isEqualTo(compressedRepresentation) }
        }
    }

    @Nested
    inner class UnconventionalNotation {

        /**
         * Notation of not existing IP addresses of 24 bytes length.
         */
        private val unconventionalNotation = object : Notation {
            override val byteCount = 24
            override val groupSize = 3
            override val groupSeparator = '⌁'
            override val base = 32
            override val defaultVerbosity: Notation.Verbosity = Notation.Verbosity.Compressed
        }

        @TestFactory
        fun `should format as conventional representation`() = testEach(
            bigIntegerOfDecimalString("3232239617") to "⌁⌁60⌁ag401",
            BigInteger.ONE to "⌁⌁1",
            BigInteger.ZERO to "⌁⌁",
            (BigInteger.TWO shl (192 - 1)).dec() to "fvvvv⌁fvvvv⌁fvvvv⌁fvvvv⌁fvvvv⌁fvvvv⌁fvvvv⌁fvvvv"
        ) { (value, conventionalRepresentation) ->
            expect { unconventionalNotation.format(value) }.that { isEqualTo(conventionalRepresentation) }
        }
    }
}

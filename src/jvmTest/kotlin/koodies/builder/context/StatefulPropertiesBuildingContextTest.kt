package koodies.builder.context

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import koodies.builder.Builder
import koodies.builder.Init
import koodies.builder.ListBuilder
import koodies.builder.providing
import koodies.concurrent.process.CommandLine
import koodies.net.IPv4Address
import koodies.net.IPv6Address
import koodies.net.div
import koodies.net.ip4Of
import koodies.net.ip6Of
import koodies.ranges.map
import koodies.test.testEach
import koodies.unit.Size
import koodies.unit.bytes
import koodies.unit.deca
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

class HigherOrderBuilderTest {

    private data class BuiltObject(
        val nullableList: List<BigDecimal>?,
        val listOfPairs: List<Pair<IPv4Address, IPv6Address>>,
        val nullableString: String?,
        val size: Size,
        val nullableCommandLine: CommandLine?,
        val number: Int,
    )

    private class BuiltBuilder : StatusfulPropertiesBuildingContext<BuiltBuilder, BuiltObject>({
        BuiltObject(
            ::nullableList.accessValue() ?: emptyList(),
            ::listOfPairs.requireValue(),
            ::nullableString.accessValue() ?: "╳",
            ::size.requireValue(),
            ::nullableCommandLine.accessValue(),
            ::number.requireValue(),
        )
    }) {

        val nullableList by building { ListBuilder<BigDecimal>() }
        val listOfPairs by building { ListBuilder<Pair<IPv4Address, IPv6Address>>() }

        val nullableString by providing<String>()
        val size by providing<Size>()

        val nullableCommandLine by storing<CommandLine>()
        val number by storing<Int>()

        companion object {
            fun build(init: Init<BuiltBuilder>): BuiltObject {
                return Builder.build(init) { BuiltBuilder() }
            }
        }
    }

    @Test
    fun `should build with all fields set`() {
        val built = BuiltBuilder.build {
            nullableList { (BigDecimal.ONE..BigDecimal.TEN).map { plus(2) }.apply { +start; +endInclusive } }
            listOfPairs { +(ip4Of(42) to (ip6Of("ff::42") / 123).broadcastAddress) }
            nullableString { "𓌈🥸𓂈" }
            size { 21.deca.bytes }
            nullableCommandLine(CommandLine("echo", "Hello World"))
            number(-1)
        }
        expectThat(built).isEqualTo(BuiltObject(
            nullableList = listOf(BigDecimal.parseString("3"), BigDecimal.parseString("12")),
            listOfPairs = listOf(ip4Of(42) to (ip6Of("ff::42") / 123).broadcastAddress),
            nullableString = "𓌈🥸𓂈",
            size = 21.deca.bytes,
            nullableCommandLine = CommandLine("echo", "Hello World"),
            number = -1,
        ))
    }

    @Test
    fun `should build with mandatory only`() {
        val built = BuiltBuilder.build {
            listOfPairs { +(ip4Of(42) to (ip6Of("ff::42") / 123).broadcastAddress) }
            size { 21.deca.bytes }
            number(-1)
        }
        expectThat(built).isEqualTo(BuiltObject(
            nullableList = emptyList(),
            listOfPairs = listOf(ip4Of(42) to (ip6Of("ff::42") / 123).broadcastAddress),
            nullableString = "╳",
            size = 21.deca.bytes,
            nullableCommandLine = null,
            number = -1
        ))
    }

    @TestFactory
    fun `shoudld throw on missing value`() = testEach<Init<BuiltBuilder>>(
        {
            nullableList { (BigDecimal.ONE..BigDecimal.TEN).map { plus(2) } }
            nullableString { "𓌈🥸𓂈" }
            size { 21.deca.bytes }
            nullableCommandLine(CommandLine("echo", "Hello World"))
            number(-1)
        },
        {
            nullableList { (BigDecimal.ONE..BigDecimal.TEN).map { plus(2) } }
            listOfPairs { +(ip4Of(42) to (ip6Of("ff::42") / 123).broadcastAddress) }
            nullableString { "𓌈🥸𓂈" }
            nullableCommandLine(CommandLine("echo", "Hello World"))
            number(-1)
        },
        {
            nullableList { (BigDecimal.ONE..BigDecimal.TEN).map { plus(2) } }
            listOfPairs { +(ip4Of(42) to (ip6Of("ff::42") / 123).broadcastAddress) }
            nullableString { "𓌈🥸𓂈" }
            size { 21.deca.bytes }
            nullableCommandLine(CommandLine("echo", "Hello World"))
        },
    ) {
        expectThrowing { BuiltBuilder.build { } }.that { isFailure().isA<IllegalStateException>() }
    }
}

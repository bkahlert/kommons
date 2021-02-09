package koodies.builder

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import koodies.builder.context.ElementAddingContext
import koodies.builder.context.PropertiesBuildingContext
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

class DynamicContextBuilderTest {

    private data class CustomObject(
        val nullableList: List<BigDecimal>?,
        val listOfPairs: List<Pair<IPv4Address, IPv6Address>>,

        val nullableString: String?,
        val size: Size,

        val nullableCommandLine: CommandLine?,
        val number: Int,
    )

    private interface CustomContext {
        val nullableList: (Init<ElementAddingContext<BigDecimal>, Unit>) -> Unit
        val listOfPairs: (Init<ElementAddingContext<Pair<IPv4Address, IPv6Address>>, Unit>) -> Unit

        val nullableString: (() -> String) -> Unit
        val size: (() -> Size) -> Unit

        val nullableCommandLine: (CommandLine) -> Unit
        val number: (Int) -> Unit
    }

    private object CustomBuilder : DynamicContextBuilder<CustomContext, CustomObject>({
        CustomObject(
            CustomContext::nullableList.getOrNull() ?: emptyList(),
            CustomContext::listOfPairs.require(),
            CustomContext::nullableString.getOrNull() ?: "╳",
            CustomContext::size.require(),
            CustomContext::nullableCommandLine.getOrNull(),
            CustomContext::number.require(),
        )
    }, {
        object : CustomContext {
            override val nullableList by building { ListBuilder<BigDecimal>() }
            override val listOfPairs by building { ListBuilder<Pair<IPv4Address, IPv6Address>>() }

            override val nullableString by providing<String>()
            override val size by providing<Size>()

            override val nullableCommandLine by storing<CommandLine>()
            override val number by storing<Int>()
        }
    })

    @Test
    fun `should build with all fields set`() {
        val built = CustomBuilder.build {
            nullableList { (BigDecimal.ONE..BigDecimal.TEN).map { plus(2) }.apply { +start; +endInclusive } }
            listOfPairs { +(ip4Of(42) to (ip6Of("ff::42") / 123).broadcastAddress) }
            nullableString { "𓌈🥸𓂈" }
            size { 21.deca.bytes }
            nullableCommandLine(CommandLine("echo", "Hello World"))
            number(-1)
        }
        expectThat(built).isEqualTo(CustomObject(
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
        val built = CustomBuilder.build {
            listOfPairs { +(ip4Of(42) to (ip6Of("ff::42") / 123).broadcastAddress) }
            size { 21.deca.bytes }
            number(-1)
        }
        expectThat(built).isEqualTo(CustomObject(
            nullableList = emptyList(),
            listOfPairs = listOf(ip4Of(42) to (ip6Of("ff::42") / 123).broadcastAddress),
            nullableString = "╳",
            size = 21.deca.bytes,
            nullableCommandLine = null,
            number = -1
        ))
    }

    @TestFactory
    fun `should throw on missing value`() = testEach<Init<CustomContext, Unit>>(
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
    ) { init ->
        expectThrowing { CustomBuilder.build(init) }.that { isFailure().isA<IllegalStateException>() }
    }

    interface XXX {
        val x: String
        val nullableList: (Init<ElementAddingContext<String>, Unit>) -> Unit
//        fun nullableList(init:Init<ElementAddingContext<String>,Unit>):Unit
//        val nullableList2: ElementAddingContext<String>.() -> Unit
    }


    class YYY : XXX, PropertiesBuildingContext<YYY> {
        override val x: String = "an x"
        override val nullableList by building { ListBuilder<String>() }
    }
//
//    class ZZZ : StatefulPropertiesBuildingContext<YYY>() {
//        override val context = object : XXX, PropertiesBuildingContext<XXX> {
//            override val x: String
//                get() = TODO("Not yet implemented")
//            override val nullableList: (Init<ElementAddingContext<String>, Unit>) -> Unit
//                get() = TODO("Not yet implemented")
//
//        }
//        override val x: String = "an x"
//        override val nullableList by building { ListBuilder<String>() }
//    }
}

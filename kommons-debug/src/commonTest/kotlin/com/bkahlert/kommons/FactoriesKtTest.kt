package com.bkahlert.kommons

import com.bkahlert.kommons.Converter.Companion.converter
import com.bkahlert.kommons.Converter.ConversionException
import com.bkahlert.kommons.Converter.Converter1
import com.bkahlert.kommons.Converter.Converter2
import com.bkahlert.kommons.Converter.Converter3
import com.bkahlert.kommons.Creator.Companion.creator
import com.bkahlert.kommons.Creator.CreationException
import com.bkahlert.kommons.Creator.Creator1
import com.bkahlert.kommons.Creator.Creator2
import com.bkahlert.kommons.Creator.Creator3
import com.bkahlert.kommons.Parser.Companion.parser
import com.bkahlert.kommons.Parser.ParsingException
import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.text.cs
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class FactoriesKtTest {

    @Test fun creation_exception() = testAll {
        CreationException(Int::class, "value1", "value2") should {
            it.message shouldBe "Failed to create an instance of Int of \"value1\", \"value2\""
            it.cause shouldBe null
        }

        CreationException(Int::class, RuntimeException(), "value1", "value2") should {
            it.message shouldBe "Failed to create an instance of Int of \"value1\", \"value2\""
            it.cause.shouldBeInstanceOf<RuntimeException>()
        }

        CreationException("custom message") should {
            it.message shouldBe "custom message"
            it.cause shouldBe null
        }

        CreationException("custom message", RuntimeException()) should {
            it.message shouldBe "custom message"
            it.cause.shouldBeInstanceOf<RuntimeException>()
        }
    }

    @Test fun of_or_null() = testAll {
        intCreator1.ofOrNull("1") shouldBe 1
        intCreator1.ofOrNull("-1-") shouldBe null
        throwingCreator1.ofOrNull("1") shouldBe 1
        throwingCreator1.ofOrNull("-1-") shouldBe null

        intCreator2.ofOrNull("1", 2) shouldBe 3
        intCreator2.ofOrNull("-1-", 2) shouldBe null
        throwingCreator2.ofOrNull("1", 2) shouldBe 3
        throwingCreator2.ofOrNull("-1-", 2) shouldBe null

        intCreator3.ofOrNull("1", 2, 3) shouldBe 6
        intCreator3.ofOrNull("-1-", 2, 3) shouldBe null
        throwingCreator3.ofOrNull("1", 2, 3) shouldBe 6
        throwingCreator3.ofOrNull("-1-", 2, 3) shouldBe null
    }

    @Test fun of() = testAll {
        intCreator1.of("1") shouldBe 1
        shouldThrow<NumberFormatException> { intCreator1.of("-1-") }
        throwingCreator1.of("1") shouldBe 1
        shouldThrow<CreationException> { throwingCreator1.of("-1-") }.cause shouldBe null

        intCreator2.of("1", 2) shouldBe 3
        shouldThrow<NumberFormatException> { intCreator2.of("-1-", 2) }
        throwingCreator2.of("1", 2) shouldBe 3
        shouldThrow<CreationException> { throwingCreator2.of("-1-", 2) }.cause shouldBe null

        intCreator3.of("1", 2, 3) shouldBe 6
        shouldThrow<NumberFormatException> { intCreator3.of("-1-", 2, 3) }
        throwingCreator3.of("1", 2, 3) shouldBe 6
        shouldThrow<CreationException> { throwingCreator3.of("-1-", 2, 3) }.cause shouldBe null
    }

    @Test fun creator() = testAll {
        creator(intCreator1::of) should { creator ->
            creator.ofOrNull("1") shouldBe 1
            creator.ofOrNull("-1-") shouldBe null
            creator.of("1") shouldBe 1
            shouldThrow<CreationException> { creator.of("-1-") }.cause.shouldBeInstanceOf<NumberFormatException>()
        }
        creator(throwingCreator1::of) should { creator ->
            creator.ofOrNull("1") shouldBe 1
            creator.ofOrNull("-1-")
            creator.of("1") shouldBe 1
            shouldThrow<CreationException> { creator.of("-1-") }.cause shouldBe null
        }
        creator<String, Int> { it.toIntOrNull() } should { creator ->
            creator.ofOrNull("1") shouldBe 1
            creator.ofOrNull("-1-")
            creator.of("1") shouldBe 1
            shouldThrow<CreationException> { creator.of("-1-") }.cause shouldBe null
        }

        creator(intCreator2::of) should { creator ->
            creator.ofOrNull("1", 2) shouldBe 3
            creator.ofOrNull("-1-", 2) shouldBe null
            creator.of("1", 2) shouldBe 3
            shouldThrow<CreationException> { creator.of("-1-", 2) }.cause.shouldBeInstanceOf<NumberFormatException>()
        }
        creator(throwingCreator2::of) should { creator ->
            creator.ofOrNull("1", 2) shouldBe 3
            creator.ofOrNull("-1-", 2) shouldBe null
            creator.of("1", 2) shouldBe 3
            shouldThrow<CreationException> { creator.of("-1-", 2) }.cause shouldBe null
        }
        creator<String, Int, Int> { p1, p2 -> p1.toIntOrNull()?.let { it + p2 } } should { creator ->
            creator.ofOrNull("1", 2) shouldBe 3
            creator.ofOrNull("-1-", 2) shouldBe null
            creator.of("1", 2) shouldBe 3
            shouldThrow<CreationException> { creator.of("-1-", 2) }.cause shouldBe null
        }

        creator(intCreator3::of) should { creator ->
            creator.ofOrNull("1", 2, 3) shouldBe 6
            creator.ofOrNull("-1-", 2, 3) shouldBe null
            creator.of("1", 2, 3) shouldBe 6
            shouldThrow<CreationException> { creator.of("-1-", 2, 3) }.cause.shouldBeInstanceOf<NumberFormatException>()
        }
        creator(throwingCreator3::of) should { creator ->
            creator.ofOrNull("1", 2, 3) shouldBe 6
            creator.ofOrNull("-1-", 2, 3) shouldBe null
            creator.of("1", 2, 3) shouldBe 6
            shouldThrow<CreationException> { creator.of("-1-", 2, 3) }.cause shouldBe null
        }
        creator<String, Int, Int, Int> { p1, p2, p3 -> p1.toIntOrNull()?.let { it + p2 + p3 } } should { creator ->
            creator.ofOrNull("1", 2, 3) shouldBe 6
            creator.ofOrNull("-1-", 2, 3) shouldBe null
            creator.of("1", 2, 3) shouldBe 6
            shouldThrow<CreationException> { creator.of("-1-", 2, 3) }.cause shouldBe null
        }
    }

    @Test fun conversion_exception() = testAll {
        ConversionException(Int::class, "value1", "value2") should {
            it.message shouldBe "Failed to convert \"value1\", \"value2\" to an instance of Int"
            it.cause shouldBe null
        }

        ConversionException(Int::class, RuntimeException(), "value1", "value2") should {
            it.message shouldBe "Failed to convert \"value1\", \"value2\" to an instance of Int"
            it.cause.shouldBeInstanceOf<RuntimeException>()
        }

        ConversionException("custom message") should {
            it.message shouldBe "custom message"
            it.cause shouldBe null
        }

        ConversionException("custom message", RuntimeException()) should {
            it.message shouldBe "custom message"
            it.cause.shouldBeInstanceOf<RuntimeException>()
        }
    }

    @Test fun from_or_null() = testAll {
        intConverter1.fromOrNull("1") shouldBe 1
        intConverter1.fromOrNull("-1-") shouldBe null
        throwingConverter1.fromOrNull("1") shouldBe 1
        throwingConverter1.fromOrNull("-1-") shouldBe null

        intConverter2.fromOrNull("1", 2) shouldBe 3
        intConverter2.fromOrNull("-1-", 2) shouldBe null
        throwingConverter2.fromOrNull("1", 2) shouldBe 3
        throwingConverter2.fromOrNull("-1-", 2) shouldBe null

        intConverter3.fromOrNull("1", 2, 3) shouldBe 6
        intConverter3.fromOrNull("-1-", 2, 3) shouldBe null
        throwingConverter3.fromOrNull("1", 2, 3) shouldBe 6
        throwingConverter3.fromOrNull("-1-", 2, 3) shouldBe null
    }

    @Test fun from() = testAll {
        intConverter1.from("1") shouldBe 1
        shouldThrow<NumberFormatException> { intConverter1.from("-1-") }
        throwingConverter1.from("1") shouldBe 1
        shouldThrow<ConversionException> { throwingConverter1.from("-1-") }.cause shouldBe null

        intConverter2.from("1", 2) shouldBe 3
        shouldThrow<NumberFormatException> { intConverter2.from("-1-", 2) }
        throwingConverter2.from("1", 2) shouldBe 3
        shouldThrow<ConversionException> { throwingConverter2.from("-1-", 2) }.cause shouldBe null

        intConverter3.from("1", 2, 3) shouldBe 6
        shouldThrow<NumberFormatException> { intConverter3.from("-1-", 2, 3) }
        throwingConverter3.from("1", 2, 3) shouldBe 6
        shouldThrow<ConversionException> { throwingConverter3.from("-1-", 2, 3) }.cause shouldBe null
    }

    @Test fun converter() = testAll {
        converter(intConverter1::from) should { converter ->
            converter.fromOrNull("1") shouldBe 1
            converter.fromOrNull("-1-") shouldBe null
            converter.from("1") shouldBe 1
            shouldThrow<ConversionException> { converter.from("-1-") }.cause.shouldBeInstanceOf<NumberFormatException>()
        }
        converter(throwingConverter1::from) should { converter ->
            converter.fromOrNull("1") shouldBe 1
            converter.fromOrNull("-1-")
            converter.from("1") shouldBe 1
            shouldThrow<ConversionException> { converter.from("-1-") }.cause shouldBe null
        }
        converter<String, Int> { it.toIntOrNull() } should { converter ->
            converter.fromOrNull("1") shouldBe 1
            converter.fromOrNull("-1-")
            converter.from("1") shouldBe 1
            shouldThrow<ConversionException> { converter.from("-1-") }.cause shouldBe null
        }

        converter(intConverter2::from) should { converter ->
            converter.fromOrNull("1", 2) shouldBe 3
            converter.fromOrNull("-1-", 2) shouldBe null
            converter.from("1", 2) shouldBe 3
            shouldThrow<ConversionException> { converter.from("-1-", 2) }.cause.shouldBeInstanceOf<NumberFormatException>()
        }
        converter(throwingConverter2::from) should { converter ->
            converter.fromOrNull("1", 2) shouldBe 3
            converter.fromOrNull("-1-", 2) shouldBe null
            converter.from("1", 2) shouldBe 3
            shouldThrow<ConversionException> { converter.from("-1-", 2) }.cause shouldBe null
        }
        converter<String, Int, Int> { p1, p2 -> p1.toIntOrNull()?.let { it + p2 } } should { converter ->
            converter.fromOrNull("1", 2) shouldBe 3
            converter.fromOrNull("-1-", 2) shouldBe null
            converter.from("1", 2) shouldBe 3
            shouldThrow<ConversionException> { converter.from("-1-", 2) }.cause shouldBe null
        }

        converter(intConverter3::from) should { converter ->
            converter.fromOrNull("1", 2, 3) shouldBe 6
            converter.fromOrNull("-1-", 2, 3) shouldBe null
            converter.from("1", 2, 3) shouldBe 6
            shouldThrow<ConversionException> { converter.from("-1-", 2, 3) }.cause.shouldBeInstanceOf<NumberFormatException>()
        }
        converter(throwingConverter3::from) should { converter ->
            converter.fromOrNull("1", 2, 3) shouldBe 6
            converter.fromOrNull("-1-", 2, 3) shouldBe null
            converter.from("1", 2, 3) shouldBe 6
            shouldThrow<ConversionException> { converter.from("-1-", 2, 3) }.cause shouldBe null
        }
        converter<String, Int, Int, Int> { p1, p2, p3 -> p1.toIntOrNull()?.let { it + p2 + p3 } } should { converter ->
            converter.fromOrNull("1", 2, 3) shouldBe 6
            converter.fromOrNull("-1-", 2, 3) shouldBe null
            converter.from("1", 2, 3) shouldBe 6
            shouldThrow<ConversionException> { converter.from("-1-", 2, 3) }.cause shouldBe null
        }
    }


    @Test fun parsing_exception() = testAll {
        listOf(
            ParsingException("value".cs, Int::class),
            ParsingException("value", Int::class),
        ).forAll {
            it.message shouldBe "Failed to parse \"value\" into an instance of Int"
            it.cause shouldBe null
        }

        listOf(
            ParsingException("value".cs, Int::class, RuntimeException()),
            ParsingException("value", Int::class, RuntimeException()),
        ).forAll {
            it.message shouldBe "Failed to parse \"value\" into an instance of Int"
            it.cause.shouldBeInstanceOf<RuntimeException>()
        }

        listOf(
            ParsingException("custom message"),
        ).forAll {
            it.message shouldBe "custom message"
            it.cause shouldBe null
        }

        listOf(
            ParsingException("custom message", RuntimeException()),
        ).forAll {
            it.message shouldBe "custom message"
            it.cause.shouldBeInstanceOf<RuntimeException>()
        }
    }

    @Test fun parse_or_null() = testAll {
        intParser.parseOrNull("1".cs) shouldBe 1
        intParser.parseOrNull("-1-".cs) shouldBe null
        intParser.parseOrNull("1") shouldBe 1
        intParser.parseOrNull("-1-") shouldBe null

        throwingParser.parseOrNull("1".cs) shouldBe 1
        throwingParser.parseOrNull("-1-".cs) shouldBe null
        throwingParser.parseOrNull("1") shouldBe 1
        throwingParser.parseOrNull("-1-") shouldBe null
    }

    @Test fun parse() = testAll {
        intParser.parse("1".cs) shouldBe 1
        shouldThrow<NumberFormatException> { intParser.parse("-1-".cs) }
        intParser.parse("1") shouldBe 1
        shouldThrow<NumberFormatException> { intParser.parse("-1-") }

        throwingParser.parse("1".cs) shouldBe 1
        shouldThrow<ParsingException> { throwingParser.parse("-1-".cs) }.cause shouldBe null
        throwingParser.parse("1") shouldBe 1
        shouldThrow<ParsingException> { throwingParser.parse("-1-") }.cause shouldBe null
    }

    @Test fun parser() = testAll {
        parser(intParser::parse) should { parser ->
            parser.parseOrNull("1".cs) shouldBe 1
            parser.parseOrNull("-1-".cs) shouldBe null
            parser.parseOrNull("1") shouldBe 1
            parser.parseOrNull("-1-") shouldBe null

            parser.parse("1".cs) shouldBe 1
            shouldThrow<ParsingException> { parser.parse("-1-".cs) }.cause.shouldBeInstanceOf<NumberFormatException>()
            parser.parse("1") shouldBe 1
            shouldThrow<ParsingException> { parser.parse("-1-") }.cause.shouldBeInstanceOf<NumberFormatException>()
        }

        parser(throwingParser::parse) should { parser ->
            parser.parseOrNull("1".cs) shouldBe 1
            parser.parseOrNull("-1-".cs) shouldBe null
            parser.parseOrNull("1") shouldBe 1
            parser.parseOrNull("-1-") shouldBe null

            parser.parse("1".cs) shouldBe 1
            shouldThrow<ParsingException> { parser.parse("-1-".cs) }.cause shouldBe null
            parser.parse("1") shouldBe 1
            shouldThrow<ParsingException> { parser.parse("-1-") }.cause shouldBe null
        }

        parser { it.toString().toIntOrNull() } should { parser ->
            parser.parseOrNull("1".cs) shouldBe 1
            parser.parseOrNull("-1-".cs) shouldBe null
            parser.parseOrNull("1") shouldBe 1
            parser.parseOrNull("-1-") shouldBe null

            parser.parse("1".cs) shouldBe 1
            shouldThrow<ParsingException> { parser.parse("-1-".cs) }.cause shouldBe null
            parser.parse("1") shouldBe 1
            shouldThrow<ParsingException> { parser.parse("-1-") }.cause shouldBe null
        }
    }
}

internal val intCreator1 = object : Creator1<String, Int> {
    override fun of(obj: String): Int = obj.toInt()
}

internal val throwingCreator1 = object : Creator1<String, Int> {
    override fun of(obj: String): Int = obj.toIntOrNull() ?: throw CreationException("error creating of $obj")
}


internal val intCreator2 = object : Creator2<String, Int, Int> {
    override fun of(obj1: String, obj2: Int): Int = obj1.toInt() + obj2
}

internal val throwingCreator2 = object : Creator2<String, Int, Int> {
    override fun of(obj1: String, obj2: Int): Int = obj1.toIntOrNull()?.let { it + obj2 } ?: throw CreationException("error creating of $obj1, $obj2")
}


internal val intCreator3 = object : Creator3<String, Int, Int, Int> {
    override fun of(obj1: String, obj2: Int, obj3: Int): Int = obj1.toInt() + obj2 + obj3
}

internal val throwingCreator3 = object : Creator3<String, Int, Int, Int> {
    override fun of(obj1: String, obj2: Int, obj3: Int): Int =
        obj1.toIntOrNull()?.let { it + obj2 + obj3 } ?: throw CreationException("error creating of $obj1, $obj2, $obj3")
}


internal val intConverter1 = object : Converter1<String, Int> {
    override fun from(obj: String): Int = obj.toInt()
}

internal val throwingConverter1 = object : Converter1<String, Int> {
    override fun from(obj: String): Int = obj.toIntOrNull() ?: throw ConversionException("error converting $obj")
}


internal val intConverter2 = object : Converter2<String, Int, Int> {
    override fun from(obj1: String, obj2: Int): Int = obj1.toInt() + obj2
}

internal val throwingConverter2 = object : Converter2<String, Int, Int> {
    override fun from(obj1: String, obj2: Int): Int = obj1.toIntOrNull()?.let { it + obj2 } ?: throw ConversionException("error converting $obj1, $obj2")
}


internal val intConverter3 = object : Converter3<String, Int, Int, Int> {
    override fun from(obj1: String, obj2: Int, obj3: Int): Int = obj1.toInt() + obj2 + obj3
}

internal val throwingConverter3 = object : Converter3<String, Int, Int, Int> {
    override fun from(obj1: String, obj2: Int, obj3: Int): Int =
        obj1.toIntOrNull()?.let { it + obj2 + obj3 } ?: throw ConversionException("error converting $obj1, $obj2, $obj3")
}


internal val intParser = object : Parser<Int> {
    override fun parse(string: CharSequence): Int = string.toString().toInt()
}

internal val throwingParser = object : Parser<Int> {
    override fun parse(string: CharSequence): Int = string.toString().toIntOrNull() ?: throw ParsingException("error parsing $string")
}

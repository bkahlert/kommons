package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.junit.DynamicTestDisplayNameGenerator.displayNameFor
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.text.LineSeparators
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.nio.file.Paths
import kotlin.collections.Map.Entry
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty
import kotlin.streams.asStream


class DynamicTestDisplayNameGeneratorTest {

    @Suppress("SpellCheckingInspection")
    @Test fun display_name_for() = testAll {
        displayNameFor(nullSubject) shouldBe "𝘯𝘶𝘭𝘭"
        displayNameFor(kPropertySubject) shouldBe "ᴩʀᴏᴩᴇʀᴛy length"
        displayNameFor(kFunctionSubject) shouldBe "ꜰᴜɴᴄᴛɪᴏɴ toString"
        displayNameFor(functionSubject) shouldBe "ꜰᴜɴᴄᴛɪᴏɴ hashCode"
        displayNameFor(tripleSubject) shouldBe "( \"string\", 42, 𝘤𝘭𝘢𝘴𝘴 Any )"
        displayNameFor(pairSubject) shouldBe "( \"string\", 42 )"
        displayNameFor(entrySubject) shouldBe "\"string\" → 42"
        displayNameFor(enumSubject) shouldBe "EnumSubject1"
        displayNameFor(sealedSubject) shouldBe "SealedSubject.SealedSubject1"
        displayNameFor(charSubject) shouldBe "\"c\" LATIN SMALL LETTER C"
        displayNameFor(charStringSubject) shouldBe "\"🫠\" 0x1FAE0"
        displayNameFor(blankStringSubject) shouldBe "\" \" SPACE"
        displayNameFor(emptyStringSubject) shouldBe "\"\""
        displayNameFor(stringSubject) shouldBe "\"string\""
        displayNameFor(anySubject) shouldBe "string representation"
    }

    @Test fun display_name_for__explicit() = testAll {
        displayNameFor(nullSubject, "foo {} bar") shouldBe "foo 𝘯𝘶𝘭𝘭 bar"
        displayNameFor(kPropertySubject, "foo {} bar") shouldBe "foo length bar"
        displayNameFor(kFunctionSubject, "foo {} bar") shouldBe "foo toString bar"
        displayNameFor(functionSubject, "foo {} bar") shouldBe "foo hashCode bar"
        displayNameFor(tripleSubject, "foo {}-{}-{} bar") shouldBe "foo \"string\"-42-𝘤𝘭𝘢𝘴𝘴 Any bar"
        displayNameFor(pairSubject, "foo {}-{} bar") shouldBe "foo \"string\"-42 bar"
        displayNameFor(entrySubject, "foo {}-{} bar") shouldBe "foo \"string\"-42 bar"
        displayNameFor(enumSubject, "foo {} bar") shouldBe "foo EnumSubject1 bar"
        displayNameFor(sealedSubject, "foo {} bar") shouldBe "foo SealedSubject.SealedSubject1 bar"
        displayNameFor(charSubject, "foo {} bar") shouldBe "foo \"c\" bar"
        displayNameFor(charStringSubject, "foo {} bar") shouldBe "foo \"🫠\" bar"
        displayNameFor(blankStringSubject, "foo {} bar") shouldBe "foo \" \" bar"
        displayNameFor(emptyStringSubject, "foo {} bar") shouldBe "foo \"\" bar"
        displayNameFor(stringSubject, "foo {} bar") shouldBe "foo \"string\" bar"
        displayNameFor(anySubject, "foo {} bar") shouldBe "foo string representation bar"
    }

    @TestFactory fun demo() = sequenceOf(
        kPropertySubject,
        kFunctionSubject,
        functionSubject,
        tripleSubject,
        pairSubject,
        entrySubject,
        enumSubject,
        sealedSubject,
        charSubject,
        charStringSubject,
        blankStringSubject,
        emptyStringSubject,
        stringSubject,
        nullSubject,
        anySubject,
    ).map { subject ->
        DynamicTest.dynamicTest(displayNameFor(subject)) {}
    }.asStream()


    @Test fun to_compact_string() = testAll {
        runtimeException.toCompactString() should {
            it shouldMatchGlob "RuntimeException: Something happened at.(DynamicTestDisplayNameGeneratorTest.kt:*)"
            it shouldNotContain "\n"
        }
        emptyException.toCompactString() should {
            it shouldMatchGlob "RuntimeException at.(DynamicTestDisplayNameGeneratorTest.kt:*)"
            it shouldNotContain "\n"
        }

        Result.failure<String>(runtimeException).toCompactString() should {
            it shouldMatchGlob "RuntimeException: Something happened at.(DynamicTestDisplayNameGeneratorTest.kt:*)"
            it shouldNotContain "\n"
        }
        Result.failure<String>(emptyException).toCompactString() should {
            it shouldMatchGlob "RuntimeException at.(DynamicTestDisplayNameGeneratorTest.kt:*)"
            it shouldNotContain "\n"
        }

        Result.success("good").toCompactString() shouldBe "\"good\""
        Result.success(Paths.get("/path")).toCompactString() shouldMatchGlob "file://*/path"
        Result.success(emptyList<Any>()).toCompactString() shouldBe "[]"
        Result.success(arrayOf("a", "b")).toCompactString() shouldBe Result.success(listOf("a", "b")).toCompactString()

        (null as String?).toCompactString() shouldBe "null"
        Unit.toCompactString() shouldBe ""

        SomeClass().toCompactString() shouldBe "SomeClass"
        SomeClass().Foo().toCompactString() shouldBe "SomeClass.Foo"
        SomeClass.NestedClass.Foo().toCompactString() shouldBe "NestedClass.Foo"

        LineSeparators.Unicode.joinToString("") { "line$it" }.toCompactString() shouldBe "\"line⏎line⏎line⏎line⏎line⏎line\""
    }
}

internal enum class EnumSubject {
    EnumSubject1,
    @Suppress("unused") ENUM_SUBJECT_2,
    ;
}

internal sealed class SealedSubject {
    @Suppress("CanSealedSubClassBeObject") class SealedSubject1 : SealedSubject()
    @Suppress("unused", "ClassName") object SEALED_SUBJECT_2 : SealedSubject()
}


internal val nullSubject: Any? = null
internal val kPropertySubject: KProperty<*> = String::length
internal val kFunctionSubject: KFunction1<*, *> = Any::toString
internal val functionSubject: Function<*> = Any::hashCode
internal val tripleSubject: Triple<*, *, *> = Triple("string", 42, Any::class)
internal val pairSubject: Pair<*, *> = Pair("string", 42)
internal val entrySubject: Entry<*, *> = mapOf(pairSubject).entries.first()
internal val enumSubject: EnumSubject = EnumSubject.EnumSubject1
internal val sealedSubject: SealedSubject = SealedSubject.SealedSubject1()
internal const val charSubject: Char = 'c'
internal const val charStringSubject: String = "🫠"
internal const val blankStringSubject: String = " "
internal const val emptyStringSubject: String = ""
internal const val stringSubject: String = "string"
internal val anySubject: Any = object {
    override fun toString(): String {
        return "string representation"
    }
}


internal val emptyException = RuntimeException()
internal val runtimeException = RuntimeException(
    "Something happened\n" +
        " ➜ A dump has been written to:\n" +
        "   - file:///var/folders/…/file.log (unchanged)\n" +
        "   - file:///var/folders/…/file.ansi-removed.log (ANSI escape/control sequences removed)\n" +
        " ➜ The last lines are:\n" +
        "    raspberry\n" +
        "    Login incorrect\n" +
        "    raspberrypi login:"
)

internal class SomeClass {
    inner class Foo
    sealed class NestedClass {
        class Foo : NestedClass()
    }
}

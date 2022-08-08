package com.bkahlert.kommons.text

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test

class CasesKtTest {

    @Test
    fun is_lower_case() = testAll {
        'a'.isLowerCase() shouldBe true
        'A'.isLowerCase() shouldBe false
        '9'.isLowerCase() shouldBe false
        '_'.isLowerCase() shouldBe false
    }

    @Test
    fun is_upper_case() = testAll {
        'a'.isUpperCase() shouldBe false
        'A'.isUpperCase() shouldBe true
        '9'.isUpperCase() shouldBe false
        '_'.isUpperCase() shouldBe false
    }

    @Test
    fun capitalize_char_sequence() = testAll {
        charSequence_aa.capitalize() shouldBe "Aa"
        charSequence_aA.capitalize() shouldBe "AA"
        charSequence_a9.capitalize() shouldBe "A9"
        charSequence_a_.capitalize() shouldBe "A_"
        charSequence_Aa.capitalize() shouldBeSameInstanceAs charSequence_Aa
        charSequence_AA.capitalize() shouldBeSameInstanceAs charSequence_AA
        charSequence_A9.capitalize() shouldBeSameInstanceAs charSequence_A9
        charSequence_A_.capitalize() shouldBeSameInstanceAs charSequence_A_
        charSequence_9a.capitalize() shouldBeSameInstanceAs charSequence_9a
        charSequence_9A.capitalize() shouldBeSameInstanceAs charSequence_9A
        charSequence_99.capitalize() shouldBeSameInstanceAs charSequence_99
        charSequence_9_.capitalize() shouldBeSameInstanceAs charSequence_9_
        charSequence__a.capitalize() shouldBeSameInstanceAs charSequence__a
        charSequence__A.capitalize() shouldBeSameInstanceAs charSequence__A
        charSequence__9.capitalize() shouldBeSameInstanceAs charSequence__9
        charSequence___.capitalize() shouldBeSameInstanceAs charSequence___
    }

    @Test
    fun capitalize_string() = testAll {
        string_aa.capitalize() shouldBe "Aa"
        string_aA.capitalize() shouldBe "AA"
        string_a9.capitalize() shouldBe "A9"
        string_a_.capitalize() shouldBe "A_"
        string_Aa.capitalize() shouldBeSameInstanceAs string_Aa
        string_AA.capitalize() shouldBeSameInstanceAs string_AA
        string_A9.capitalize() shouldBeSameInstanceAs string_A9
        string_A_.capitalize() shouldBeSameInstanceAs string_A_
        string_9a.capitalize() shouldBeSameInstanceAs string_9a
        string_9A.capitalize() shouldBeSameInstanceAs string_9A
        string_99.capitalize() shouldBeSameInstanceAs string_99
        string_9_.capitalize() shouldBeSameInstanceAs string_9_
        string__a.capitalize() shouldBeSameInstanceAs string__a
        string__A.capitalize() shouldBeSameInstanceAs string__A
        string__9.capitalize() shouldBeSameInstanceAs string__9
        string___.capitalize() shouldBeSameInstanceAs string___
    }

    @Test
    fun decapitalize_char_sequence() = testAll {
        charSequence_aa.decapitalize() shouldBeSameInstanceAs charSequence_aa
        charSequence_aA.decapitalize() shouldBeSameInstanceAs charSequence_aA
        charSequence_a9.decapitalize() shouldBeSameInstanceAs charSequence_a9
        charSequence_a_.decapitalize() shouldBeSameInstanceAs charSequence_a_
        charSequence_Aa.decapitalize() shouldBe "aa"
        charSequence_AA.decapitalize() shouldBe "aA"
        charSequence_A9.decapitalize() shouldBe "a9"
        charSequence_A_.decapitalize() shouldBe "a_"
        charSequence_9a.decapitalize() shouldBeSameInstanceAs charSequence_9a
        charSequence_9A.decapitalize() shouldBeSameInstanceAs charSequence_9A
        charSequence_99.decapitalize() shouldBeSameInstanceAs charSequence_99
        charSequence_9_.decapitalize() shouldBeSameInstanceAs charSequence_9_
        charSequence__a.decapitalize() shouldBeSameInstanceAs charSequence__a
        charSequence__A.decapitalize() shouldBeSameInstanceAs charSequence__A
        charSequence__9.decapitalize() shouldBeSameInstanceAs charSequence__9
        charSequence___.decapitalize() shouldBeSameInstanceAs charSequence___
    }

    @Test
    fun decapitalize_string() = testAll {
        string_aa.decapitalize() shouldBeSameInstanceAs string_aa
        string_aA.decapitalize() shouldBeSameInstanceAs string_aA
        string_a9.decapitalize() shouldBeSameInstanceAs string_a9
        string_a_.decapitalize() shouldBeSameInstanceAs string_a_
        string_Aa.decapitalize() shouldBe "aa"
        string_AA.decapitalize() shouldBe "aA"
        string_A9.decapitalize() shouldBe "a9"
        string_A_.decapitalize() shouldBe "a_"
        string_9a.decapitalize() shouldBeSameInstanceAs string_9a
        string_9A.decapitalize() shouldBeSameInstanceAs string_9A
        string_99.decapitalize() shouldBeSameInstanceAs string_99
        string_9_.decapitalize() shouldBeSameInstanceAs string_9_
        string__a.decapitalize() shouldBeSameInstanceAs string__a
        string__A.decapitalize() shouldBeSameInstanceAs string__A
        string__9.decapitalize() shouldBeSameInstanceAs string__9
        string___.decapitalize() shouldBeSameInstanceAs string___
    }
}

internal const val string_aa: String = "aa"
internal const val string_aA: String = "aA"
internal const val string_a9: String = "a9"
internal const val string_a_: String = "a_"
internal const val string_Aa: String = "Aa"
internal const val string_AA: String = "AA"
internal const val string_A9: String = "A9"
internal const val string_A_: String = "A_"
internal const val string_9a: String = "9a"
internal const val string_9A: String = "9A"
internal const val string_99: String = "99"
internal const val string_9_: String = "9_"
internal const val string__a: String = "_a"
internal const val string__A: String = "_A"
internal const val string__9: String = "_9"
internal const val string___: String = "__"

internal val charSequence_aa: CharSequence = StringBuilder(string_aa)
internal val charSequence_aA: CharSequence = StringBuilder(string_aA)
internal val charSequence_a9: CharSequence = StringBuilder(string_a9)
internal val charSequence_a_: CharSequence = StringBuilder(string_a_)
internal val charSequence_Aa: CharSequence = StringBuilder(string_Aa)
internal val charSequence_AA: CharSequence = StringBuilder(string_AA)
internal val charSequence_A9: CharSequence = StringBuilder(string_A9)
internal val charSequence_A_: CharSequence = StringBuilder(string_A_)
internal val charSequence_9a: CharSequence = StringBuilder(string_9a)
internal val charSequence_9A: CharSequence = StringBuilder(string_9A)
internal val charSequence_99: CharSequence = StringBuilder(string_99)
internal val charSequence_9_: CharSequence = StringBuilder(string_9_)
internal val charSequence__a: CharSequence = StringBuilder(string__a)
internal val charSequence__A: CharSequence = StringBuilder(string__A)
internal val charSequence__9: CharSequence = StringBuilder(string__9)
internal val charSequence___: CharSequence = StringBuilder(string___)

package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import kotlin.test.Test

class IdentifiersTest {

    @Test fun length() = testAll {
        "abc".toIdentifier(6) shouldHaveLength 6
        "abc".toIdentifier() shouldHaveLength 8
        "abc-def-ghi".toIdentifier() shouldHaveLength 11
    }

    @Test fun should_replace_first_char_by_letter_if_not_a_letter() = testAll {
        "-YZ--ABC".toIdentifier() shouldBe "XYZ--ABC"
        "1YZ--ABC".toIdentifier() shouldBe "IYZ--ABC"
    }

    @Test fun should_replace_leading_digit_with_matching_letter() = testAll {
        "0_ABC123".toIdentifier() shouldBe "O_ABC123"
        "1_ABC123".toIdentifier() shouldBe "I_ABC123"
        "2_ABC123".toIdentifier() shouldBe "Z_ABC123"
        "3_ABC123".toIdentifier() shouldBe "B_ABC123"
        "4_ABC123".toIdentifier() shouldBe "R_ABC123"
        "5_ABC123".toIdentifier() shouldBe "P_ABC123"
        "6_ABC123".toIdentifier() shouldBe "G_ABC123"
        "7_ABC123".toIdentifier() shouldBe "Z_ABC123"
        "8_ABC123".toIdentifier() shouldBe "O_ABC123"
        "9_ABC123".toIdentifier() shouldBe "Y_ABC123"
    }

    @Test fun should_replace_leading_digit_with_lower_case_if_majority_amount_of_letters_are_lower_case() = testAll {
        "0_Abc123".toIdentifier() shouldBe "o_Abc123"
        "1_Abc123".toIdentifier() shouldBe "i_Abc123"
        "2_Abc123".toIdentifier() shouldBe "z_Abc123"
        "3_Abc123".toIdentifier() shouldBe "b_Abc123"
        "4_Abc123".toIdentifier() shouldBe "r_Abc123"
        "5_Abc123".toIdentifier() shouldBe "p_Abc123"
        "6_Abc123".toIdentifier() shouldBe "g_Abc123"
        "7_Abc123".toIdentifier() shouldBe "z_Abc123"
        "8_Abc123".toIdentifier() shouldBe "o_Abc123"
        "9_Abc123".toIdentifier() shouldBe "y_Abc123"
    }

    @Test fun should_keep_alphanumeric_period_underscore_and_dash() = testAll {
        "aB3._-xx".toIdentifier() shouldBe "aB3._-xx"
    }

    @Test fun should_replace_whitespace_with_dash() = testAll {
        "abc- \u2000".toIdentifier(6) shouldBe "abc---"
    }

    @Test fun should_replace_with_underscore_by_default() = testAll {
        "a𝕓☰👋⻦𐦀︎".toIdentifier() shouldBe "a_______"
    }

    @Test fun should_produce_same_basename_for_same_input() = testAll {
        "abc".toIdentifier() shouldBe "abc".toIdentifier()
    }

    @Test fun should_produce_same_basename_for_high_length() = testAll {
        "abc".toIdentifier(1000) shouldBe "abc".toIdentifier(1000)
    }

    @Test fun should_return_constant_for_null() = testAll {
        null.toIdentifier() shouldBe null.toIdentifier()
    }
}

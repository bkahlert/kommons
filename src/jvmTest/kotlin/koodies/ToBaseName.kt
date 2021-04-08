package koodies

import koodies.test.testEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.hasLength
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class BaseNameKtTest {

    @Nested
    inner class ToBaseName {

        @TestFactory
        fun `should replace first char by letter if not a letter`() = testEach(
            "-YZ--ABC" to "XYZ--ABC",
            "1YZ--ABC" to "IYZ--ABC",
        ) { (string, expected) ->
            expect { string.toBaseName() }.that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should replace leading digit with matching letter`() = testEach(
            "0_ABC123" to "O_ABC123",
            "1_ABC123" to "I_ABC123",
            "2_ABC123" to "Z_ABC123",
            "3_ABC123" to "B_ABC123",
            "4_ABC123" to "R_ABC123",
            "5_ABC123" to "P_ABC123",
            "6_ABC123" to "G_ABC123",
            "7_ABC123" to "Z_ABC123",
            "8_ABC123" to "O_ABC123",
            "9_ABC123" to "Y_ABC123",
        ) { (string, expected) ->
            expect { string.toBaseName() }.that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should replace leading digit with lower case if majority amount of letters are lower case`() = testEach(
            "0_Abc123" to "o_Abc123",
            "1_Abc123" to "i_Abc123",
            "2_Abc123" to "z_Abc123",
            "3_Abc123" to "b_Abc123",
            "4_Abc123" to "r_Abc123",
            "5_Abc123" to "p_Abc123",
            "6_Abc123" to "g_Abc123",
            "7_Abc123" to "z_Abc123",
            "8_Abc123" to "o_Abc123",
            "9_Abc123" to "y_Abc123",
        ) { (string, expected) ->
            expect { string.toBaseName() }.that { isEqualTo(expected) }
        }

        @Test
        fun `should keep alphanumeric period underscore and dash`() {
            expectThat("aB3._-xx".toBaseName()).isEqualTo("aB3._-xx")
        }

        @Test
        fun `should replace whitespace with dash`() {
            expectThat("abc- \n\t\r".toBaseName()).isEqualTo("abc-----")
        }

        @Test
        fun `should replace with underscore by default`() {
            expectThat("a𝕓☰👋⻦𐦀︎".toBaseName()).isEqualTo("a_______")
        }

        @Test
        fun `should fill up to min length`() {
            expectThat("abc".toBaseName(6)).hasLength(6)
        }

        @Test
        fun `should fill up to 8 chars by default`() {
            expectThat("abc".toBaseName()).hasLength(8)
        }

        @Test
        fun `should create random string in case of null`() {
            expectThat(null.toBaseName()).hasLength(8)
        }
    }
}

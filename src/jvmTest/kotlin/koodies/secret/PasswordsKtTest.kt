package koodies.secret

import koodies.test.SystemProperty
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PasswordsKtTest {

    @Nested
    inner class Password {

        @SystemProperty("individual-key", "B{1:Βϊ𝌁\uD834\uDF57")
        @Test
        fun `should get password from env`() {
            expectThat(password("individual-key", 1)).isEqualTo("Az09Αω𝌀𝍖")
        }
    }
}

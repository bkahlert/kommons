package koodies.secret

import koodies.test.junit.systemproperties.SystemProperty
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
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

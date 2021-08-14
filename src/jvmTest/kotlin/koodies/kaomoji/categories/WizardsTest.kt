package koodies.kaomoji.categories

import koodies.test.asserting
import koodies.test.expecting
import koodies.text.codePointCount
import org.junit.jupiter.api.Test
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo

class WizardsTest {

    @Test
    fun `should contain all kaomoji`() {
        expecting { Wizards.size } that { isEqualTo(38) }
    }

    @Test
    fun `should create random wizard`() {
        val kaomoji = Wizards.`(＃￣_￣)o︠・━・・━・━━・━☆`.random()
        kaomoji.codePointCount asserting { isGreaterThanOrEqualTo(5) }
    }
}

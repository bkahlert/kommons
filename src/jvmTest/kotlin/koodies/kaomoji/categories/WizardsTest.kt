package koodies.kaomoji.categories

import koodies.kaomoji.Kaomoji
import koodies.test.asserting
import koodies.test.expecting
import koodies.test.testEach
import koodies.text.codePointCount
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo

class WizardsTest {

    @Test
    fun `should contain all kaomoji`() {
        expecting { Wizards.size } that { isEqualTo(8) }
    }

    @TestFactory
    fun `should be properly set`() = testEach(
        Wizards.`(ﾉ＞ω＜)ﾉ﹕･ﾟ’★,｡･﹕*﹕･ﾟ’☆` to Kaomoji("(ﾉ", ">", "ω", "<", ")ﾉ", " :｡･:*:･ﾟ’★,｡･:*:･ﾟ’☆"),
        Wizards.`(＃￣_￣)o︠・━・・━・━━・━☆` to Kaomoji("(＃", "￣", "_", "￣", ")o︠", "・━・・━・━━・━☆"),
        Wizards.`(／￣‿￣)／~~☆’․･․･﹕★’․･․･﹕☆` to Kaomoji("(/", "￣", "‿", "￣", ")/", "~~☆’.･.･:★’.･.･:☆"),
        Wizards.`(∩ᄑ_ᄑ)⊃━☆ﾟ*･｡*･﹕≡( ε﹕)` to Kaomoji("(∩", "ᄑ", "_", "ᄑ", ")⊃", "━☆ﾟ*･｡*･:≡( ε:)"),
        Wizards.`(ノ ˘_˘)ノ ζζζ  ζζζ  ζζζ` to Kaomoji("(ノ ", "˘", "_", "˘", ")ノ", " ζζζ  ζζζ  ζζζ"),
        Wizards.`(ノ°∀°)ノ⌒･*﹕․｡․ ․｡․﹕*･゜ﾟ･*☆` to Kaomoji("(ノ", "°", "∀", "°", ")ノ", "⌒･*:.｡. .｡.:*･゜ﾟ･*☆"),
        Wizards.`(⊃｡•́‿•̀｡)⊃━✿✿✿✿✿✿` to Kaomoji("(⊃｡", "•́", "‿", "•̀", "｡)⊃", "━✿✿✿✿✿✿"),
        Wizards.`ଘ(੭ˊᵕˋ)੭* ੈ✩‧₊˚` to Kaomoji("ଘ(੭", "ˊ", "ᵕ", "ˋ", ")੭", "* ੈ✩‧₊˚"),
    ) { (actual, expected) ->
        expecting { actual } that { isEqualTo(expected) }
    }

    @Test
    fun `should create random wizard`() {
        val kaomoji = Wizards.`(＃￣_￣)o︠・━・・━・━━・━☆`.random()
        kaomoji.codePointCount asserting { isGreaterThanOrEqualTo(5) }
    }
}

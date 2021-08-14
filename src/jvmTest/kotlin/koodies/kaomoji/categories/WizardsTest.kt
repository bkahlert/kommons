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
        expecting { Wizards.size } that { isEqualTo(38) }
    }

    @TestFactory
    fun `should be properly set`() = testEach(
        Wizards.`(ﾉ＞ω＜)ﾉ﹕･ﾟ’★,｡･﹕*﹕･ﾟ’☆` to Kaomoji("(ﾉ", ">", "ω", "<", ")ﾉ", " :｡･:*:･ﾟ’★,｡･:*:･ﾟ’☆"),
        Wizards.`(＃￣_￣)o︠・━・・━・━━・━☆` to Kaomoji("(＃", "￣", "_", "￣", ")o︠", "・━・・━・━━・━☆"),
        Wizards.`(／￣‿￣)／~~☆’․･․･﹕★’․･․･﹕☆` to Kaomoji("(/", "￣", "‿", "￣", ")", "/~~☆’.･.･:★’.･.･:☆"),
        Wizards.`(∩ᄑ_ᄑ)⊃━☆ﾟ*･｡*･﹕≡( ε﹕)` to Kaomoji("(∩", "ᄑ", "_", "ᄑ", ")⊃", "━☆ﾟ*･｡*･:≡( ε:)"),
        Wizards.`(ノ ˘_˘)ノ ζζζ  ζζζ  ζζζ` to Kaomoji("(ノ ", "˘", "_", "˘", ")ノ", " ζζζ  ζζζ  ζζζ"),
        Wizards.`(ノ°∀°)ノ⌒･*﹕․｡․ ․｡․﹕*･゜ﾟ･*☆` to Kaomoji("(ノ", "°", "∀", "°", ")ノ", "⌒･*:.｡. .｡.:*･゜ﾟ･*☆"),
        Wizards.`(⊃｡•́‿•̀｡)⊃━✿✿✿✿✿✿` to Kaomoji("(⊃｡", "•́", "‿", "•̀", "｡)⊃", "━✿✿✿✿✿✿"),
        Wizards.`ଘ(੭ˊᵕˋ)੭* ੈ✩‧₊˚` to Kaomoji("ଘ(੭", "ˊ", "ᵕ", "ˋ", ")੭", "* ੈ✩‧₊˚"),
        Wizards.`(`･Д･)ノ=☆` to Kaomoji("(`", "･", "Д", "･", ")ノ", "=☆"),
        Wizards.`(*｀□)＜炎炎炎炎` to Kaomoji("(*", "｀", "□", "", ")", "<炎炎炎炎"),
        Wizards.`（／｀△´）／≡≡★` to Kaomoji("（／", "`", "△", "´", "）", "／≡≡★"),
        Wizards.`彡ﾟ◉ω◉ )つー☆*` to Kaomoji("彡ﾟ", "◉", "ω", "◉", " )つ", "ー☆*"),
        Wizards.`(∩^o^)⊃━☆゜․*` to Kaomoji("(∩", "^", "o", "^", ")⊃", "━☆゜.*"),
        Wizards.`(ﾉ◕ヮ◕)ﾉ*﹕･ﾟ✧` to Kaomoji("(ﾉ", "◕", "ヮ", "◕", ")ﾉ", "*:･ﾟ✧"),
        Wizards.`(☆_・)・‥…━━━★` to Kaomoji("(", "☆", "_", "・", ")・", "‥…━━━★"),
        Wizards.`(∩｀-´)⊃━炎炎炎炎炎` to Kaomoji("(∩", "｀", "-", "´", ")⊃", "━炎炎炎炎炎"),
        Wizards.`ପ(⚈᷉ʙ⚈᷉)੭̸୫൦⃛` to Kaomoji("ପ(", "⚈᷉", "ʙ", "⚈᷉", ")੭̸", "୫൦⃛"),
        Wizards.`✩°｡⋆⸜(ू｡•ω•｡)` to Kaomoji("✩°｡⋆⸜(ू｡", "•", "ω", "•", "｡)"),
        Wizards.`( ･ω･)o┫☆炎炎炎炎` to Kaomoji("( ", "･", "ω", "･", ")o", "┫☆炎炎炎炎"),
        Wizards.`੭•̀ω•́)੭̸*✩⁺˚` to Kaomoji("੭", "•̀", "ω", "•́", ")੭̸", "*✩⁺˚"),
        Wizards.`( °-°)シ ミ★ ミ☆` to Kaomoji("( ", "°", "-", "°", ")シ", " ミ★ ミ☆"),
        Wizards.`(σ`∀`)σ*。・゜+․*` to Kaomoji("(σ", "'", "∀", "'", ")σ", "*。・゜+.*"),
        Wizards.`(っ・ω・）っ≡≡≡≡≡≡☆` to Kaomoji("(っ", "・", "ω", "・", "）っ", "≡≡≡≡≡≡☆"),
        Wizards.`(ﾉ≧∀≦)ﾉ・‥…━━━★` to Kaomoji("(ﾉ", "≧", "∀", "≦", ")ﾉ", "・‥…━━━★"),
        Wizards.`(੭•̀ω•́)੭̸*✩⁺˚` to Kaomoji("(੭", "•̀", "ω", "•́", ")੭̸", "*✩⁺˚"),
        Wizards.`(∩｀-´)⊃━✿✿✿✿✿✿` to Kaomoji("(∩", "｀", "-", "´", ")⊃", "━✿✿✿✿✿✿"),
        Wizards.`ヽ༼ຈل͜ຈ༽⊃─☆*﹕・ﾟ` to Kaomoji("ヽ༼", "ຈ", "ل͜", "ຈ", "༽⊃", "─☆*:・ﾟ"),
        Wizards.`( ◔ ౪◔)⊃━☆ﾟ․*・` to Kaomoji("( ", "◔", " ౪", "◔", ")⊃", "━☆ﾟ.*・"),
        Wizards.`￡(*’ο’）／☆*。;+，` to Kaomoji("￡(*", "’", "ο", "’", "）", "/☆*。;+，"),
        Wizards.`(੭ˊ͈ ꒵ˋ͈)੭̸*✧⁺˚` to Kaomoji("(੭", "ˊ͈", " ꒵", "ˋ͈", ")੭̸", "*✧⁺˚"),
        Wizards.`(*’▽’)ノ＾—==ΞΞΞ☆` to Kaomoji("(*", "’", "▽", "’", ")ノ", "＾—==ΞΞΞ☆"),
        Wizards.`(∩｡･ｏ･｡)っ․ﾟ☆｡``` to Kaomoji("(∩｡", "･", "ｏ", "･", "｡)っ", ".ﾟ☆｡'`"),
        Wizards.`(∩^o^)⊃━☆ﾟ․*･｡ﾟ` to Kaomoji("(∩", "^", "o", "^", ")⊃", "━☆ﾟ.*･｡ﾟ"),
        Wizards.`༼つಠ益ಠ༽つ ─=≡ΣO))` to Kaomoji("༼つ", "ಠ", "益", "ಠ", "༽つ", " ─=≡ΣO))"),
        Wizards.`(つ◕౪◕)つ━☆ﾟ․*･｡ﾟ` to Kaomoji("(つ", "◕", "౪", "◕", ")つ", "━☆ﾟ.*･｡ﾟ"),
        Wizards.`(*ﾟー^)／``*﹕;,．★` to Kaomoji("(*", "ﾟ", "ー", "^", ")", "/'`*:;,．★"),
        Wizards.`(∩｀-´)⊃━☆ﾟ․*･｡ﾟ` to Kaomoji("(∩", "｀", "-", "´", ")⊃", "━☆ﾟ.*･｡ﾟ"),
        Wizards.`((ε(*･ω･)_／ﾟ･․*･･｡☆` to Kaomoji("((ε(*", "･", "ω", "･", ")_", "/ﾟ･.*･･｡☆"),
    ) { (actual, expected) ->
        expecting { actual } that { isEqualTo(expected) }
    }

    @Test
    fun `should create random wizard`() {
        val kaomoji = Wizards.`(＃￣_￣)o︠・━・・━・━━・━☆`.random()
        kaomoji.codePointCount asserting { isGreaterThanOrEqualTo(5) }
    }
}

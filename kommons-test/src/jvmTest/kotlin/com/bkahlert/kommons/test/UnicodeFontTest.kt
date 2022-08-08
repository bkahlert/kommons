package com.bkahlert.kommons.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class UnicodeFontTest {

    @Test fun format() = testAll {
        UnicodeFont.Bold.format(capitalLetters) shouldBe "𝐀𝐁𝐂𝐃𝐄𝐅𝐆𝐇𝐈𝐉𝐊𝐋𝐌𝐍𝐎𝐏𝐐𝐑𝐒𝐓𝐔𝐕𝐖𝐗𝐘𝐙"
        UnicodeFont.Italic.format(capitalLetters) shouldBe "𝐴𝐵𝐶𝐷𝐸𝐹𝐺𝐻𝐼𝐽𝐾𝐿𝑀𝑁𝑂𝑃𝑄𝑅𝑆𝑇𝑈𝑉𝑊𝑋𝑌𝑍"
        UnicodeFont.BoldItalic.format(capitalLetters) shouldBe "𝑨𝑩𝑪𝑫𝑬𝑭𝑮𝑯𝑰𝑱𝑲𝑳𝑴𝑵𝑶𝑷𝑸𝑹𝑺𝑻𝑼𝑽𝑾𝑿𝒀𝒁"
        UnicodeFont.Script.format(capitalLetters) shouldBe "𝒜ℬ𝒞𝒟ℰℱ𝒢ℋℐ𝒥𝒦ℒℳ𝒩𝒪𝒫𝒬ℛ𝒮𝒯𝒰𝒱𝒲𝒳𝒴𝒵"
        UnicodeFont.BoldScript.format(capitalLetters) shouldBe "𝓐𝓑𝓒𝓓𝓔𝓕𝓖𝓗𝓘𝓙𝓚𝓛𝓜𝓝𝓞𝓟𝓠𝓡𝓢𝓣𝓤𝓥𝓦𝓧𝓨𝓩"
        UnicodeFont.Fraktur.format(capitalLetters) shouldBe "𝔄𝔅ℭ𝔇𝔈𝔉𝔊ℌℑ𝔍𝔎𝔏𝔐𝔑𝔒𝔓𝔔ℜ𝔖𝔗𝔘𝔙𝔚𝔛𝔜ℨ"
        UnicodeFont.BoldFraktur.format(capitalLetters) shouldBe "𝕬𝕭𝕮𝕯𝕰𝕱𝕲𝕳𝕴𝕵𝕶𝕷𝕸𝕹𝕺𝕻𝕼𝕽𝕾𝕿𝖀𝖁𝖂𝖃𝖄𝖅"
        UnicodeFont.DoubleStruck.format(capitalLetters) shouldBe "𝔸𝔹ℂ𝔻𝔼𝔽𝔾ℍ𝕀𝕁𝕂𝕃𝕄ℕ𝕆ℙℚℝ𝕊𝕋𝕌𝕍𝕎𝕏𝕐ℤ"
        UnicodeFont.SansSerif.format(capitalLetters) shouldBe "𝖠𝖡𝖢𝖣𝖤𝖥𝖦𝖧𝖨𝖩𝖪𝖫𝖬𝖭𝖮𝖯𝖰𝖱𝖲𝖳𝖴𝖵𝖶𝖷𝖸𝖹"
        UnicodeFont.SansSerifBold.format(capitalLetters) shouldBe "𝗔𝗕𝗖𝗗𝗘𝗙𝗚𝗛𝗜𝗝𝗞𝗟𝗠𝗡𝗢𝗣𝗤𝗥𝗦𝗧𝗨𝗩𝗪𝗫𝗬𝗭"
        UnicodeFont.SansSerifItalic.format(capitalLetters) shouldBe "𝘈𝘉𝘊𝘋𝘌𝘍𝘎𝘏𝘐𝘑𝘒𝘓𝘔𝘕𝘖𝘗𝘘𝘙𝘚𝘛𝘜𝘝𝘞𝘟𝘠𝘡"
        UnicodeFont.SansSerifBoldItalic.format(capitalLetters) shouldBe "𝘼𝘽𝘾𝘿𝙀𝙁𝙂𝙃𝙄𝙅𝙆𝙇𝙈𝙉𝙊𝙋𝙌𝙍𝙎𝙏𝙐𝙑𝙒𝙓𝙔𝙕"
        UnicodeFont.Monospace.format(capitalLetters) shouldBe "𝙰𝙱𝙲𝙳𝙴𝙵𝙶𝙷𝙸𝙹𝙺𝙻𝙼𝙽𝙾𝙿𝚀𝚁𝚂𝚃𝚄𝚅𝚆𝚇𝚈𝚉"

        UnicodeFont.Bold.format(smallLetters) shouldBe "𝐚𝐛𝐜𝐝𝐞𝐟𝐠𝐡𝐢𝐣𝐤𝐥𝐦𝐧𝐨𝐩𝐪𝐫𝐬𝐭𝐮𝐯𝐰𝐱𝐲𝐳"
        UnicodeFont.Italic.format(smallLetters) shouldBe "𝑎𝑏𝑐𝑑𝑒𝑓𝑔ℎ𝑖𝑗𝑘𝑙𝑚𝑛𝑜𝑝𝑞𝑟𝑠𝑡𝑢𝑣𝑤𝑥𝑦𝑧"
        UnicodeFont.BoldItalic.format(smallLetters) shouldBe "𝒂𝒃𝒄𝒅𝒆𝒇𝒈𝒉𝒊𝒋𝒌𝒍𝒎𝒏𝒐𝒑𝒒𝒓𝒔𝒕𝒖𝒗𝒘𝒙𝒚𝒛"
        UnicodeFont.Script.format(smallLetters) shouldBe "𝒶𝒷𝒸𝒹ℯ𝒻ℊ𝒽𝒾𝒿𝓀𝓁𝓂𝓃ℴ𝓅𝓆𝓇𝓈𝓉𝓊𝓋𝓌𝓍𝓎𝓏"
        UnicodeFont.BoldScript.format(smallLetters) shouldBe "𝓪𝓫𝓬𝓭𝓮𝓯𝓰𝓱𝓲𝓳𝓴𝓵𝓶𝓷𝓸𝓹𝓺𝓻𝓼𝓽𝓾𝓿𝔀𝔁𝔂𝔃"
        UnicodeFont.Fraktur.format(smallLetters) shouldBe "𝔞𝔟𝔠𝔡𝔢𝔣𝔤𝔥𝔦𝔧𝔨𝔩𝔪𝔫𝔬𝔭𝔮𝔯𝔰𝔱𝔲𝔳𝔴𝔵𝔶𝔷"
        UnicodeFont.BoldFraktur.format(smallLetters) shouldBe "𝖆𝖇𝖈𝖉𝖊𝖋𝖌𝖍𝖎𝖏𝖐𝖑𝖒𝖓𝖔𝖕𝖖𝖗𝖘𝖙𝖚𝖛𝖜𝖝𝖞𝖟"
        UnicodeFont.DoubleStruck.format(smallLetters) shouldBe "𝕒𝕓𝕔𝕕𝕖𝕗𝕘𝕙𝕚𝕛𝕜𝕝𝕞𝕟𝕠𝕡𝕢𝕣𝕤𝕥𝕦𝕧𝕨𝕩𝕪𝕫"
        UnicodeFont.SansSerif.format(smallLetters) shouldBe "𝖺𝖻𝖼𝖽𝖾𝖿𝗀𝗁𝗂𝗃𝗄𝗅𝗆𝗇𝗈𝗉𝗊𝗋𝗌𝗍𝗎𝗏𝗐𝗑𝗒𝗓"
        UnicodeFont.SansSerifBold.format(smallLetters) shouldBe "𝗮𝗯𝗰𝗱𝗲𝗳𝗴𝗵𝗶𝗷𝗸𝗹𝗺𝗻𝗼𝗽𝗾𝗿𝘀𝘁𝘂𝘃𝘄𝘅𝘆𝘇"
        UnicodeFont.SansSerifItalic.format(smallLetters) shouldBe "𝘢𝘣𝘤𝘥𝘦𝘧𝘨𝘩𝘪𝘫𝘬𝘭𝘮𝘯𝘰𝘱𝘲𝘳𝘴𝘵𝘶𝘷𝘸𝘹𝘺𝘻"
        UnicodeFont.SansSerifBoldItalic.format(smallLetters) shouldBe "𝙖𝙗𝙘𝙙𝙚𝙛𝙜𝙝𝙞𝙟𝙠𝙡𝙢𝙣𝙤𝙥𝙦𝙧𝙨𝙩𝙪𝙫𝙬𝙭𝙮𝙯"
        UnicodeFont.Monospace.format(smallLetters) shouldBe "𝚊𝚋𝚌𝚍𝚎𝚏𝚐𝚑𝚒𝚓𝚔𝚕𝚖𝚗𝚘𝚙𝚚𝚛𝚜𝚝𝚞𝚟𝚠𝚡𝚢𝚣"

        UnicodeFont.Bold.format(digits) shouldBe "𝟎𝟏𝟐𝟑𝟒𝟓𝟔𝟕𝟖𝟗"
        UnicodeFont.Italic.format(digits) shouldBe digits
        UnicodeFont.BoldItalic.format(digits) shouldBe digits
        UnicodeFont.Script.format(digits) shouldBe digits
        UnicodeFont.BoldScript.format(digits) shouldBe digits
        UnicodeFont.Fraktur.format(digits) shouldBe digits
        UnicodeFont.BoldFraktur.format(digits) shouldBe digits
        UnicodeFont.DoubleStruck.format(digits) shouldBe "𝟘𝟙𝟚𝟛𝟜𝟝𝟞𝟟𝟠𝟡"
        UnicodeFont.SansSerif.format(digits) shouldBe "𝟢𝟣𝟤𝟥𝟦𝟧𝟨𝟩𝟪𝟫"
        UnicodeFont.SansSerifBold.format(digits) shouldBe "𝟬𝟭𝟮𝟯𝟰𝟱𝟲𝟳𝟴𝟵"
        UnicodeFont.SansSerifItalic.format(digits) shouldBe digits
        UnicodeFont.SansSerifBoldItalic.format(digits) shouldBe digits
        UnicodeFont.Monospace.format(digits) shouldBe "𝟶𝟷𝟸𝟹𝟺𝟻𝟼𝟽𝟾𝟿"
    }

    @Test fun format_throwing() = testAll {
        UnicodeFont.Bold.format(digits) shouldBe "𝟎𝟏𝟐𝟑𝟒𝟓𝟔𝟕𝟖𝟗"
        shouldThrow<IllegalArgumentException> { UnicodeFont.Italic.format(digits) { throw IllegalArgumentException("cannot format $it") } }
    }
}

private const val capitalLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val smallLetters = "abcdefghijklmnopqrstuvwxyz"
private const val digits = "0123456789"

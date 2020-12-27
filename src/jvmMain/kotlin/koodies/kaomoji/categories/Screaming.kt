@file:Suppress(
    "PublicApiImplicitType",
    "KDocMissingDocumentation",
    "ObjectPropertyName",
    "RemoveRedundantBackticks",
    "unused",
    "NonAsciiCharacters",
    "SpellCheckingInspection",
    "DANGEROUS_CHARACTERS"
)


package koodies.kaomoji.categories

import koodies.kaomoji.Category

object Screaming : Category() {
    val `ヽ(๏∀๏ )ﾉ` by auto()
    val `ヽ(｀Д´)ﾉ` by auto()
    val `ヽ(ｏ`皿′ｏ)ﾉ` by auto("ヽ(ｏ`皿′ｏ)ﾉ")
    val `ヽ(`Д´)ﾉ` by auto("ヽ(`Д´)ﾉ")
}

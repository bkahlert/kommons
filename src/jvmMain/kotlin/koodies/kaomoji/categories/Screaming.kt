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
import koodies.kaomoji.Kaomoji

public object Screaming : Category() {
    public val `ヽ(๏∀๏ )ﾉ`: Kaomoji by auto()
    public val `ヽ(｀Д´)ﾉ`: Kaomoji by auto()
    public val `ヽ(ｏ`皿′ｏ)ﾉ`: Kaomoji by auto("ヽ(ｏ`皿′ｏ)ﾉ")
    public val `ヽ(`Д´)ﾉ`: Kaomoji by auto("ヽ(`Д´)ﾉ")
}

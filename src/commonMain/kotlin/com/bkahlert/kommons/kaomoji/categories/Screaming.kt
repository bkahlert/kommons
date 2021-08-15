@file:Suppress(
    "KDocMissingDocumentation",
    "ObjectPropertyName",
    "RemoveRedundantBackticks",
    "unused",
    "NonAsciiCharacters",
    "SpellCheckingInspection",
    "DANGEROUS_CHARACTERS"
)

package com.bkahlert.kommons.kaomoji.categories

import com.bkahlert.kommons.kaomoji.Category
import com.bkahlert.kommons.kaomoji.Kaomoji
import kotlin.js.JsName

public object Screaming : Category() {
    @JsName("screaming00") public val `ヽ(๏∀๏ )ﾉ`: Kaomoji by auto()
    @JsName("screaming01") public val `ヽ(｀Д´)ﾉ`: Kaomoji by auto()
    @JsName("screaming02") public val `ヽ(ｏ`皿′ｏ)ﾉ`: Kaomoji by auto("ヽ(ｏ`皿′ｏ)ﾉ")
    @JsName("screaming03") public val `ヽ(`Д´)ﾉ`: Kaomoji by auto("ヽ(`Д´)ﾉ")
}

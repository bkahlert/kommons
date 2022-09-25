@file:Suppress(
    "KDocMissingDocumentation",
    "ObjectPropertyName",
    "RemoveRedundantBackticks",
    "unused",
    "NonAsciiCharacters",
    "SpellCheckingInspection"
)

package com.bkahlert.kommons.kaomoji.categories

import com.bkahlert.kommons.kaomoji.Category
import com.bkahlert.kommons.kaomoji.Kaomoji
import kotlin.js.JsName

public object Rain : Category() {
    @JsName("rain00") public val `｀、ヽ｀ヽ｀、ヽ（ノ＞＜）ノ ｀、ヽ｀☂ヽ｀、ヽ`: Kaomoji by parsing("｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ")
    @JsName("rain01") public val `｀ヽ｀（（（（（ （ ⊃・ω・）⊃☂｀（´ω｀u）））ヽ｀、`: Kaomoji by parsing("｀ヽ｀((((( ( ⊃・ω・)⊃☂｀(´ω｀u)))ヽ｀、")
}

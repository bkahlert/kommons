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

public object Heroes : Category() {
    @JsName("heroes00") public val `─=≡Σ（（［ ⊐•̀⌂•́］⊐`: Kaomoji by parsing("─=≡Σ(([ ⊐•̀⌂•́]⊐")
    @JsName("heroes01") public val `⸺̲͞ （（（ꎤ ✧曲✧）—̠͞o`: Kaomoji by parsing("⸺̲͞ (((ꎤ ✧曲✧)—̠͞o") // ‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞o not rendering in all browsers
}

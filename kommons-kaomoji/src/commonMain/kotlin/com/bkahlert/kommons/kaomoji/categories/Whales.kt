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

public object Whales : Category() {
    @JsName("whales00") public val `․ ＞＜｛｛｛․______）`: Kaomoji by parsing(". ><{{{.______)")
    @JsName("whales01") public val `․ ＞＜｛｛｛o ______）`: Kaomoji by parsing(". ><{{{o ______)")
    @JsName("whales02") public val `․ ＞＜｛｛｛x_______）`: Kaomoji by parsing(". ><{{{x_______)")
}

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

public object Money : Category() {
    @JsName("money00") public val `［̲̅＄̲̅（̲̅5̲̅）̲̅＄̲̅］`: Kaomoji by parsing("[̲̅\$̲̅(̲̅5̲̅)̲̅\$̲̅]")
}

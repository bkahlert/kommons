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

public object Celebrities : Category() {
    @JsName("eltonJohn") public val `⪿ ↂ ˒̫̮ ↂ ⫀`: Kaomoji by parsing()
}
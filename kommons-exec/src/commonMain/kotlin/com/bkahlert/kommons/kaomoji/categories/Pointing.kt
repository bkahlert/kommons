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

public object Pointing : Category() {
    @JsName("pointing00") public val `☜ق❂Ⴢ❂ق☞`: Kaomoji by auto()
    @JsName("pointing01") public val `☜（⌒▽⌒）☞`: Kaomoji by auto("☜(⌒▽⌒)☞")
    @JsName("pointing02") public val `☜（ﾟヮﾟ☜）`: Kaomoji by auto("☜(ﾟヮﾟ☜)")
    @JsName("pointing03") public val `☜-（ΘLΘ）-☞`: Kaomoji by auto("☜-(ΘLΘ)-☞")
}

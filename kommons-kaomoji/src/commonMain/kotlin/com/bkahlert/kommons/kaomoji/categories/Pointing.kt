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

public object Pointing : Category() {
    @JsName("pointing00") public val `☜ق❂Ⴢ❂ق☞`: Kaomoji by parsing()
    @JsName("pointing01") public val `☜（⌒▽⌒）☞`: Kaomoji by parsing("☜(⌒▽⌒)☞")
    @JsName("pointing02") public val `☜（ﾟヮﾟ☜）`: Kaomoji by parsing("☜(ﾟヮﾟ☜)")
    @JsName("pointing03") public val `☜-（ΘLΘ）-☞`: Kaomoji by parsing("☜-(ΘLΘ)-☞")
}

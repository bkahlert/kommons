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

public object Chasing : Category() {
    @JsName("chasing00") public val `（○｀д´）ﾉｼ Σ（っﾟДﾟ）っ`: Kaomoji by parsing("(○｀д´)ﾉｼ Σ(っﾟДﾟ)っ")
    @JsName("chasing01") public val `☎Σ⊂⊂（☉ω☉∩）`: Kaomoji by parsing("☎Σ⊂⊂(☉ω☉∩)")
}
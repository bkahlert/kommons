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

/**
 * Selection of baby [Kaomoji]
 */
public object Babies : Category() {

    /**
     * `ლ(´ڡ´ლ)`
     */
    @JsName("babies00") public val `ლ（´ڡ´ლ）`: Kaomoji by auto("ლ(´ڡ`ლ)")

    /**
     * `ლ(́◉◞౪◟◉‵ლ)`
     */
    @JsName("babies01") public val `ლ（́◉◞౪◟◉‵ლ）`: Kaomoji by auto("ლ(́◉◞౪◟◉‵ლ)")

    /**
     * `(●´ω｀●)`
     */
    @JsName("babies02") public val `（●´ω｀●）`: Kaomoji by auto("(●´ω｀●)")
}

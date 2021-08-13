@file:Suppress(
    "PublicApiImplicitType",
    "KDocMissingDocumentation",
    "ObjectPropertyName",
    "RemoveRedundantBackticks",
    "unused",
    "NonAsciiCharacters",
    "SpellCheckingInspection",
    "DANGEROUS_CHARACTERS"
)


package koodies.kaomoji.categories

import koodies.kaomoji.Category
import koodies.kaomoji.Kaomoji
import kotlin.js.JsName

/**
 * Selection of baby [Kaomoji]
 */
public object Babies : Category() {

    /**
     * `ლ(´ڡ´ლ)`
     */
    @JsName("babies00") public val `ლ(´ڡ´ლ)`: Kaomoji by auto("ლ(´ڡ`ლ)")

    /**
     * `ლ(́◉◞౪◟◉‵ლ)`
     */
    @JsName("babies01") public val `ლ(́◉◞౪◟◉‵ლ)`: Kaomoji by auto()

    /**
     * `(●´ω｀●)`
     */
    @JsName("babies02") public val `(●´ω｀●)`: Kaomoji by auto()
}

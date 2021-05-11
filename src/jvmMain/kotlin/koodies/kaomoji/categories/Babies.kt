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

/**
 * Selection of baby [Kaomoji]
 */
public object Babies : Category() {

    /**
     * `ლ(´ڡ´ლ)`
     */
    public val `ლ(´ڡ´ლ)`: Kaomoji by auto("ლ(´ڡ`ლ)")

    /**
     * `ლ(́◉◞౪◟◉‵ლ)`
     */
    public val `ლ(́◉◞౪◟◉‵ლ)`: Kaomoji by auto()

    /**
     * `(●´ω｀●)`
     */
    public val `(●´ω｀●)`: Kaomoji by auto()
}

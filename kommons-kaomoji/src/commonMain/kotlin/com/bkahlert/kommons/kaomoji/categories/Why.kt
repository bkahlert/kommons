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

public object Why : Category() {
    @JsName("why00") public val `щ（ﾟДﾟщ`: Kaomoji by parsing()
    @JsName("why01") public val `щ（ಠ益ಠщ）`: Kaomoji by parsing("щ(ಠ益ಠщ)")
    @JsName("why02") public val `щ（ಥДಥщ）`: Kaomoji by parsing("щ(ಥДಥщ)")
}

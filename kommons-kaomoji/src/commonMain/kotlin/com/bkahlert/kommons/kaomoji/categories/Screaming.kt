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

public object Screaming : Category() {
    @JsName("screaming00") public val `ヽ（๏∀๏ ）ﾉ`: Kaomoji by parsing("ヽ(๏∀๏ )ﾉ")
    @JsName("screaming01") public val `ヽ（｀Д´）ﾉ`: Kaomoji by parsing("ヽ(｀Д´)ﾉ")
    @JsName("screaming02") public val `ヽ（ｏ`皿′ｏ）ﾉ`: Kaomoji by parsing("ヽ(ｏ`皿′ｏ)ﾉ")
    @JsName("screaming03") public val `ヽ（`Д´）ﾉ`: Kaomoji by parsing("ヽ(`Д´)ﾉ")
}

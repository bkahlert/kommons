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

public object Geek : Category() {
    @JsName("geek00") public val `◖⎚∠⎚◗`: Kaomoji by parsing()
    @JsName("geek01") public val `［⸌º～°］⸌`: Kaomoji by parsing("[⸌º~°]⸌")
    @JsName("geek02") public val `◘-◘`: Kaomoji by parsing()
    @JsName("geek03") public val `［¬⫏-⫐］¬`: Kaomoji by parsing("[¬⫏-⫐]¬")
    public val `ㄖꏁㄖ`: Kaomoji by parsing()
    @JsName("geek05") public val `╰（⊡-⊡）و✎⮹`: Kaomoji by parsing("╰(⊡-⊡)و✎⮹")
    @JsName("geek06") public val `（⌐■_■）┐`: Kaomoji by parsing("(⌐■_■)┐")
    @JsName("geek07") public val `（․づ▣ ͜ʖ▣）づ․`: Kaomoji by parsing("(.づ▣ ͜ʖ▣)づ.")
    @JsName("geek08") public val `◙‿◙`: Kaomoji by parsing()
    @JsName("geek09") public val `◪_◪`: Kaomoji by parsing()
    @JsName("geek10") public val `☐_☐`: Kaomoji by parsing()
    @JsName("geek11") public val `（ •_•）＞⌐■-■`: Kaomoji by parsing("( •_•)>⌐■-■")
    @JsName("geek12") public val `＜【☯】‿【☯】＞`: Kaomoji by parsing("<【☯】‿【☯】>")
}

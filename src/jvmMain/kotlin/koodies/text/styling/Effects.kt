package koodies.text.styling

class Effects(private val text: String) {
    /**
     * `·<❮❰❰❰ echo ❱❱❱❯>·`
     */
    fun echo(): String = "·<❮❰❰❰ $text ❱❱❱❯>·"

    /**
     * `͔˱❮❰( saying`
     */
    fun saying(): String = "͔˱❮❰( $text"

    /**
     * `【tag】`
     */
    fun tag(): String = "【$text】"

    /**
     * `❲unit❳`
     */
    fun unit(): String = "❲$text❳"
}

val CharSequence.effects get() = Effects(this.toString())
val String.effects get() = Effects(this)

package koodies.text.styling

public class Effects(private val text: String) {
    /**
     * `·<❮❰❰❰ echo ❱❱❱❯>·`
     */
    public fun echo(): String = "·<❮❰❰❰ $text ❱❱❱❯>·"

    /**
     * `͔˱❮❰( saying`
     */
    public fun saying(): String = "͔˱❮❰( $text"

    /**
     * `【tag】`
     */
    public fun tag(): String = "【$text】"

    /**
     * `❲unit❳`
     */
    public fun unit(): String = "❲$text❳"
}

public val CharSequence.effects: Effects get() = Effects(this.toString())
public val String.effects: Effects get() = Effects(this)

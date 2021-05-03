package koodies.terminal

import koodies.text.Banner

@Deprecated("use koodies.text.Banner.banner")
public object Banner {
    @Deprecated("use koodies.text.Banner.banner", ReplaceWith("banner(text)", "koodies.text.Banner.banner"))
    public fun banner(text: String): String = Banner.banner(text)
}

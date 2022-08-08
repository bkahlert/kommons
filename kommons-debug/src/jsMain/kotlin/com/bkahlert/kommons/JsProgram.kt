package com.bkahlert.kommons

import com.bkahlert.kommons.Platform.Browser
import com.bkahlert.kommons.Platform.NodeJS
import kotlinx.browser.window

/** The running program. */
public actual object Program {

    /** Whether this program is running in debug mode. */
    public actual val isDebugging: Boolean = false

    private val onExitDelegate by lazy {
        when (Platform.Current) {
            Browser -> ::browserOnExit
            NodeJS -> ::nodeOnExit
            else -> error("Unsupported platform")
        }
    }

    /** Registers the specified [handler] as to be called when this program is about to stop. */
    public actual fun onExit(handler: () -> Unit): Unit = onExitDelegate(handler)
}

private fun browserOnExit(handler: () -> Unit) {
    window.addEventListener(
        type = "beforeunload",
        callback = {
            runCatching(handler).onFailure {
                console.error("An exception occurred while unloading.", it)
            }
        })
}

private fun nodeOnExit(handler: () -> Unit) {
    val process = js("require('process')")
    process.on("beforeExit", fun(_: Any?) {
        runCatching(handler).onFailure {
            console.error("An exception occurred while unloading.", it)
        }
    })
}

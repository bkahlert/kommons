package koodies.exception

/**
 * The root cause of this [Throwable], that is,
 * the throwable that was thrown the first.
 */
public val Throwable.rootCause: Throwable
    get() {
        var rootCause: Throwable = this
        while (rootCause.cause != null && rootCause.cause !== rootCause) {
            rootCause = rootCause.cause ?: error("Must not happen.")
        }
        return rootCause
    }

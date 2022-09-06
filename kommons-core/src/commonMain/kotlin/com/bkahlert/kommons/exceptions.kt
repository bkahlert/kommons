package com.bkahlert.kommons

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

/**
 * The causes of this [Throwable], that is,
 * a list containing the [Throwable.cause], the cause of the cause, ...
 */
public val Throwable.causes: List<Throwable>
    get() = when (val cause = cause) {
        null -> emptyList()
        else -> buildList {
            add(cause)
            addAll(cause.causes)
        }
    }

package com.bkahlert.kommons.test.junit

import org.junit.jupiter.api.extension.ExtensionContext

/** Display name of a test which is resolved in the scope a test execution. */
public class DisplayName(
    /** Display names of all ancestors. */
    public val ancestorDisplayNames: List<String>,
    /** Display name. */
    public val displayName: String,
) : CharSequence by displayName {
    internal constructor(extensionContext: ExtensionContext) : this(
        extensionContext.ancestors.reversed().map { it.displayName },
        extensionContext.displayName,
    )

    /** Composed display name consisting of all display names but the root display name separated by ` ➜ `. */
    public val composedDisplayName: String
        get() = buildList {
            addAll(ancestorDisplayNames.drop(1))
            add(displayName)
        }.joinToString(" ➜ ")

    override fun toString(): String = displayName
}

public fun ExtensionContext.displayName(): DisplayName = DisplayName(this)

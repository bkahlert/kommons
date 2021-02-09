package koodies.builder.context

import koodies.builder.Init

/**
 * Stores the aggregated [state] of all operations
 * performed on the possibly immutable [context].
 */
interface StatefulContext<C, S> {
    /**
     * The context an [Init] operates on during a build process.
     */
    val context: C

    /**
     * The aggregated state that reflects all operations
     * performed on the [context] during a build process.
     */
    val state: S
}

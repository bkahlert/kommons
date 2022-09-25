package com.bkahlert.kommons.exec

/** An executable can be executed using [exec]. */
public interface Executable {

    /** [Executor] that can be used to execute this executable. */
    public val exec: SyncExecutor
}

package com.bkahlert.kommons.io

/**
 * A file stored purely in-memory.
 */
public interface InMemoryFile {
    /**
     * Name of this in-memory file.
     */
    public val name: String

    /**
     * Data this in-memory file consists of.
     */
    public val data: ByteArray
}

package com.bkahlert.kommons_deprecated.io

// TODO delete if possible
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
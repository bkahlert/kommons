package koodies.io.file

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime

/**
 * This path's last modified time.
 */
var Path.lastModified: FileTime
    get() = Files.getLastModifiedTime(this)
    set(fileTime) {
        Files.setLastModifiedTime(this, fileTime)
    }

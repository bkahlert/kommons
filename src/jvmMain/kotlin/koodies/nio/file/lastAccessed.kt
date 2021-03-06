package koodies.io.file

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime

/**
 * This path's last accessed time.
 */
public var Path.lastAccessed: FileTime
    get() = Files.getFileAttributeView(this, BasicFileAttributeView::class.java).readAttributes().lastAccessTime()
    set(fileTime) {
        Files.setAttribute(this, "basic:lastAccessTime", fileTime)
    }

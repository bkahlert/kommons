package koodies.io.file

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime

/**
 * This path's creation time.
 */
var Path.created: FileTime
    get() = Files.getFileAttributeView(this, BasicFileAttributeView::class.java).readAttributes().creationTime()
    set(fileTime) {
        Files.setAttribute(this, "basic:creationTime", fileTime)
    }


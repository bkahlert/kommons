package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.time.Now
import com.bkahlert.kommons.time.seconds
import com.bkahlert.kommons.unit.milli
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.setLastModifiedTime
import kotlin.time.Duration

public fun Path.touch(): Path {
    parent.requireExists()
    if (exists()) setLastModifiedTime(Now.fileTime)
    else createFile()
    return this
}

/**
 * Contains since when this file was last modified.
 */
public var Path.age: Duration
    get() :Duration = (Now.millis - getLastModifiedTime().toMillis()).milli.seconds
    set(value) {
        setLastModifiedTime(FileTime.from(Now.minus(value)))
    }

/**
 * This path's creation time.
 */
public var Path.created: FileTime
    get() = Files.getFileAttributeView(this, BasicFileAttributeView::class.java).readAttributes().creationTime()
    set(fileTime) {
        Files.setAttribute(this, "basic:creationTime", fileTime)
    }

/**
 * This path's last accessed time.
 */
public var Path.lastAccessed: FileTime
    get() = Files.getFileAttributeView(this, BasicFileAttributeView::class.java).readAttributes().lastAccessTime()
    set(fileTime) {
        Files.setAttribute(this, "basic:lastAccessTime", fileTime)
    }

/**
 * This path's last modified time.
 */
public var Path.lastModified: FileTime
    get() = Files.getLastModifiedTime(this)
    set(fileTime) {
        Files.setLastModifiedTime(this, fileTime)
    }

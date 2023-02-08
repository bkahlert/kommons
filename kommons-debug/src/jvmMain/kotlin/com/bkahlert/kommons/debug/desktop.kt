package com.bkahlert.kommons.debug

import com.bkahlert.kommons.io.toFileOrNull
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path

private val logger = LoggerFactory.getLogger("kommons-debug")

/** Attempts to open locally this URL locally. */
public fun URL.open() {
    toFileOrNull()?.open()
}

/** Attempts to locally open this URI locally. */
public fun URI.open() {
    toFileOrNull()?.open()
}

/** Attempts to locally open this path locally. */
public fun Path.open() {
    toFileOrNull()?.open()
}

/** Attempts to locally open this file locally. */
public fun File.open() {
    kotlin.runCatching {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(this)
    }.onFailure { exception ->
        logger.error("Failed to open {}", this, exception)
    }
}


/** Attempts to locally open the directory containing the file this URL points, or the directory itself if it's one. */
public fun URL.locate() {
    toFileOrNull()?.locate()
}

/** Attempts to locally open the directory containing the file this URI points, or the directory itself if it's one. */
public fun URI.locate() {
    toFileOrNull()?.locate()
}

/** Attempts to locally open the directory containing the file this path points, or the directory itself if it's one. */
public fun Path.locate() {
    toFileOrNull()?.locate()
}

/** Attempts to locally open the directory containing this file, or the directory itself if it's one. */
public fun File.locate() {
    if (isDirectory) open()
    else parentFile?.open()
}

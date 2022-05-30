package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.time.Now
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.setLastModifiedTime

public fun Path.touch(): Path {
    parent.requireExists()
    if (exists()) setLastModifiedTime(Now.fileTime)
    else createFile()
    return this
}

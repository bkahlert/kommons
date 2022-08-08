package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.Now
import com.bkahlert.kommons.toFileTime
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.setLastModifiedTime

public fun Path.touch(): Path {
    parent.requireExists()
    if (exists()) setLastModifiedTime(Now.toFileTime())
    else createFile()
    return this
}

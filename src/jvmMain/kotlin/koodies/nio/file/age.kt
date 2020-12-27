package koodies.io.file

import koodies.time.Now
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.time.Duration
import kotlin.time.milliseconds

var Path.age: Duration
    get() :Duration = (Now.millis - lastModified.toMillis()).milliseconds
    set(value) {
        lastModified = FileTime.from(Now.minus(value))
    }

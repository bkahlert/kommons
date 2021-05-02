package koodies.io.file

import koodies.io.path.pathString
import koodies.text.quoted
import java.nio.file.Path

public val Path.quoted: String get() = pathString.quoted

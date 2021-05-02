package koodies.io.file

import koodies.io.path.pathString
import java.net.URLConnection
import java.nio.file.Path

/**
 * Contains the likely MIME type guessed from the file name.
 */
public val Path.guessedMimeType: String?
    get() = kotlin.runCatching { URLConnection.guessContentTypeFromName(fileName.pathString) }.getOrNull()

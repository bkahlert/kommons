package koodies.io.file

import koodies.io.path.asString
import java.net.URLConnection
import java.nio.file.Path

/**
 * Contains the likely MIME type guessed from the file name.
 */
val Path.guessedMimeType: String?
    get() = kotlin.runCatching { URLConnection.guessContentTypeFromName(fileName.asString()) }.getOrNull()

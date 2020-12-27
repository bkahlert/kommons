package koodies.io.file

import koodies.nio.file.toBase64
import java.nio.file.Path

/**
 * Converts this path to a [data URI](https://en.wikipedia.org/wiki/Data_URI_scheme) of the form
 * `data:[<media type>][;base64],<data>`, e.g. `data:image/gif;base64,...`
 */
fun Path.toDataUri(explicitMimeType: String? = null, fallbackMimeType: String = "application/octet-stream"): String {
    val mimeType = explicitMimeType ?: guessedMimeType ?: fallbackMimeType
    return "data:$mimeType;base64,${toBase64()}"
}

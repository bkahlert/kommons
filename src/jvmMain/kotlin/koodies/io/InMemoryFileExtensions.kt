package koodies.io

import koodies.docker.DockerImage
import koodies.docker.docker
import koodies.exec.output
import koodies.io.path.asPath
import koodies.io.path.copyTo
import koodies.io.path.copyToDirectory
import koodies.io.path.extensionOrNull
import koodies.io.path.pathString
import koodies.io.path.withDirectoriesCreated
import koodies.io.path.writeBytes
import koodies.text.ANSI
import koodies.text.ANSI.resetLines
import koodies.tracing.rendering.Renderer.Companion.NOOP
import koodies.tracing.rendering.RendererProvider
import java.nio.file.Path
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readBytes

/**
 * Copies `this` in-memory file to the specified [target].
 *
 * @see copyToDirectory
 * @see copyToTemp
 */
public fun InMemoryFile.copyTo(target: Path): Path =
    target.withDirectoriesCreated().writeBytes(data)

/**
 * Copies `this` in-memory file to the specified [target] directory.
 *
 * @see copyTo
 * @see copyToTemp
 */
public fun InMemoryFile.copyToDirectory(target: Path): Path =
    target.resolve(name).withDirectoriesCreated().writeBytes(data)

/**
 * Copies `this` in-memory file to a temporary directory—the name based on the
 * optional [base] and [extension].
 */
public fun InMemoryFile.copyToTemp(
    base: String = name.asPath().nameWithoutExtension,
    extension: String = name.asPath().extensionOrNull?.let { ".$it" } ?: "",
): Path = copyTo(Koodies.FilesTemp.tempFile(base, extension))

/**
 * Returns this image as a bitmap. The image is automatically rasterized if necessary.
 */
public fun InMemoryImage.rasterize(renderer: RendererProvider = { NOOP }): ByteArray =
    if (isBitmap) data
    else runWithTempDir {
        val rasterized = "image.png"
        docker(LibRSvg, "-z", 5, "--output", rasterized, renderer = renderer, inputStream = data.inputStream())
        resolve(rasterized).readBytes()
    }

/**
 * Renders this image to a sequence of [ANSI] escape codes.
 */
public fun InMemoryImage.toAsciiArt(renderer: RendererProvider = { NOOP }): String = runWithTempDir {
    val fileName = resolve(this@toAsciiArt.baseName).writeBytes(rasterize(renderer)).fileName
    docker(Chafa, renderer = renderer) { "/opt/bin/chafa -c full -w 9 $fileName" }
        .io.output.ansiKept.resetLines()
}

/**
 * Returns an [InMemoryImage] with `this` file loaded as its content.
 */
public fun Path.asImageOrNull(): InMemoryImage? {
    if (isRegularFile() && isReadable() && fileName.extensionOrNull in InMemoryImage.EXTENSIONS) {
        return InMemoryImage(fileName.pathString, readBytes())
    }
    return null
}

@Suppress("SpellCheckingInspection")
internal object LibRSvg : DockerImage("minidocks", listOf("librsvg"))

@Suppress("SpellCheckingInspection")
internal object Chafa : DockerImage("rafib", listOf("awesome-cli-binaries"))

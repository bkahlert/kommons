package com.bkahlert.kommons.io

import com.bkahlert.kommons.Kommons
import com.bkahlert.kommons.docker.Docker.AwesomeCliBinaries
import com.bkahlert.kommons.docker.Docker.LibRSvg
import com.bkahlert.kommons.docker.docker
import com.bkahlert.kommons.exec.output
import com.bkahlert.kommons.io.path.asPath
import com.bkahlert.kommons.io.path.copyTo
import com.bkahlert.kommons.io.path.copyToDirectory
import com.bkahlert.kommons.io.path.createParentDirectories
import com.bkahlert.kommons.io.path.extensionOrNull
import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kommons.io.path.runWithTempDir
import com.bkahlert.kommons.io.path.tempFile
import com.bkahlert.kommons.io.path.writeBytes
import com.bkahlert.kommons.text.ANSI
import com.bkahlert.kommons.text.ANSI.resetLines
import com.bkahlert.kommons.tracing.rendering.Renderer.Companion.NOOP
import com.bkahlert.kommons.tracing.rendering.RendererProvider
import java.nio.file.Path
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readBytes

/**
 * Copies this in-memory file to the specified [target].
 *
 * @see copyToDirectory
 * @see copyToTemp
 */
public fun InMemoryFile.copyTo(target: Path): Path =
    target.createParentDirectories().writeBytes(data)

/**
 * Copies this in-memory file to the specified [target] directory.
 *
 * @see copyTo
 * @see copyToTemp
 */
public fun InMemoryFile.copyToDirectory(target: Path): Path =
    target.resolve(name).createParentDirectories().writeBytes(data)

/**
 * Copies this in-memory file to a temporary directory—the name based on the
 * optional [base] and [extension].
 */
public fun InMemoryFile.copyToTemp(
    base: String = name.asPath().nameWithoutExtension,
    extension: String = name.asPath().extensionOrNull?.let { ".$it" } ?: "",
): Path = copyTo(Kommons.FilesTemp.tempFile(base, extension))

/**
 * Returns this image as a bitmap. The image is automatically rasterized if necessary.
 */
public fun InMemoryImage.rasterize(renderer: RendererProvider = { NOOP }): ByteArray =
    if (isBitmap) data
    else runWithTempDir {
        val rasterized = "image.png"
        docker(LibRSvg, "-z", 5, "--output", rasterized, name = "rasterize vector", renderer = renderer, inputStream = data.inputStream())
        resolve(rasterized).readBytes()
    }

/**
 * Renders this image to a sequence of [ANSI] escape codes.
 */
public fun InMemoryImage.toAsciiArt(renderer: RendererProvider = { NOOP }): String = runWithTempDir {
    val fileName = resolve(this@toAsciiArt.baseName).writeBytes(rasterize(renderer)).fileName
    docker(AwesomeCliBinaries, name = "convert to ascii art", renderer = renderer) { "/opt/bin/chafa -c full -w 9 $fileName" }
        .io.output.ansiKept.resetLines()
}

/**
 * Returns an [InMemoryImage] with this file loaded as its content.
 */
public fun Path.asImageOrNull(): InMemoryImage? {
    if (isRegularFile() && isReadable() && fileName.extensionOrNull in InMemoryImage.EXTENSIONS) {
        return InMemoryImage(fileName.pathString, readBytes())
    }
    return null
}

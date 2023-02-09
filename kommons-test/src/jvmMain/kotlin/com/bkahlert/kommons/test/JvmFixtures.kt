package com.bkahlert.kommons.test

import com.bkahlert.kommons.io.createTempDirectory
import com.bkahlert.kommons.io.useBufferedOutputStream
import com.bkahlert.kommons.io.useBufferedWriter
import com.bkahlert.kommons.test.fixtures.EmojiTextDocumentFixture
import com.bkahlert.kommons.test.fixtures.GifImageFixture
import com.bkahlert.kommons.test.fixtures.HtmlDocumentFixture
import com.bkahlert.kommons.test.fixtures.ResourceFixture
import com.bkahlert.kommons.test.fixtures.SvgImageFixture
import com.bkahlert.kommons.test.fixtures.TextResourceFixture
import com.bkahlert.kommons.test.fixtures.UnicodeTextDocumentFixture
import java.io.InputStream
import java.io.StringReader
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.attribute.FileAttribute
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempFile
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/** The [ResourceFixture.name] as a [Path]. */
public val ResourceFixture<*>.fileName: Path get() = Paths.get(name)

/** Returns an [InputStream] for reading the [ResourceFixture.contents] of this [ResourceFixture]. */
public fun ResourceFixture<*>.inputStream(): InputStream = bytes.inputStream()

/** Returns a [StringReader] for reading the [TextResourceFixture.contents] of this [ResourceFixture]. */
public fun TextResourceFixture.reader(): StringReader = contents.reader()

/**
 * Copies this [ResourceFixture] to the specified [target].
 *
 * @see copyToDirectory
 * @see copyToTempFile
 */
public fun ResourceFixture<*>.copyTo(
    target: Path,
    overwrite: Boolean = false,
): Path =
    if (overwrite) target.useBufferedOutputStream { output -> inputStream().copyTo(output) }
    else target.useBufferedOutputStream(CREATE_NEW, WRITE) { output -> inputStream().copyTo(output) }

/**
 * Copies this [ResourceFixture] to the specified [target] directory.
 *
 * @see copyTo
 * @see copyToTempFile
 */
public fun ResourceFixture<*>.copyToDirectory(
    target: Path,
    overwrite: Boolean = false,
    createDirectories: Boolean = false,
): Path {
    if (createDirectories) target.createDirectories()
    return copyTo(target.resolve(name), overwrite)
}

/**
 * Copies this [ResourceFixture] to a new file in the default temp directory, using
 * the given [prefix] and [suffix] to generate its name.
 *
 * @see copyTo
 * @see copyToDirectory
 */
public fun ResourceFixture<*>.copyToTempFile(
    prefix: String = fileName.nameWithoutExtension.let { "$it." },
    suffix: String? = fileName.extension.takeUnless { it.isEmpty() }?.let { ".$it" },
): Path = createTempFile(prefix, suffix).useBufferedOutputStream { output -> inputStream().copyTo(output) }

/**
 * Copies this [ResourceFixture] to a new file in the default temp directory, using
 * the given [prefix] and [suffix] to generate its name,
 * the specified [ResourceFixture.contents] encoded using UTF-8 or the specified [charset].
 *
 * @see copyTo
 * @see copyToDirectory
 */
public fun TextResourceFixture.copyToTempTextFile(
    charset: Charset = Charsets.UTF_8,
    prefix: String = fileName.nameWithoutExtension.let { "$it." },
    suffix: String? = fileName.extension.takeUnless { it.isEmpty() }?.let { ".$it" },
): Path = createTempFile(prefix, suffix).useBufferedWriter(charset = charset) { output -> reader().copyTo(output) }


/**
 * Copies a [SvgImageFixture] to
 * the specified [fileName] (default: "kommons.svg") in this directory.
 */
public fun Path.createAnyFile(fileName: String? = null): Path =
    SvgImageFixture.let { it.copyTo(resolve(fileName ?: it.name)) }

/**
 * Copies one of the specified [fixtures]
 * (default: [GifImageFixture], [HtmlDocumentFixture], [SvgImageFixture], [UnicodeTextDocumentFixture])
 * to this directory.
 */
public fun Path.createRandomFile(
    vararg fixtures: ResourceFixture<*> = arrayOf(GifImageFixture, EmojiTextDocumentFixture, SvgImageFixture, UnicodeTextDocumentFixture),
): Path = fixtures.random().let { it.copyTo(resolve(it.name)) }

/**
 * Copies one of the specified [fixtures]
 * (default: [GifImageFixture], [HtmlDocumentFixture], [SvgImageFixture], [UnicodeTextDocumentFixture])
 * to the specified [fileName] in this directory.
 */
public fun Path.createRandomFile(
    fileName: String,
    vararg fixtures: ResourceFixture<*> = arrayOf(GifImageFixture, EmojiTextDocumentFixture, SvgImageFixture, UnicodeTextDocumentFixture),
): Path = fixtures.random().copyTo(resolve(fileName))

/**
 * Creates a new directory in the directory specified by this path, using
 * the given [prefix] to generate its name.
 *
 * The directory will contain
 * a [GifImageFixture], a [SvgImageFixture],
 * and a directory with the name `docs` containing
 * a [HtmlDocumentFixture] and a [UnicodeTextDocumentFixture].
 */
public fun Path.createDirectoryWithFiles(
    prefix: String? = null, vararg attributes: FileAttribute<*>
): Path = createTempDirectory(prefix, *attributes).apply {
    GifImageFixture.copyToDirectory(this)
    SvgImageFixture.copyToDirectory(this)
    resolve("docs").createDirectories().apply {
        EmojiTextDocumentFixture.copyToDirectory(this)
        UnicodeTextDocumentFixture.copyToDirectory(this)
    }
}

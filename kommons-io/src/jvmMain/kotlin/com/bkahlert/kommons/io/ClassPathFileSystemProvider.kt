package com.bkahlert.kommons.io

import com.bkahlert.kommons.Program
import com.bkahlert.kommons.text.withPrefix
import java.net.URI
import java.net.URL
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SeekableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.AccessMode
import java.nio.file.CopyOption
import java.nio.file.DirectoryStream
import java.nio.file.DirectoryStream.Filter
import java.nio.file.FileStore
import java.nio.file.FileSystem
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.NoSuchFileException
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.ReadOnlyFileSystemException
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileAttributeView
import java.nio.file.spi.FileSystemProvider
import kotlin.io.path.readBytes
import kotlin.reflect.KClass

/**
 * A file system provider that loads class path resource identified
 * by the `classpath` URI scheme, e.g. `classpath:file.txt`.
 */
public class ClassPathFileSystemProvider : FileSystemProvider() {

    /** Returns URI scheme that identifies this file system provider. */
    override fun getScheme(): String = URI_SCHEME

    /** Creating new classpath file system isn't supported and results in an [UnsupportedOperationException]. */
    override fun newFileSystem(uri: URI, env: MutableMap<String, *>): FileSystem = throw UnsupportedOperationException()

    /** Returns the file system of the located class path resource. */
    override fun getFileSystem(uri: URI): FileSystem = getPath(uri).fileSystem

    // TODO return read-only
    /** Returns the located class path resource. */
    override fun getPath(uri: URI): Path {
        val scheme: String? = uri.scheme
        return if (scheme != null && scheme.equals(URI_SCHEME, ignoreCase = true)) {
            val name = uri.toString().substring(URI_SCHEME.length + 1)
            val url = getResource(name)
            when (val resourceSchema = url.protocol) {
                defaultScheme -> defaultFileSystemProvider.getPath(url.toURI())
                jarScheme -> try {
                    jarFileSystemProvider.getPath(url.toURI())
                } catch (e: FileSystemNotFoundException) {
                    jarFileSystemProvider.newFileSystem(url.toURI(), emptyMap<String, Nothing>()).getPath(url.toString().substringAfter("!"))
                }

                else -> throw IllegalArgumentException("Resource scheme $resourceSchema is not supported")
            }
        } else {
            throw IllegalArgumentException("URI scheme is not $scheme")
        }
    }

    /** Opens a file **read-only**, returning a seekable byte channel to access the file. */
    override fun newByteChannel(path: Path, options: MutableSet<out OpenOption>, vararg attrs: FileAttribute<*>): SeekableByteChannel =
        Files.newByteChannel(requireSupportedPath(path), mutableSetOf(*options.toTypedArray(), StandardOpenOption.READ), *attrs)

    /** Opens a directory, returning a directory stream to iterate over the entries in the specified [dir]. */
    override fun newDirectoryStream(dir: Path, filter: Filter<in Path>): DirectoryStream<Path> =
        Files.newDirectoryStream(requireSupportedPath(dir), filter)

    /** Creating new classpath directory isn't permitted and results in an [ReadOnlyFileSystemException]. */
    override fun createDirectory(dir: Path, vararg attrs: FileAttribute<*>) {
        requireSupportedPath(dir)
        throw ReadOnlyFileSystemException()
    }

    /** Deleting a classpath file isn't permitted and results in an [ReadOnlyFileSystemException]. */
    override fun delete(path: Path) {
        requireSupportedPath(path)
        throw ReadOnlyFileSystemException()
    }

    /** Copies a file to a target file. */
    override fun copy(source: Path, target: Path, vararg options: CopyOption): Unit =
        Unit.also { Files.copy(requireSupportedPath(source), target, *options) }

    /** Moving a classpath file isn't permitted and results in an [ReadOnlyFileSystemException]. */
    override fun move(source: Path, target: Path, vararg options: CopyOption) {
        requireSupportedPath(source)
        throw ReadOnlyFileSystemException()
    }

    /** Tests if two paths locate the same file. */
    override fun isSameFile(path: Path, path2: Path): Boolean =
        Files.isSameFile(requireSupportedPath(path), path2)

    /** Tells whether a file is considered hidden. */
    override fun isHidden(path: Path): Boolean =
        Files.isHidden(requireSupportedPath(path))

    /** Returns the file store representing the file store where a file is located. */
    override fun getFileStore(path: Path): FileStore =
        Files.getFileStore(requireSupportedPath(path))

    /**
     * Checks the existence, and optionally the accessibility, of a file.
     *
     * Writing classpath files isn't permitted and results in an [ReadOnlyFileSystemException]
     * if the specified [modes] contain [AccessMode.WRITE].
     */
    override fun checkAccess(path: Path, vararg modes: AccessMode): Unit =
        requireSupportedProvider(path)
            .also { if (AccessMode.WRITE in modes) throw ReadOnlyFileSystemException() }
            .checkAccess(path, *modes)

    /** Returns a file attribute view of a given type. */
    override fun <V : FileAttributeView> getFileAttributeView(path: Path, type: Class<V>, vararg options: LinkOption): V? =
        Files.getFileAttributeView(requireSupportedPath(path), type, *options)

    /** Reads a file's attributes as a bulk operation. */
    override fun <A : BasicFileAttributes> readAttributes(path: Path, type: Class<A>, vararg options: LinkOption): A =
        Files.readAttributes(requireSupportedPath(path), type, *options)

    /** Reads a set of file attributes as a bulk operation. */
    override fun readAttributes(path: Path, attributes: String, vararg options: LinkOption): MutableMap<String, Any?> =
        Files.readAttributes(requireSupportedPath(path), attributes, *options)

    /** Setting attributes isn't supported and results in an [ReadOnlyFileSystemException]. */
    override fun setAttribute(path: Path, attribute: String, value: Any?, vararg options: LinkOption) {
        requireSupportedPath(path)
        throw ReadOnlyFileSystemException()
    }

    public companion object {
        /** The URI scheme that identifies this file system provider. */
        public const val URI_SCHEME: String = "classpath"

        private val defaultFileSystem: FileSystem by lazy { FileSystems.getDefault() }
        private val defaultFileSystemProvider: FileSystemProvider by lazy { defaultFileSystem.provider() }
        private val defaultScheme: String by lazy { defaultFileSystemProvider.scheme }
        private const val jarScheme: String = "jar"
        private val jarFileSystemProvider by lazy {
            installedProviders().firstOrNull { it.scheme.equals(jarScheme, ignoreCase = true) }
                ?: error("Required file system provider for URI scheme $jarScheme missing")
        }

        private val supportedSchema by lazy { listOf(defaultScheme, jarScheme) }

        /** Returns the specified [path] if it's supported or throws an [IllegalArgumentException] otherwise. */
        private fun requireSupportedPath(path: Path): Path {
            val scheme = path.toUri().scheme
            require(scheme in supportedSchema) {
                "URI scheme $scheme is not supported. Only paths returned by ${ClassPathFileSystemProvider::getPath.name} are supported."
            }
            return path
        }

        /** Returns the provider of the specified [path] if it's supported or throws an [IllegalArgumentException] otherwise. */
        private fun requireSupportedProvider(path: Path): FileSystemProvider = requireSupportedPath(path).fileSystem.provider()

        /** Loads the resource with the specified name or throws a [NoSuchFileException] otherwise. */
        private fun getResource(name: String): URL {
            val contextClassLoaderResource = Program.contextClassLoader.getResource(name)
            if (contextClassLoaderResource != null) return contextClassLoaderResource

            if (name.endsWith(".class")) {
                val fqn = name.removePrefix("/").removeSuffix(".class").replace('/', '.')
                val simpleName = fqn.substringAfterLast('.')
                val ownClassLoaderResource = kotlin.runCatching { Class.forName(fqn).getResource("$simpleName.class") }.getOrNull()
                if (ownClassLoaderResource != null) return ownClassLoaderResource
            }

            throw NoSuchFileException(name, null, "$name could not be found")
        }
    }
}

/**
 * Returns a [Path] that points to the specified [resource].
 *
 * The explicit `classpath:` schema is optional.
 */
@Suppress("FunctionName")
public fun ClassPath(resource: String): Path =
    Paths.get(URI(resource.withPrefix(ClassPathFileSystemProvider.URI_SCHEME + ":")))


/** Constructs a new file system identified by the specified [uri]. */
internal fun FileSystemProvider.newFileSystem(uri: URI): FileSystem =
    newFileSystem(uri, mutableMapOf<String, Any?>())

/** Opens or creates a file, returning a seekable byte channel to access the file. */
internal fun FileSystemProvider.newByteChannel(path: Path, vararg attrs: FileAttribute<*>): SeekableByteChannel =
    newByteChannel(path, mutableSetOf(), *attrs)

/** Opens a directory, returning a directory stream to iterate over the entries in the specified [dir]. */
internal fun FileSystemProvider.newDirectoryStream(dir: Path): DirectoryStream<Path> =
    newDirectoryStream(dir) { true }

/** Returns a file attribute view of a given type. */
internal fun <V : FileAttributeView> FileSystemProvider.getFileAttributeView(path: Path, type: KClass<V>, vararg options: LinkOption): V? =
    getFileAttributeView(path, type.java, *options)

/** Reads a set of file attributes as a bulk operation. */
internal fun <A : BasicFileAttributes> FileSystemProvider.readAttributes(path: Path, type: KClass<A>, vararg options: LinkOption): A =
    readAttributes(path, type.java, *options)


/**
 * Constructs a stream that reads bytes from the given channel.
 * @see Channels.newInputStream
 */
internal fun ReadableByteChannel.newInputStream() =
    Channels.newInputStream(this)

/**
 * Constructs a stream that writes bytes to the given channel.
 * @see Channels.newOutputStream
 */
internal fun WritableByteChannel.newOutputStream() =
    Channels.newOutputStream(this)

/**
 * Reads this byte channel completely into a byte array.
 * @see Channels.newInputStream
 * @see readBytes
 */
internal fun ReadableByteChannel.readBytes(): ByteArray =
    newInputStream().use { it.readBytes() }

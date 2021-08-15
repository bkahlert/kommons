package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.io.path.ReadOnlyFileSystem.Companion.privateAsReadyOnly
import com.bkahlert.kommons.io.path.ReadOnlyFileSystemProvider.Companion.privateAsReadyOnly
import com.bkahlert.kommons.io.path.ReadOnlyPath.Companion.privateAsReadyOnly
import java.io.IOException
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.file.AccessMode
import java.nio.file.CopyOption
import java.nio.file.DirectoryStream
import java.nio.file.FileStore
import java.nio.file.FileSystem
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.ReadOnlyFileSystemException
import java.nio.file.StandardOpenOption
import java.nio.file.WatchService
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileAttributeView
import java.nio.file.attribute.UserPrincipalLookupService
import java.nio.file.spi.FileSystemProvider

/**
 * Wraps this file system provider in order to make it read-only.
 */
public fun FileSystemProvider.asReadOnly(): FileSystemProvider = privateAsReadyOnly()

/**
 * Wraps this file system in order to make it read-only.
 */
public fun FileSystem.asReadOnly(): FileSystem = privateAsReadyOnly()

/**
 * Wraps this path in order to make it read-only.
 */
public fun Path.asReadOnly(): Path = privateAsReadyOnly()

private val Path.wrappedPath: Path get() = if (this is WrappedPath) wrappedPath.wrappedPath else this

internal interface WrappedPath {
    val wrappedPath: Path
}

private class ReadOnlyFileSystemProvider private constructor(private val provider: FileSystemProvider) : FileSystemProvider() {
    companion object {
        fun FileSystemProvider.privateAsReadyOnly(): ReadOnlyFileSystemProvider =
            if (this is ReadOnlyFileSystemProvider) this
            else ReadOnlyFileSystemProvider(this)
    }

    override fun getScheme(): String = provider.scheme
    override fun newFileSystem(uri: URI?, env: MutableMap<String, *>?): FileSystem =
        provider.newFileSystem(uri, env).asReadOnly()

    override fun getFileSystem(uri: URI?): FileSystem =
        provider.getFileSystem(uri).asReadOnly()

    override fun getPath(uri: URI): Path =
        provider.getPath(uri).asReadOnly()

    override fun newByteChannel(path: Path?, options: MutableSet<out OpenOption>?, vararg attrs: FileAttribute<*>?): SeekableByteChannel? =
        if ((options ?: emptySet()).all { it == StandardOpenOption.READ }) {
            provider.newByteChannel(path?.wrappedPath, options, *attrs)
        } else throw ReadOnlyFileSystemException()

    override fun newDirectoryStream(dir: Path?, predicate: DirectoryStream.Filter<in Path>?): DirectoryStream<Path> =
        object : DirectoryStream<Path> {
            val stream = provider.newDirectoryStream(dir?.wrappedPath, predicate)

            override fun iterator(): MutableIterator<Path> =
                stream.iterator().asSequence().map { it.asReadOnly() }.toMutableList().listIterator()

            override fun close() = stream.close()
        }

    override fun createDirectory(dir: Path?, vararg attrs: FileAttribute<*>?) =
        throw ReadOnlyFileSystemException()

    override fun delete(path: Path?): Unit =
        throw ReadOnlyFileSystemException()

    override fun copy(source: Path?, target: Path?, vararg options: CopyOption?) =
        provider.copy(source?.wrappedPath, target)

    override fun move(source: Path?, target: Path?, vararg options: CopyOption?): Unit =
        throw ReadOnlyFileSystemException()

    override fun isSameFile(path: Path?, path2: Path?): Boolean =
        provider.isSameFile(path?.wrappedPath, path2?.wrappedPath)

    override fun isHidden(path: Path?): Boolean =
        provider.isHidden(path)

    override fun getFileStore(path: Path?): FileStore =
        provider.getFileStore(path?.wrappedPath)

    override fun checkAccess(path: Path?, vararg modes: AccessMode?) =
        if (modes.contains(AccessMode.WRITE)) throw IOException(ReadOnlyFileSystemException())
        else provider.checkAccess(path?.wrappedPath)

    override fun <V : FileAttributeView?> getFileAttributeView(path: Path?, type: Class<V>?, vararg options: LinkOption?): V =
        provider.getFileAttributeView(path?.wrappedPath, type, *options)

    override fun <A : BasicFileAttributes?> readAttributes(path: Path?, type: Class<A>?, vararg options: LinkOption?): A =
        provider.readAttributes(path?.wrappedPath, type)

    override fun readAttributes(path: Path?, attributes: String?, vararg options: LinkOption?): MutableMap<String, Any> =
        provider.readAttributes(path?.wrappedPath, attributes, *options)

    override fun setAttribute(path: Path?, attribute: String?, value: Any?, vararg options: LinkOption?): Unit =
        throw ReadOnlyFileSystemException()

    override fun toString(): String = "$provider"
}

private class ReadOnlyFileSystem private constructor(private val fileSystem: FileSystem) : FileSystem() {
    companion object {
        fun FileSystem.privateAsReadyOnly(): ReadOnlyFileSystem =
            if (this is ReadOnlyFileSystem) this
            else ReadOnlyFileSystem(this)
    }

    override fun close() = throw ReadOnlyFileSystemException()
    override fun provider(): FileSystemProvider = fileSystem.provider().asReadOnly()
    override fun isOpen(): Boolean = fileSystem.isOpen
    override fun isReadOnly(): Boolean = true
    override fun getSeparator(): String = fileSystem.separator
    override fun getRootDirectories(): MutableIterable<Path> = fileSystem.rootDirectories.map { it.asReadOnly() }.toMutableList()
    override fun getFileStores(): MutableIterable<FileStore> = fileSystem.fileStores
    override fun supportedFileAttributeViews(): MutableSet<String> = fileSystem.supportedFileAttributeViews()
    override fun getPath(first: String, vararg more: String?): Path = fileSystem.getPath(first, *more).asReadOnly()
    override fun getPathMatcher(syntaxAndPattern: String?): PathMatcher = fileSystem.getPathMatcher(syntaxAndPattern)
    override fun getUserPrincipalLookupService(): UserPrincipalLookupService = fileSystem.userPrincipalLookupService
    override fun newWatchService(): WatchService = fileSystem.newWatchService()
    override fun toString(): String = "$fileSystem"
}

private class ReadOnlyPath private constructor(override val wrappedPath: Path) : WrappedPath, Path by wrappedPath {
    companion object {
        fun Path.privateAsReadyOnly(): ReadOnlyPath =
            if (this is ReadOnlyPath) this
            else ReadOnlyPath(this)
    }

    override fun getParent(): Path = wrappedPath.parent.asReadOnly()
    override fun iterator(): MutableIterator<Path> = wrappedPath.asSequence().map { it.asReadOnly() }.toMutableList().listIterator()
    override fun getRoot(): Path = wrappedPath.root.asReadOnly()
    override fun getFileName(): Path = wrappedPath.fileName.asReadOnly()
    override fun getName(index: Int): Path = wrappedPath.getName(index).asReadOnly()
    override fun subpath(beginIndex: Int, endIndex: Int): Path = wrappedPath.subpath(beginIndex, endIndex).asReadOnly()
    override fun normalize(): Path = wrappedPath.normalize().asReadOnly()
    override fun resolve(other: Path): Path = wrappedPath.resolve(other.wrappedPath).asReadOnly()
    override fun resolve(other: String): Path = wrappedPath.resolve(other).asReadOnly()
    override fun resolveSibling(other: Path): Path = wrappedPath.resolveSibling(other.wrappedPath).asReadOnly()
    override fun resolveSibling(other: String): Path = wrappedPath.resolveSibling(other).asReadOnly()
    override fun relativize(other: Path): Path = wrappedPath.relativize(other.wrappedPath).asReadOnly()
    override fun toAbsolutePath(): Path = wrappedPath.toAbsolutePath().asReadOnly()
    override fun toRealPath(vararg options: LinkOption?): Path = wrappedPath.toRealPath().asReadOnly()
    override fun getFileSystem(): FileSystem = wrappedPath.fileSystem.asReadOnly()
    override fun toString(): String = "$wrappedPath"

    val serialized: String get() = throw UnsupportedOperationException("Unsupported to avoid accidental write access.")
}

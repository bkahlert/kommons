package com.bkahlert.kommons.autoconfigure

import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.boot.configurationprocessor.MetadataStore
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.net.URI
import java.nio.file.Paths
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.tools.FileObject
import javax.tools.JavaFileManager.Location
import javax.tools.JavaFileObject

/**
 * Utility that can instantiate [MetadataStore] and therefor give access to the metadata created by
 * [ConfigurationMetadataAnnotationProcessor] (e.g. `spring-configuration-metadata.json`).
 *
 * @author Björn Kahlert
 */
object MetadataStoreReader {
    private fun mockProcessingEnvironment(): ProcessingEnvironment {
        val processingEnvironment: ProcessingEnvironment = mock(ProcessingEnvironment::class.java)
        `when`(processingEnvironment.filer).thenReturn(ClassPathFiler())
        return processingEnvironment
    }

    val metadataStore: MetadataStore get() = MetadataStore(mockProcessingEnvironment())

    private class ClassPathFiler : Filer {
        var classPath = Paths.get("target", "classes")
        override fun createSourceFile(name: CharSequence, vararg originatingElements: Element): JavaFileObject {
            return unsupported()
        }

        override fun createClassFile(name: CharSequence, vararg originatingElements: Element): JavaFileObject {
            return unsupported()
        }

        override fun createResource(
            location: Location, moduleAndPkg: CharSequence, relativeName: CharSequence, vararg originatingElements: Element,
        ): FileObject {
            return unsupported()
        }

        override fun getResource(location: Location, moduleAndPkg: CharSequence, relativeName: CharSequence): FileObject {
            return fileObjectFor(File(classPath.toAbsolutePath().toFile(), relativeName.toString()))
        }

        private class FileBackedFileObject(
            private val file: File,
        ) : FileObject {
            override fun toUri(): URI {
                return unsupported()
            }

            override fun getName(): String = unsupported()

            override fun openInputStream(): InputStream {
                return DataInputStream(FileInputStream(file))
            }

            override fun openOutputStream(): OutputStream {
                return unsupported()
            }

            override fun openReader(ignoreEncodingErrors: Boolean): Reader {
                return unsupported()
            }

            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
                return unsupported()
            }

            override fun openWriter(): Writer {
                return unsupported()
            }

            override fun getLastModified(): Long = unsupported()

            override fun delete(): Boolean {
                return unsupported()
            }
        }

        companion object {
            fun <T> unsupported(): T {
                throw UnsupportedOperationException("This method is not implemented.")
            }

            fun fileObjectFor(file: File): FileObject {
                return FileBackedFileObject(file)
            }
        }
    }
}

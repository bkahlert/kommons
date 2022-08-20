package com.bkahlert.kommons.logging.spring

import com.bkahlert.kommons.Program
import com.bkahlert.kommons.logging.LoggingPreset
import com.bkahlert.kommons.logging.spring.LoggingProperties.PresetProperties
import com.bkahlert.kommons.test.testAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainInOrder
import org.junit.jupiter.api.Test
import org.springframework.boot.configurationprocessor.MetadataStore
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata
import org.springframework.util.ClassUtils
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.Writer
import java.net.URI
import java.net.URL
import java.util.Locale
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.FileObject
import javax.tools.JavaFileManager.Location
import javax.tools.JavaFileObject
import javax.tools.StandardLocation.CLASS_OUTPUT
import kotlin.reflect.KClass

class ConfigurationMetadataIntegrationTest {

    @Test fun logging_preset_console() = testAll {
        configuredProperties.forAny {
            it.name shouldBe LoggingProperties.CONSOLE_LOG_PRESET_PROPERTY
            it.kClass shouldBe LoggingPreset::class
            it.description.shouldContainInOrder("Preset", "CONSOLE log")
            it.sourceKClass shouldBe PresetProperties::class
        }
    }

    @Test fun logging_preset_file() = testAll {
        configuredProperties.forAny {
            it.name shouldBe LoggingProperties.FILE_LOG_PRESET_PROPERTY
            it.kClass shouldBe LoggingPreset::class
            it.description.shouldContainInOrder("Preset", "FILE log")
            it.sourceKClass shouldBe PresetProperties::class
        }
    }

    companion object {
        val configuredProperties: List<ItemMetadata>
            get() = MetadataStore(FileReadOnlyProcessingEnvironment()).readMetadata().items
    }
}

val ItemMetadata.kClass: KClass<*>?
    get() = type?.let { ClassUtils.resolveClassName(it, null)?.kotlin }

val ItemMetadata.sourceKClass: KClass<*>?
    get() = sourceType?.let { ClassUtils.resolveClassName(it, null)?.kotlin }

class FileReadOnlyProcessingEnvironment : ProcessingEnvironment {
    override fun getOptions(): MutableMap<String, String> = throwUnsupportedOperationException()
    override fun getMessager(): Messager = throwUnsupportedOperationException()
    override fun getFiler(): Filer = ReadOnlyFiler()
    override fun getElementUtils(): Elements = throwUnsupportedOperationException()
    override fun getTypeUtils(): Types = throwUnsupportedOperationException()
    override fun getSourceVersion(): SourceVersion = throwUnsupportedOperationException()
    override fun getLocale(): Locale = throwUnsupportedOperationException()
    private fun throwUnsupportedOperationException(): Nothing {
        throw UnsupportedOperationException("This implementation only allows reading files.")
    }
}

class ReadOnlyFiler : Filer {
    override fun createSourceFile(name: CharSequence, vararg originatingElements: Element): JavaFileObject = throwUnsupportedOperationException()
    override fun createClassFile(name: CharSequence, vararg originatingElements: Element): JavaFileObject = throwUnsupportedOperationException()
    override fun createResource(location: Location, pkg: CharSequence, relativeName: CharSequence, vararg originatingElements: Element): FileObject =
        throwUnsupportedOperationException()

    override fun getResource(location: Location, pkg: CharSequence, relativeName: CharSequence): FileObject {
        check(location == CLASS_OUTPUT)
        val name = relativeName.toString()
        val resource = checkNotNull(Program.contextClassLoader.getResource(name))
        return ReadOnlyFileObject(resource, name)
    }

    private fun throwUnsupportedOperationException(): Nothing {
        throw UnsupportedOperationException("This implementation only allows read access.")
    }
}

class ReadOnlyFileObject(
    private val resource: URL,
    private val name: String
) : FileObject {
    override fun toUri(): URI = resource.toURI()
    override fun getName(): String = name
    override fun openInputStream(): InputStream = resource.openStream()
    override fun openOutputStream(): OutputStream = throwUnsupportedOperationException()
    override fun openReader(ignoreEncodingErrors: Boolean): Reader = openInputStream().reader()
    override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = openReader(ignoreEncodingErrors).readText()
    override fun openWriter(): Writer = throwUnsupportedOperationException()
    override fun getLastModified(): Long = Long.MIN_VALUE
    override fun delete(): Boolean = false
    private fun throwUnsupportedOperationException(): Nothing {
        throw UnsupportedOperationException("This implementation only allows read access.")
    }
}

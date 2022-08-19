package com.bkahlert.logging.support

import com.bkahlert.logging.logback.LogbackConfiguration.Encoder
import com.bkahlert.logging.support.LogbackConfigurationExtension.LogbackTestConfiguration
import com.bkahlert.logging.support.LogbackUtil.isLogbackDebugMode
import com.bkahlert.logging.support.LogbackUtil.loadConfigurationStream
import com.bkahlert.logging.support.LogbackUtil.reset
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import org.junit.platform.commons.support.AnnotationSupport
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * This extensionwhen annotated to a classtemporarily reconfigures Logback according
 * to [LogbackTestConfiguration].<br></br>
 * The previous state will be restored after the test finished.
 *
 * @author Björn Kahlert
 */
class LogbackConfigurationExtension : BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        refresh(context)
    }

    override fun afterEach(context: ExtensionContext?) {
        reset()
    }

    private enum class LogbackPropertyFormatter(
        val getValue: (LogbackTestConfiguration) -> String,

        ) {
        CONSOLE_APPENDER({ config -> config.CONSOLE_APPENDER.name }),
        FILE_APPENDER({ config -> config.FILE_APPENDER.name });

        private fun format(config: LogbackTestConfiguration): String {
            return "<property name=\"" + name + "\" value=\"" + getValue(config) + "\"/>"
        }

        companion object {
            fun fromContext(context: ExtensionContext): List<String> =
                AnnotationSupport.findAnnotation(context.element, LogbackTestConfiguration::class.java).map { config ->
                    values().map { it.format(config) }
                }.orElseGet { emptyList() }
        }
    }

    /**
     * Annotation to specify the configuration
     */
    @ResourceLock(Resources.SYSTEM_PROPERTIES, mode = READ_WRITE)
    @Retention(AnnotationRetention.RUNTIME)
    @Target(FUNCTION)
    @ExtendWith(LogbackConfigurationExtension::class)
    annotation class LogbackTestConfiguration(
        val CONSOLE_APPENDER: Encoder = Encoder.preset,
        val FILE_APPENDER: Encoder = Encoder.preset,
    )

    companion object {
        fun getConfigAsStream(properties: Collection<String>): InputStream {
            return ByteArrayInputStream(
                Stream.of(
                    Stream.of(
                        "<configuration debug=\"$isLogbackDebugMode\">"
                    ),
                    properties.stream(),
                    Stream.of(
                        "    <property name=\"LOG_PATH\" value=\"" + Files.createTempDirectory(LogbackConfigurationExtension::class.java.simpleName) + "\"/>",
                        "    <include resource=\"com/bkahlert/kommons/logging/logback/base.xml\"/>",
                        "</configuration>"
                    )
                ).flatMap(Function.identity()).collect(Collectors.joining("\n")).toByteArray(StandardCharsets.UTF_8)
            )
        }

        fun refresh(extensionContext: ExtensionContext) {
            reset()
            val properties = LogbackPropertyFormatter.fromContext(extensionContext)
            loadConfigurationStream(getConfigAsStream(properties))
        }
    }
}

package com.bkahlert.kommons.debug

import com.bkahlert.kommons.createJarAndResolve
import com.bkahlert.kommons.test.createAnyFile
import com.bkahlert.kommons.test.junit.SystemProperty
import io.kotest.assertions.throwables.shouldNotThrowAny
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.div

class DesktopKtTest {

    @SystemProperty("java.awt.headless", "true")
    @Test fun open(@TempDir tempDir: Path) {
        listOf(
            (tempDir / "dir").createDirectory(),
            tempDir.createAnyFile(),
            tempDir / "non-existing",
        ).forEach { path ->
            shouldNotThrowAny { path.toUri().toURL().open() }
            shouldNotThrowAny { path.toUri().open() }
            shouldNotThrowAny { path.open() }
            shouldNotThrowAny { path.toFile().open() }
        }

        shouldNotThrowAny { URL("https://example.com/path").open() }
        shouldNotThrowAny { tempDir.createJarAndResolve().toUri().open() }
        shouldNotThrowAny { tempDir.createJarAndResolve().open() }
    }

    @SystemProperty("java.awt.headless", "true")
    @Test fun locate(@TempDir tempDir: Path) {
        listOf(
            (tempDir / "dir").createDirectory(),
            tempDir.createAnyFile(),
            tempDir / "non-existing",
        ).forEach { path ->
            shouldNotThrowAny { path.toUri().toURL().locate() }
            shouldNotThrowAny { path.toUri().locate() }
            shouldNotThrowAny { path.locate() }
            shouldNotThrowAny { path.toFile().locate() }
        }

        shouldNotThrowAny { URL("https://example.com/path").locate() }
        shouldNotThrowAny { tempDir.createJarAndResolve().toUri().locate() }
        shouldNotThrowAny { tempDir.createJarAndResolve().locate() }
    }
}

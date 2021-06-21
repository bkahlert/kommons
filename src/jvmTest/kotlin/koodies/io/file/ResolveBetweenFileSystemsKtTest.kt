package koodies.io.file

import koodies.io.randomDirectory
import koodies.io.randomFile
import koodies.junit.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path

class ResolveBetweenFileSystemsKtTest {

    @Nested
    inner class WithSameFileSystem {

        @Test
        fun `should return relative jar path resolved against jar path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            tempJarFileSystem().use { jarFileSystem ->
                val receiverJarPath: Path = jarFileSystem.rootDirectories.first().randomDirectory().randomDirectory()
                val relativeJarPath: Path = receiverJarPath.parent.relativize(receiverJarPath)
                expectThat(receiverJarPath.resolveBetweenFileSystems(relativeJarPath))
                    .isEqualTo(receiverJarPath.resolve(receiverJarPath.last()))
            }
        }

        @Test
        fun `should return relative file path resolved against file path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val receiverFilePath = randomDirectory().randomDirectory()
            val relativeFilePath: Path = receiverFilePath.parent.relativize(receiverFilePath)
            expectThat(receiverFilePath.resolveBetweenFileSystems(relativeFilePath))
                .isEqualTo(receiverFilePath.resolve(receiverFilePath.last()))
        }
    }

    @Nested
    inner class WithAbsoluteOtherPath {

        @Test
        fun `should return absolute jar path resolved against jar path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            tempJarFileSystem().use { jarFileSystem ->
                val receiverJarPath: Path = jarFileSystem.rootDirectories.first().randomDirectory().randomFile()
                val absoluteJarPath: Path = jarFileSystem.rootDirectories.first()
                expectThat(receiverJarPath.resolveBetweenFileSystems(absoluteJarPath)).isEqualTo(absoluteJarPath)
            }
        }

        @Test
        fun `should return absolute jar path resolved against file path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val receiverFilePath: Path = randomDirectory().randomFile()
            tempJarFileSystem().use { jarFileSystem ->
                val absoluteJarPath: Path = jarFileSystem.rootDirectories.first()
                expectThat(receiverFilePath.resolveBetweenFileSystems(absoluteJarPath)).isEqualTo(absoluteJarPath)
            }
        }

        @Test
        fun `should return absolute file path resolved against jar path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val otherFileAbsPath: Path = randomDirectory()
            tempJarFileSystem().use { jarFileSystem ->
                val receiverJarPath: Path = jarFileSystem.rootDirectories.first().randomDirectory().randomFile()
                expectThat(receiverJarPath.resolveBetweenFileSystems(otherFileAbsPath)).isEqualTo(otherFileAbsPath)
            }
        }

        @Test
        fun `should return absolute file path resolved against file path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val receiverFilePath = randomDirectory().randomFile()
            val otherFileAbsPath: Path = randomDirectory()
            expectThat(receiverFilePath.resolveBetweenFileSystems(otherFileAbsPath)).isEqualTo(otherFileAbsPath)
        }
    }


    @Nested
    inner class WithRelativeOtherPath {

        @Test
        fun `should return file path on relative jar path resolved against file path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val receiverFilePath: Path = randomDirectory().randomFile()
            tempJarFileSystem().use { jarFileSystem ->
                val relativeJarPath: Path = jarFileSystem.rootDirectories.first().randomDirectory().randomFile()
                    .let { absPath -> absPath.parent.relativize(absPath) }
                expectThat(receiverFilePath.resolveBetweenFileSystems(relativeJarPath))
                    .isEqualTo(receiverFilePath.resolve(relativeJarPath.first().toString()))
            }
        }

        @Test
        fun `should return jar path on relative file path resolved against jar path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val relativeFilePath: Path = randomDirectory().randomFile()
                .let { absPath -> absPath.parent.relativize(absPath) }
            tempJarFileSystem().use { jarFileSystem ->
                val receiverJarPath: Path = jarFileSystem.rootDirectories.first().randomDirectory().randomFile()
                expectThat(receiverJarPath.resolveBetweenFileSystems(relativeFilePath))
                    .isEqualTo(receiverJarPath.resolve(relativeFilePath.first().toString()))
            }
        }
    }
}

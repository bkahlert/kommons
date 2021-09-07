package com.bkahlert.kommons.docker

import com.bkahlert.kommons.docker.TestImages.BusyBox
import com.bkahlert.kommons.test.IdeaWorkaroundTest
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isSuccess
import strikt.assertions.isTrue

class DockerTest {

    @Nested
    inner class IsInstalled {

        @Test
        fun `should not throw`() {
            expectCatching { Docker.isInstalled }.isSuccess()
        }
    }

    @DockerRequiring @Test
    fun `should return true if engine is running`() {
        expectThat(Docker.engineRunning).isTrue()
    }

    @Nested
    inner class InfoProperty {

        @DockerRequiring @Test
        fun `should return info for existing key`() {
            expectThat(Docker.info["server.server-version"]).toStringMatchesCurlyPattern("{}.{}.{}")
        }

        @DockerRequiring @Test
        fun `should return null for unknown key`() {
            expectThat(Docker.info["unknown.key"]).isNull()
        }
    }

    @Nested
    inner class ImageProperty {

        @Test
        fun `should build instances`() {
            expectThat(Docker.images { "hello-world" }).isEqualTo(TestImages.HelloWorld)
        }

        @Test
        fun `should provide commands`() {
            expectCatching { Docker.images.list() }.isSuccess()
        }
    }

    @Nested
    inner class ContainerProperty {

        @Test
        fun `should build instances`() {
            expectThat(Docker.containers { "docker-container".sanitized }).isEqualTo(DockerContainer("docker-container"))
        }

        @DockerRequiring @Test
        fun `should provide commands`() {
            expectCatching { Docker.containers.list() }.isSuccess()
        }
    }

    @Nested
    inner class ContainerRunning {

        @ContainersTest @IdeaWorkaroundTest
        fun `should return true if container is running`(testContainers: TestContainers) {
            val container = testContainers.newRunningTestContainer()
            expectThat(Docker(container.name).isRunning).isTrue()
        }

        @ContainersTest @IdeaWorkaroundTest
        fun `should return false if container exited`(testContainers: TestContainers) {
            val container = testContainers.newExitedTestContainer()
            expectThat(Docker(container.name).isRunning).isFalse()
        }

        @ContainersTest @IdeaWorkaroundTest
        fun `should return false if container does not exist`(testContainers: TestContainers) {
            val container = testContainers.newNotExistentContainer()
            expectThat(Docker(container.name).isRunning).isFalse()
        }
    }

    @DockerRequiring @Test
    fun `should search`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(Docker.search("busybox")).any { image.isEqualTo(BusyBox) }
    }
}

package com.bkahlert.kommons_deprecated.docker

import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons_deprecated.docker.TestImages.BusyBox
import com.bkahlert.kommons_deprecated.test.withTempDir
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.inspectors.forAny
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DockerTest {

    @Nested
    inner class IsInstalled {

        @Test
        fun `should not throw`() {
            shouldNotThrowAny { Docker.isInstalled }
        }
    }

    @DockerRequiring @Test
    fun `should return true if engine is running`() {
        Docker.engineRunning shouldBe true
    }

    @Nested
    inner class InfoProperty {

        @DockerRequiring @Test
        fun `should return info for existing key`() {
            Docker.info["server.server-version"].toString() shouldMatchGlob "*.*.*"
        }

        @DockerRequiring @Test
        fun `should return null for unknown key`() {
            Docker.info["unknown.key"] shouldBe null
        }
    }

    @Nested
    inner class ImageProperty {

        @Test
        fun `should build instances`() {
            Docker.images { "hello-world" } shouldBe TestImages.HelloWorld
        }

        @Test
        fun `should provide commands`() {
            shouldNotThrowAny { Docker.images.list() }
        }
    }

    @Nested
    inner class ContainerProperty {

        @Test
        fun `should build instances`() {
            Docker.containers { "docker-container".sanitized } shouldBe DockerContainer("docker-container")
        }

        @DockerRequiring @Test
        fun `should provide commands`() {
            shouldNotThrowAny { Docker.containers.list() }
        }
    }

    @Nested
    inner class ContainerRunning {

        @ContainersTest
        fun `should return true if container is running`(testContainers: TestContainers) {
            val container = testContainers.newRunningTestContainer()
            Docker(container.name).isRunning shouldBe true
        }

        @ContainersTest
        fun `should return false if container exited`(testContainers: TestContainers) {
            val container = testContainers.newExitedTestContainer()
            Docker(container.name).isRunning shouldBe false
        }

        @ContainersTest
        fun `should return false if container does not exist`(testContainers: TestContainers) {
            val container = testContainers.newNotExistentContainer()
            Docker(container.name).isRunning shouldBe false
        }
    }

    @DockerRequiring @Test
    fun `should search`(simpleId: SimpleId) = withTempDir(simpleId) {
        Docker.search("busybox").forAny { it.image shouldBe BusyBox }
    }
}

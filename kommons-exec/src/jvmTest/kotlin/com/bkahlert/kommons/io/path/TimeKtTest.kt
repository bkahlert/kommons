package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.Now
import com.bkahlert.kommons.io.lastModified
import com.bkahlert.kommons.minus
import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isLessThan
import strikt.java.exists
import kotlin.io.path.createFile
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class TimeKtTest {

    @Nested
    inner class Touch {

        @Test
        fun `should update last modified`(simpleId: SimpleId) = withTempDir(simpleId) {
            val file = resolve("file").apply {
                createFile()
                lastModified -= 1.days
            }

            file.touch()

            expectThat(Now.minus(file.lastModified.toInstant())).isLessThan(5.seconds)
        }

        @Test
        fun `should create file if missing`(simpleId: SimpleId) = withTempDir(simpleId) {
            val file = resolve("file")
            file.touch()
            expectThat(file).exists()
        }
    }
}

package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.lastModified
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.time.Now
import com.bkahlert.kommons.time.days
import com.bkahlert.kommons.time.minus
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isLessThan
import strikt.java.exists
import kotlin.io.path.createFile

class TimeKtTest {

    @Nested
    inner class Touch {

        @Test
        fun `should update last modified`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = resolve("file").apply {
                createFile()
                lastModified -= 1.days
            }

            file.touch()

            expectThat(Now.fileTime.toMillis() - file.lastModified.toMillis()).isLessThan(5000)
        }

        @Test
        fun `should create file if missing`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = resolve("file")
            file.touch()
            expectThat(file).exists()
        }
    }
}

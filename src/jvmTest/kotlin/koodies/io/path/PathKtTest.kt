package koodies.io.path

import koodies.junit.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThan
import strikt.assertions.isNotNull
import strikt.assertions.parent
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.time.Duration
import kotlin.time.minutes
import kotlin.time.seconds

@Execution(CONCURRENT)
class PathKtTest {

    @Nested
    inner class WithMissingDirectoriesCreated {

        @Test
        fun `should create missing directories`() = withTempDir {
            val file = resolve("some/dir/some/file")
            expectThat(file.withDirectoriesCreated()).parent.isNotNull().exists()
        }
    }

    @Nested
    inner class AgeKtTest {

        @Test
        fun `should read age`() = withTempDir {
            expectThat(randomFile()).age
                .isLessThan(1.seconds)
                .isGreaterThanOrEqualTo(Duration.ZERO)
        }

        @Test
        fun `should save age`() = withTempDir {
            val file = createFile()
            file.age = 20.minutes
            expectThat(file).age
                .isLessThan(20.minutes + 1.seconds)
                .isGreaterThanOrEqualTo(20.minutes)
        }
    }
}


val Assertion.Builder<out Path>.age get() = get { age }

fun <T : Path> Assertion.Builder<T>.hasAge(age: Duration) =
    assert("is $age old") {
        val actualAge = it.age
        when (actualAge == age) {
            true -> pass()
            else -> fail("is actually $actualAge old")
        }
    }

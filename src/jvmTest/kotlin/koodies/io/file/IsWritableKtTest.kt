package koodies.io.file

import strikt.api.Assertion
import java.nio.file.Files
import java.nio.file.Path

class IsWritableKtTest {
}


/**
 * Asserts that the subject is writable.
 *
 * @see Files.isWritable
 */
fun <T : Path> Assertion.Builder<T>.isWritable(): Assertion.Builder<T> =
    assertThat("is writable") { Files.isWritable(it) }

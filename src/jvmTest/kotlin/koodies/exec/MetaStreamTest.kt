package koodies.exec

import koodies.concurrent.process.IO.META
import koodies.concurrent.process.IO.META.FILE
import koodies.io.path.Locations
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isSuccess

class MetaStreamTest {

    private class Listener : (META) -> Unit {
        public var message: META? = null
        override fun invoke(message: META) {
            this.message = message
        }
    }

    private val message = FILE(Locations.Temp)

    @Test
    fun `should subscribe`() {
        val metaStream = MetaStream()
        val listener = Listener()
        expectCatching { metaStream.subscribe(listener) }.isSuccess()
        expectThat(listener.message).isNull()
    }

    @Test
    fun `should emit`() {
        val metaStream = MetaStream()
        expectCatching { metaStream.emit(message) }.isSuccess()
    }

    @Test
    fun `should receive event`() {
        val metaStream = MetaStream()
        val listener = Listener().also { metaStream.subscribe(it) }
        metaStream.emit(message)
        expectThat(listener.message).isEqualTo(message)
    }

    @Test
    fun `should receive past events`() {
        val metaStream = MetaStream()
        metaStream.emit(message)
        val listener = Listener().also { metaStream.subscribe(it) }
        expectThat(listener.message).isEqualTo(message)
    }

    @Test
    fun `should accept listeners with constructor`() {
        val listener = Listener()
        val metaStream = MetaStream(listener)
        metaStream.emit(message)
        expectThat(listener.message).isEqualTo(message)
    }
}

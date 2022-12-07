package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class NativeProgramTest {

    @Test fun is_debugging() = testAll {
        Program.isDebugging shouldBe false
    }

    @Test fun on_exit() = testAll {
        shouldNotThrowAny {
            Program.onExit {
//                println("${Platform.Current.name} did unload")
            }
        }
    }
}

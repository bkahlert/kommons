package com.bkahlert.kommons.uri

import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class AuthorityTest {

    @Test
    fun instantiation() = testAll {
        Authority("username:password", "example.com", 8080) should {
            it.userInfo shouldBe "username:password"
            it.host shouldBe "example.com"
            it.port shouldBe 8080
        }
    }

    @Test
    fun equality() = testAll {
        Authority("username:password", "example.com", 8080) should {
            it shouldBe Authority("username:password", "example.com", 8080)
            it shouldNotBe Authority("foo", "example.com", 8080)
            it shouldNotBe Authority("username:password", "foo", 8080)
            it shouldNotBe Authority("username:password", "example.com", 22)
        }
    }

    @Test
    fun to_string() = testAll {
        Authority("username:password", "example.com", 8080).toString() shouldBe "username:password@example.com:8080"
        Authority("username:password", "example.com", null).toString() shouldBe "username:password@example.com"
        Authority(null, "example.com", 8080).toString() shouldBe "example.com:8080"
        Authority(null, "example.com", null).toString() shouldBe "example.com"
    }

    @Test
    fun regex() = testAll {
        Authority.REGEX.shouldNotBeNull()
    }

    @Test
    fun parse() = testAll {
        Authority.parse("username:password@example.com:8080") shouldBe Authority("username:password", "example.com", 8080)
        Authority.parse("username:password@example.com") shouldBe Authority("username:password", "example.com", null)
        Authority.parse("example.com:8080") shouldBe Authority(null, "example.com", 8080)
        Authority.parse("example.com") shouldBe Authority(null, "example.com", null)
    }

    @Test
    fun parse_invalid() = testAll {
        shouldThrow<IllegalArgumentException> { Authority.parse("") }.message shouldBe " is no valid URI authority"
        Authority.parseOrNull("").shouldBeNull()
    }
}

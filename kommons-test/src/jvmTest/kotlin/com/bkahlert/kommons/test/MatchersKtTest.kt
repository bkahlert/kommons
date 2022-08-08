package com.bkahlert.kommons.test

import io.kotest.matchers.should
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class MatchersKtTest {

    @Test fun start_with() = testAll {
        Paths.get("foo/bar/baz") should startWith("foo")
        Paths.get("foo/bar/baz") should startWith("foo", "bar")
        Paths.get("foo/bar/baz") should startWith("foo", "bar", "baz")

        Paths.get("foo/bar/baz").shouldStartWith("foo")
        Paths.get("foo/bar/baz").shouldStartWith("foo", "bar")
        Paths.get("foo/bar/baz").shouldStartWith("foo", "bar", "baz")

        Paths.get("foo/bar/baz").shouldNotStartWith("bar")
        Paths.get("foo/bar/baz").shouldNotStartWith("fo")
    }

    @Test fun end_with() = testAll {
        Paths.get("foo/bar/baz") should endWith("baz")
        Paths.get("foo/bar/baz") should endWith("bar", "baz")
        Paths.get("foo/bar/baz") should endWith("foo", "bar", "baz")

        Paths.get("foo/bar/baz").shouldEndWith("baz")
        Paths.get("foo/bar/baz").shouldEndWith("bar", "baz")
        Paths.get("foo/bar/baz").shouldEndWith("foo", "bar", "baz")

        Paths.get("foo/bar/baz").shouldNotEndWith("bar")
        Paths.get("foo/bar/baz").shouldNotEndWith("az")
    }
}

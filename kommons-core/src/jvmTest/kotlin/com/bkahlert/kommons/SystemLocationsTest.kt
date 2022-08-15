package com.bkahlert.kommons

import com.bkahlert.kommons.test.junit.testEach
import io.kotest.matchers.paths.shouldBeADirectory
import io.kotest.matchers.paths.shouldBeAbsolute
import io.kotest.matchers.paths.shouldExist
import org.junit.jupiter.api.TestFactory

class SystemLocationsTest {

    @TestFactory fun locations() = testEach(
        SystemLocations.Work,
        SystemLocations.Home,
        SystemLocations.Temp,
        SystemLocations.JavaHome,
    ) {
        it.shouldBeAbsolute()
        it.shouldExist()
        it.shouldBeADirectory()
    }
}

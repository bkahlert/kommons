package koodies.test

import koodies.io.useClassPaths
import java.nio.file.Path

object FixturePath61C285F09D95930D0AE298B00AF09F918B0A {
    const val fixtureFileName = "61C285F09D95930D0AE298B00AF09F918B0A.txt"

    val fixtureContent = ubyteArrayOf(
        0x61u, 0xC2U, 0x85U, 0xF0U, 0x9DU, 0x95U, 0x93U, 0x0DU,
        0x0AU, 0xE2U, 0x98U, 0xB0U, 0x0AU, 0xF0U, 0x9FU, 0x91U,
        0x8BU, 0x0AU).toByteArray()

    inline operator fun <reified T> invoke(crossinline transform: Path.() -> T): List<T> =
        useClassPaths(fixtureFileName, transform)
}

package com.bkahlert.kommons

import com.bkahlert.kommons.debug.trace
import com.bkahlert.kommons.test.junit.runTests
import com.bkahlert.kommons.text.TextWidth
import com.bkahlert.kommons.text.columns
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage
import org.junit.platform.launcher.TagFilter.includeTags

class Playground {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runTests(selectPackage(Playground::class.java.packageName), launcherDiscoveryRequestBuilder = { filters(includeTags("playground")) })
        }
    }

    @Test
    @Tag("playground")
    fun name() {
        listOf("x", "한", "曲", "⮕", "😀", "👨🏾", "👩‍👩‍👧‍👧").forEach {
            "$it — ${TextWidth.calculateWidth(it)}, ${it.columns}".trace
        }
    }
}

package com.bkahlert.kommons

import com.bkahlert.kommons.runtime.contextClassLoader
import com.bkahlert.kommons.test.junit.runTests
import com.bkahlert.kommons.text.TextWidth
import com.bkahlert.kommons.text.columns
import com.bkahlert.kommons.tracing.TestSpanScope
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage
import org.junit.platform.launcher.TagFilter.includeTags
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.font.TextAttribute

import java.awt.geom.AffineTransform


class Playground {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runTests(selectPackage(Playground::class.java.packageName), launcherDiscoveryRequestBuilder = { filters(includeTags("playground")) })
        }
    }

    @Test
    @Tag("playground")
    fun TestSpanScope.name() {

        val affinetransform = AffineTransform()
        val frc = FontRenderContext(affinetransform, false, false)

        listOf(".", "x", "x͡", "한", "曲", "⮕", "😀", "👨🏾", "👩‍👩‍👧‍👧").forEach {
            log("$it — ${TextWidth.calculateWidth(it)}, ${it.columns}")
            val font = Font.createFonts(contextClassLoader.getResourceAsStream("NotoSerifCJKjp-Regular.otf")).first().deriveFont(mapOf(
                TextAttribute.WIDTH to null,
                TextAttribute.TRANSFORM to null,
                TextAttribute.TRACKING to null,
                TextAttribute.SIZE to 75,
                TextAttribute.POSTURE to null,
                TextAttribute.FAMILY to "Monospaced",
                TextAttribute.SUPERSCRIPT to null,
                TextAttribute.WEIGHT to null,
            ))
            log("$it — ${font.getStringBounds(it, frc)}")
            log("$it — ${font.canDisplay(it.codePoints().findFirst().asInt)}")
        }
        fail("xxx")
    }
}

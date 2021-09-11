package com.bkahlert.kommons.text

import com.bkahlert.kommons.printTestExecutionStatus
import com.bkahlert.kommons.test.junit.TestName.Companion.testName
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.Extensions
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

@Retention(RUNTIME)
@Target(ANNOTATION_CLASS, CLASS, FUNCTION)
@Extensions(
    ExtendWith(WideUnicodeGlyphsCondition::class),
)
annotation class TextWidthRequiring

/**
 * Condition that disables the test if wide Unicode characters cannot be measured correctly.
 */
class WideUnicodeGlyphsCondition : ExecutionCondition {

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult =
        context.testName.let { testName ->
            if (wideUnicodeGlyphs) enabled("Test ${testName.quoted} enabled because wide Unicode glyphs can be measured correctly.")
            else disabled("Test ${testName.quoted} disabled because wide Unicode characters cannot be measured correctly.")
        }

    private companion object {
        private val wideUnicodeGlyphs: Boolean by lazy {
            listOf(
                TextWidth.calculateWidth("X"),
                TextWidth.calculateWidth("⮕"),
                TextWidth.calculateWidth("😀"),
            ).zipWithNext { left, right -> left < right }
                .all { it }
                .also {
                    if (it) printTestExecutionStatus("Wide Unicode Glyphs: supported") { green }
                    else printTestExecutionStatus("Wide Unicode Glyphs: not supported") { yellow }
                }
        }
    }
}

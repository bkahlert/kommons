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
    ExtendWith(TextWidthCondition::class),
)
annotation class TextWidthRequiring

/**
 * Condition that disables the test if no the setup does not allow for fine-grained text width measuring.
 */
class TextWidthCondition : ExecutionCondition {

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult =
        context.testName.let { testName ->
            if (fineGrainedTextWidth) enabled("Test ${testName.quoted} enabled because fine-grained text width measuring is possible.")
            else disabled("Test ${testName.quoted} disabled because fine-grained text width measuring is NOT possible.")
        }

    private companion object {
        private val fineGrainedTextWidth: Boolean by lazy {
            listOf(
                TextWidth.calculateWidth("X"),
                TextWidth.calculateWidth("⮕"),
                TextWidth.calculateWidth("😀"),
            ).zipWithNext { left, right -> left < right }
                .all { it }
                .also {
                    if (it) printTestExecutionStatus("Text width measuring: fine-grained") { green }
                    else printTestExecutionStatus("Text width measuring: simple") { yellow }
                }
        }
    }
}

package com.bkahlert.kommons.test

import com.bkahlert.kommons.printTestExecutionStatus
import com.bkahlert.kommons.test.junit.displayName
import com.bkahlert.kommons.text.ANSI
import com.bkahlert.kommons.text.quoted
import com.github.ajalt.mordant.rendering.AnsiLevel
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER

@Target(ANNOTATION_CLASS, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, CLASS)
@Retention(RUNTIME)
@ExtendWith(AnsiCondition::class)
annotation class AnsiRequiring

/**
 * Condition that disables the test if no [ANSI] support was detected.
 */
class AnsiCondition : ExecutionCondition {

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult =
        context.displayName().composedDisplayName.let { testName ->
            if (ansiSupported) ConditionEvaluationResult.enabled("Test ${testName.quoted} enabled because ANSI is supported.")
            else ConditionEvaluationResult.disabled("Test ${testName.quoted} disabled because ANSI is NOT supported.")
        }

    companion object {

        private val ansiSupported: Boolean by lazy {

            val support = ANSI.terminal.info.ansiLevel
            (support != AnsiLevel.NONE).also {
                printTestExecutionStatus("ANSI support: $support")
            }
        }
    }
}

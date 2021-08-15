package com.bkahlert.kommons.test

import com.bkahlert.kommons.runtime.AnsiSupport.NONE
import com.bkahlert.kommons.test.junit.TestName.Companion.testName
import com.bkahlert.kommons.text.ANSI
import com.bkahlert.kommons.text.quoted
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
annotation class AnsiRequired

/**
 * Conditions that disables the test if no [ANSI] support was detected.
 */
class AnsiCondition : ExecutionCondition {

    private val ansiSupported: Boolean by lazy { com.bkahlert.kommons.runtime.ansiSupport != NONE }

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult =
        context.testName.let { testName ->
            if (ansiSupported) ConditionEvaluationResult.enabled("Test ${testName.quoted} enabled because ANSI is supported.")
            else ConditionEvaluationResult.disabled("Test ${testName.quoted} disabled because ANSI is NOT supported.")
        }
}
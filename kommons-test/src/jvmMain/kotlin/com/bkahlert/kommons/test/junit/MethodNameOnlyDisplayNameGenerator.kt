package com.bkahlert.kommons.test.junit

import org.junit.jupiter.api.DisplayNameGenerator
import java.lang.reflect.Method

/**
 * [DisplayNameGenerator] that leaves out the parameters of a method entirely.
 *
 * This generator extends the functionality of [DisplayNameGenerator.ReplaceUnderscores] by
 * dropping the parameter list.
 */
public class MethodNameOnlyDisplayNameGenerator : DisplayNameGenerator.ReplaceUnderscores() {

    /** Generates a display name for the given [testMethod] and the given [testClass] [testMethod] is invoked on. */
    override fun generateDisplayNameForMethod(testClass: Class<*>, testMethod: Method): String =
        super.generateDisplayNameForMethod(testClass, testMethod).substringBefore("(").trimEnd()
}

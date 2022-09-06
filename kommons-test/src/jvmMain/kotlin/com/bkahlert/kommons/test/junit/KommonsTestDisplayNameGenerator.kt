package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.text.toTitleCasedString
import org.junit.jupiter.api.DisplayNameGenerator
import java.lang.reflect.Method

/**
 * [DisplayNameGenerator.ReplaceUnderscores] based display name generator that
 * formats nested class names as lowercase with spaces and
 * leaves out the parameters of methods.
 */
public class KommonsTestDisplayNameGenerator : DisplayNameGenerator.ReplaceUnderscores() {

    /** Generates a display name for the given [nestedClass]. */
    override fun generateDisplayNameForNestedClass(nestedClass: Class<*>?): String =
        super.generateDisplayNameForNestedClass(nestedClass).toTitleCasedString().lowercase()

    /** Generates a display name for the given [testMethod] and the given [testClass] [testMethod] is invoked on. */
    override fun generateDisplayNameForMethod(testClass: Class<*>, testMethod: Method): String =
        super.generateDisplayNameForMethod(testClass, testMethod).substringBefore("(").trimEnd()
}

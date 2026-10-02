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

    /** Generates a display name for the given [nestedClass] enclosed by the given [enclosingInstanceTypes]. */
    override fun generateDisplayNameForNestedClass(enclosingInstanceTypes: List<Class<*>>, nestedClass: Class<*>): String =
        super.generateDisplayNameForNestedClass(enclosingInstanceTypes, nestedClass).toTitleCasedString().lowercase()

    /** Generates a display name for the given [testMethod] of the given [testClass] enclosed by the given [enclosingInstanceTypes]. */
    override fun generateDisplayNameForMethod(enclosingInstanceTypes: List<Class<*>>, testClass: Class<*>, testMethod: Method): String =
        super.generateDisplayNameForMethod(enclosingInstanceTypes, testClass, testMethod).substringBefore("(").trimEnd()
}

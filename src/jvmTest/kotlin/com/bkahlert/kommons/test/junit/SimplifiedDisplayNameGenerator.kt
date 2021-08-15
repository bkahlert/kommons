package com.bkahlert.kommons.test.junit

import org.junit.jupiter.api.DisplayNameGenerator
import java.lang.reflect.Method

class SimplifiedDisplayNameGenerator : DisplayNameGenerator.Standard() {

    override fun generateDisplayNameForClass(testClass: Class<*>): String =
        super.generateDisplayNameForClass(testClass) // e.g. "ParentClass"

    override fun generateDisplayNameForNestedClass(nestedClass: Class<*>): String =
        super.generateDisplayNameForNestedClass(nestedClass) // e.g. "NestedClass"

    override fun generateDisplayNameForMethod(testClass: Class<*>?, testMethod: Method): String =
        testMethod.name // e.g. "should perform"
}

package koodies.test.junit

import koodies.text.withoutSuffix
import org.junit.jupiter.api.DisplayNameGenerator
import java.lang.reflect.Method

class LessUglyDisplayNameGenerator : DisplayNameGenerator.Standard() {
    override fun generateDisplayNameForClass(testClass: Class<*>): String =
        super.generateDisplayNameForClass(testClass)

    override fun generateDisplayNameForNestedClass(nestedClass: Class<*>): String =
        super.generateDisplayNameForNestedClass(nestedClass)

    override fun generateDisplayNameForMethod(testClass: Class<*>?, testMethod: Method?): String =
        super.generateDisplayNameForMethod(testClass, testMethod).substringBeforeLast("$").withoutSuffix("()")
}

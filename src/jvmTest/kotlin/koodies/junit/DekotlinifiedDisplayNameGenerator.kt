package koodies.junit

import org.junit.jupiter.api.DisplayNameGenerator
import java.lang.reflect.Method

class DekotlinifiedDisplayNameGenerator : DisplayNameGenerator.Standard() {
    override fun generateDisplayNameForMethod(testClass: Class<*>?, testMethod: Method?): String =
        super.generateDisplayNameForMethod(testClass, testMethod).substringBeforeLast("$")
}

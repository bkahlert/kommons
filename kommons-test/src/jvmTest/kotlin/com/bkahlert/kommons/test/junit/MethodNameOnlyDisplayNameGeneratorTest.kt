package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayNameGenerator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.reflect.jvm.javaMethod

class MethodNameOnlyDisplayNameGeneratorTest {

    private val generator = MethodNameOnlyDisplayNameGenerator()
    private val standardGenerator = DisplayNameGenerator.Standard()

    @Test fun generate_display_name_for_class() = testAll {
        generator.generateDisplayNameForClass(OuterClass::class.java)
            .shouldBe(standardGenerator.generateDisplayNameForClass(OuterClass::class.java))
    }

    @Test fun generate_display_name_for_nested_class() = testAll {
        generator.generateDisplayNameForNestedClass(OuterClass.NestedClass::class.java)
            .shouldBe(standardGenerator.generateDisplayNameForNestedClass(OuterClass.NestedClass::class.java))
    }

    @Test fun generate_display_name_for_method() = testAll {
        generator.generateDisplayNameForMethod(
            OuterClass::class.java,
            checkNotNull(OuterClass::method.javaMethod),
        ).shouldBe("method")
        generator.generateDisplayNameForMethod(
            OuterClass::class.java,
            checkNotNull(OuterClass::methodWithParameters.javaMethod),
        ).shouldBe("methodWithParameters")
        generator.generateDisplayNameForMethod(
            OuterClass.NestedClass::class.java,
            checkNotNull(OuterClass.NestedClass::method.javaMethod),
        ).shouldBe("method")
        generator.generateDisplayNameForMethod(
            OuterClass.NestedClass::class.java,
            checkNotNull(OuterClass.NestedClass::methodWithParameters.javaMethod),
        ).shouldBe("methodWithParameters")
    }
}

private class OuterClass {

    inner class NestedClass {
        @Test fun method() = Unit
        @Test fun methodWithParameters(@Suppress("UNUSED_PARAMETER") baz: TestInfo) = Unit
    }

    @Test fun method() = Unit
    @Test fun methodWithParameters(@Suppress("UNUSED_PARAMETER") baz: TestInfo) = Unit
}

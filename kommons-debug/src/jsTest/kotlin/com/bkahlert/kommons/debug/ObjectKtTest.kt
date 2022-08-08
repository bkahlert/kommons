package com.bkahlert.kommons.debug

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ObjectTest {

    @Test fun test_keys() = testAll {
        Object.keys(nativeObject()) shouldBe arrayOf(
            "property",
        )
        Object.keys(BaseClass()) shouldBe arrayOf(
            "baseProperty_1",
            "openBaseProperty_1",
            "protectedOpenBaseProperty_1",
            "privateBaseProperty_1",
        )
        Object.keys(Singleton) shouldBe arrayOf(
            "baseProperty_1",
            "openBaseProperty_1",
            "protectedOpenBaseProperty_1",
            "privateBaseProperty_1",
            "singletonProperty_1",
            "privateSingletonProperty_1",
        )
        Object.keys(AnonymousSingleton) shouldBe arrayOf(
            "anonymousSingletonProperty_1",
            "privateAnonymousSingletonProperty_1",
        )
        Object.keys(ListImplementingSingleton) shouldBe arrayOf(
            "baseProperty_1",
            "openBaseProperty_1",
            "protectedOpenBaseProperty_1",
            "privateBaseProperty_1",
            "\$\$delegate_0__1",
            "singletonProperty_1",
            "privateSingletonProperty_1",
        )
        Object.keys(ListImplementingAnonymousSingleton) shouldBe arrayOf(
            "\$\$delegate_0__1",
            "anonymousSingletonProperty_1",
            "privateAnonymousSingletonProperty_1",
        )
        Object.keys(MapImplementingSingleton) shouldBe arrayOf(
            "baseProperty_1",
            "openBaseProperty_1",
            "protectedOpenBaseProperty_1",
            "privateBaseProperty_1",
            "\$\$delegate_0__1",
            "singletonProperty_1",
            "privateSingletonProperty_1",
        )
        Object.keys(MapImplementingAnonymousSingleton) shouldBe arrayOf(
            "\$\$delegate_0__1",
            "anonymousSingletonProperty_1",
            "privateAnonymousSingletonProperty_1",
        )
        Object.keys(OrdinaryClass()) shouldBe arrayOf(
            "baseProperty_1",
            "openBaseProperty_1",
            "protectedOpenBaseProperty_1",
            "privateBaseProperty_1",
            "ordinaryProperty_1",
            "privateOrdinaryProperty_1",
        )
        Object.keys(DataClass()) shouldBe arrayOf(
            "baseProperty_1",
            "openBaseProperty_1",
            "protectedOpenBaseProperty_1",
            "privateBaseProperty_1",
            "dataProperty_1",
            "openBaseProperty_2",
            "protectedOpenBaseProperty_2",
            "privateDataProperty_1"
        )
        val customObject = ClassWithDefaultToString(null)
        Object.keys(customObject) shouldBe arrayOf(
            "foo_1",
            "bar_1",
        )
        Object.keys(ClassWithDefaultToString(customObject)) shouldBe arrayOf(
            "foo_1",
            "bar_1",
        )
    }

    @Test fun test_entries() = testAll {
        Object.entries(nativeObject()) shouldBe arrayOf(
            arrayOf("property", "Function-property"),
        )
        Object.entries(BaseClass()) shouldBe arrayOf(
            arrayOf("baseProperty_1", "base-property"),
            arrayOf("openBaseProperty_1", 42),
            arrayOf("protectedOpenBaseProperty_1", "protected-open-base-property"),
            arrayOf("privateBaseProperty_1", "private-base-property"),
        )
        Object.entries(Singleton) shouldBe arrayOf(
            arrayOf("baseProperty_1", "base-property"),
            arrayOf("openBaseProperty_1", 42),
            arrayOf("protectedOpenBaseProperty_1", "protected-open-base-property"),
            arrayOf("privateBaseProperty_1", "private-base-property"),
            arrayOf("singletonProperty_1", "singleton-property"),
            arrayOf("privateSingletonProperty_1", "private-singleton-property"),
        )
        Object.entries(AnonymousSingleton) shouldBe arrayOf(
            arrayOf("anonymousSingletonProperty_1", "anonymous-singleton-property"),
            arrayOf("privateAnonymousSingletonProperty_1", "private-anonymous-singleton-property"),
        )
        Object.entries(ListImplementingSingleton) shouldBe arrayOf(
            arrayOf("baseProperty_1", "base-property"),
            arrayOf("openBaseProperty_1", 42),
            arrayOf("protectedOpenBaseProperty_1", "protected-open-base-property"),
            arrayOf("privateBaseProperty_1", "private-base-property"),
            arrayOf("\$\$delegate_0__1", listOf("foo", null)),
            arrayOf("singletonProperty_1", "singleton-property"),
            arrayOf("privateSingletonProperty_1", "private-singleton-property"),
        )
        Object.entries(ListImplementingAnonymousSingleton) shouldBe arrayOf(
            arrayOf("\$\$delegate_0__1", listOf("foo", null)),
            arrayOf("anonymousSingletonProperty_1", "anonymous-singleton-property"),
            arrayOf("privateAnonymousSingletonProperty_1", "private-anonymous-singleton-property"),
        )
        Object.entries(MapImplementingSingleton) shouldBe arrayOf(
            arrayOf("baseProperty_1", "base-property"),
            arrayOf("openBaseProperty_1", 42),
            arrayOf("protectedOpenBaseProperty_1", "protected-open-base-property"),
            arrayOf("privateBaseProperty_1", "private-base-property"),
            arrayOf("\$\$delegate_0__1", mapOf("foo" to "bar", "baz" to null)),
            arrayOf("singletonProperty_1", "singleton-property"),
            arrayOf("privateSingletonProperty_1", "private-singleton-property"),
        )
        Object.entries(MapImplementingAnonymousSingleton) shouldBe arrayOf(
            arrayOf("\$\$delegate_0__1", mapOf("foo" to "bar", "baz" to null)),
            arrayOf("anonymousSingletonProperty_1", "anonymous-singleton-property"),
            arrayOf("privateAnonymousSingletonProperty_1", "private-anonymous-singleton-property"),
        )
        Object.entries(OrdinaryClass()) shouldBe arrayOf(
            arrayOf("baseProperty_1", "base-property"),
            arrayOf("openBaseProperty_1", 42),
            arrayOf("protectedOpenBaseProperty_1", "protected-open-base-property"),
            arrayOf("privateBaseProperty_1", "private-base-property"),
            arrayOf("ordinaryProperty_1", "ordinary-property"),
            arrayOf("privateOrdinaryProperty_1", "private-ordinary-property"),
        )
        Object.entries(DataClass()) shouldBe arrayOf(
            arrayOf("baseProperty_1", "base-property"),
            arrayOf("openBaseProperty_1", 42),
            arrayOf("protectedOpenBaseProperty_1", "protected-open-base-property"),
            arrayOf("privateBaseProperty_1", "private-base-property"),
            arrayOf("dataProperty_1", "data-property"),
            arrayOf("openBaseProperty_2", 37),
            arrayOf("protectedOpenBaseProperty_2", "overridden-protected-open-base-property"),
            arrayOf("privateDataProperty_1", "private-data-property"),
        )
        val customObject = ClassWithDefaultToString(null)
        Object.entries(customObject) shouldBe arrayOf(
            arrayOf("foo_1", null),
            arrayOf("bar_1", "baz"),
        )
        Object.entries(ClassWithDefaultToString(customObject)) shouldBe arrayOf(
            arrayOf("foo_1", customObject),
            arrayOf("bar_1", "baz"),
        )
    }

    @Test fun object_get_own_property_names() = testAll {
        Object.getOwnPropertyNames(nativeObject()) shouldBe arrayOf(
            "property",
        )
        Object.getOwnPropertyNames(BaseClass()) shouldBe arrayOf(
            "baseProperty_1",
            "openBaseProperty_1",
            "protectedOpenBaseProperty_1",
            "privateBaseProperty_1"
        )
        Object.getOwnPropertyNames(Singleton) shouldBe arrayOf(
            "baseProperty_1",
            "openBaseProperty_1",
            "protectedOpenBaseProperty_1",
            "privateBaseProperty_1",
            "singletonProperty_1",
            "privateSingletonProperty_1",
        )
        Object.getOwnPropertyNames(AnonymousSingleton) shouldBe arrayOf(
            "anonymousSingletonProperty_1",
            "privateAnonymousSingletonProperty_1",
        )
        Object.getOwnPropertyNames(ListImplementingSingleton) shouldBe arrayOf(
            "baseProperty_1",
            "openBaseProperty_1",
            "protectedOpenBaseProperty_1",
            "privateBaseProperty_1",
            "\$\$delegate_0__1",
            "singletonProperty_1",
            "privateSingletonProperty_1",
            "kotlinHashCodeValue\$",
        )
        Object.getOwnPropertyNames(ListImplementingAnonymousSingleton) shouldBe arrayOf(
            "\$\$delegate_0__1",
            "anonymousSingletonProperty_1",
            "privateAnonymousSingletonProperty_1",
        )
        Object.getOwnPropertyNames(MapImplementingSingleton) shouldBe arrayOf(
            "baseProperty_1",
            "openBaseProperty_1",
            "protectedOpenBaseProperty_1",
            "privateBaseProperty_1",
            "\$\$delegate_0__1",
            "singletonProperty_1",
            "privateSingletonProperty_1",
            "kotlinHashCodeValue\$",
        )
        Object.getOwnPropertyNames(MapImplementingAnonymousSingleton) shouldBe arrayOf(
            "\$\$delegate_0__1",
            "anonymousSingletonProperty_1",
            "privateAnonymousSingletonProperty_1",
        )
        Object.getOwnPropertyNames(OrdinaryClass()) shouldBe arrayOf(
            "baseProperty_1",
            "openBaseProperty_1",
            "protectedOpenBaseProperty_1",
            "privateBaseProperty_1",
            "ordinaryProperty_1",
            "privateOrdinaryProperty_1"
        )
        Object.getOwnPropertyNames(DataClass()) shouldBe arrayOf(
            "baseProperty_1",
            "openBaseProperty_1",
            "protectedOpenBaseProperty_1",
            "privateBaseProperty_1",
            "dataProperty_1",
            "openBaseProperty_2",
            "protectedOpenBaseProperty_2",
            "privateDataProperty_1",
        )
        val customObject = ClassWithDefaultToString(null)
        Object.getOwnPropertyNames(customObject) shouldBe arrayOf(
            "foo_1",
            "bar_1",
        )
        Object.getOwnPropertyNames(ClassWithDefaultToString(customObject)) shouldBe arrayOf(
            "foo_1",
            "bar_1",
        )
    }

    @Test fun test_keys_extension() = testAll {
        nativeObject().keys shouldBe Object.keys(nativeObject())
        BaseClass().keys shouldBe Object.keys(BaseClass())
        Singleton.keys shouldBe Object.keys(Singleton)
        AnonymousSingleton.keys shouldBe Object.keys(AnonymousSingleton)
        ListImplementingSingleton.keys shouldBe Object.keys(ListImplementingSingleton)
        ListImplementingAnonymousSingleton.keys shouldBe Object.keys(ListImplementingAnonymousSingleton)
//        MapImplementingSingleton.keys shouldBe Object.keys(MapImplementingSingleton)
//        MapImplementingAnonymousSingleton.keys shouldBe Object.keys(MapImplementingAnonymousSingleton)
        Singleton.keys shouldBe Object.keys(Singleton)
        AnonymousSingleton.keys shouldBe Object.keys(AnonymousSingleton)
        OrdinaryClass().keys shouldBe Object.keys(OrdinaryClass())
        DataClass().keys shouldBe Object.keys(DataClass())
        val customObject = ClassWithDefaultToString(null)
        customObject.keys shouldBe Object.keys(customObject)
        ClassWithDefaultToString(customObject).keys shouldBe Object.keys(ClassWithDefaultToString(customObject))
    }

    @Test fun test_entries_extension() = testAll {
        nativeObject().entries shouldBe Object.entries(nativeObject())
        BaseClass().entries shouldBe Object.entries(BaseClass())
        Singleton.entries shouldBe Object.entries(Singleton)
        AnonymousSingleton.entries shouldBe Object.entries(AnonymousSingleton)
        ListImplementingSingleton.entries shouldBe Object.entries(ListImplementingSingleton)
        ListImplementingAnonymousSingleton.entries shouldBe Object.entries(ListImplementingAnonymousSingleton)
//        MapImplementingSingleton.entries shouldBe Object.entries(MapImplementingSingleton)
//        MapImplementingAnonymousSingleton.entries shouldBe Object.entries(MapImplementingAnonymousSingleton)
        OrdinaryClass().entries shouldBe Object.entries(OrdinaryClass())
        DataClass().entries shouldBe Object.entries(DataClass())
        val customObject = ClassWithDefaultToString(null)
        customObject.entries shouldBe Object.entries(customObject)
        ClassWithDefaultToString(customObject).entries shouldBe Object.entries(ClassWithDefaultToString(customObject))
    }
}

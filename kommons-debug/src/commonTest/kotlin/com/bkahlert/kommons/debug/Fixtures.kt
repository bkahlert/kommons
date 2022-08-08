@file:Suppress("MayBeConstant")

package com.bkahlert.kommons.debug

import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

internal expect fun nativeObject(): Any

internal open class BaseClass {
    val baseProperty: String = "base-property"
    open val openBaseProperty: Int = 42
    protected open val protectedOpenBaseProperty: String = "protected-open-base-property"
    private val privateBaseProperty: String = "private-base-property"

    open fun kProperties0(): Set<KProperty0<Any?>> = setOf(
        this::baseProperty,
        this::openBaseProperty,
        this::protectedOpenBaseProperty,
        this::privateBaseProperty,
    )

    companion object {
        fun kProperties1(): Set<KProperty1<out BaseClass, Any?>> = setOf(
            BaseClass::baseProperty,
            BaseClass::openBaseProperty,
            BaseClass::protectedOpenBaseProperty,
            BaseClass::privateBaseProperty,
        )
    }
}

@Suppress("unused")
internal object Singleton : BaseClass() {
    val singletonProperty: String = "singleton-property"
    private val privateSingletonProperty: String = "private-singleton-property"
}

@Suppress("unused")
val AnonymousSingleton: Any = object {
    val anonymousSingletonProperty: String = "anonymous-singleton-property"
    private val privateAnonymousSingletonProperty: String = "private-anonymous-singleton-property"
}

@Suppress("unused")
internal object ListImplementingSingleton : BaseClass(), List<Any?> by listOf("foo", null) {
    val singletonProperty: String = "singleton-property"
    private val privateSingletonProperty: String = "private-singleton-property"
}

@Suppress("unused")
val ListImplementingAnonymousSingleton = object : List<Any?> by listOf("foo", null) {
    val anonymousSingletonProperty: String = "anonymous-singleton-property"
    private val privateAnonymousSingletonProperty: String = "private-anonymous-singleton-property"
}

@Suppress("unused")
internal object MapImplementingSingleton : BaseClass(), Map<String, Any?> by mapOf("foo" to "bar", "baz" to null) {
    val singletonProperty: String = "singleton-property"
    private val privateSingletonProperty: String = "private-singleton-property"
}

@Suppress("unused")
val MapImplementingAnonymousSingleton = object : Map<String, Any?> by mapOf("foo" to "bar", "baz" to null) {
    val anonymousSingletonProperty: String = "anonymous-singleton-property"
    private val privateAnonymousSingletonProperty: String = "private-anonymous-singleton-property"
}

@Suppress("unused")
internal class OrdinaryClass : BaseClass() {
    val ordinaryProperty: String = "ordinary-property"
    private val privateOrdinaryProperty: String = "private-ordinary-property"

    class NestedClass {
        val nestedProperty: String = "nested-property"

        inner class InnerNestedClass {
            val innerNestedProperty: String = "inner-nested-property"
        }
    }
}

internal sealed class SealedClass {
    @Suppress("unused", "CanSealedSubClassBeObject") class NestedClass : SealedClass()
    object NestedObject : SealedClass()
}

internal data class DataClass(
    val dataProperty: String = "data-property",
    override val openBaseProperty: Int = 37,
) : BaseClass() {
    override val protectedOpenBaseProperty: String = "overridden-protected-open-base-property"
    private val privateDataProperty: String = "private-data-property"

    override fun kProperties0(): Set<KProperty0<Any?>> = buildSet {
        addAll(super.kProperties0())
        add(this@DataClass::dataProperty)
        add(this@DataClass::openBaseProperty)
        add(this@DataClass::protectedOpenBaseProperty)
        add(this@DataClass::privateDataProperty)
    }

    companion object {
        fun kProperties1(): Set<KProperty1<DataClass, Any?>> = buildSet {
            addAll(BaseClass.kProperties1().filterIsInstance<KProperty1<DataClass, Any?>>())
            add(DataClass::dataProperty)
            add(DataClass::openBaseProperty)
            add(DataClass::protectedOpenBaseProperty)
            add(DataClass::privateDataProperty)
        }
    }
}

internal class ClassWithDefaultToString(val foo: Any? = null) {
    val bar: String = "baz"
}

internal class ClassWithCustomToString(val foo: Any? = null) {
    override fun toString(): String = "custom toString"
}

internal class ClassWithRenderingToString(val foo: Any? = null) {
    override fun toString(): String = render()
}

internal class SelfReferencingClass : BaseClass() {
    @Suppress("unused") val selfProperty: SelfReferencingClass = this
}

internal class ThrowingClass {
    private val privateThrowingProperty: Any get() = throw RuntimeException("error reading private property")
    val throwingProperty: Any get() = throw RuntimeException("error reading property")

    fun kProperties0(): Set<KProperty0<Any?>> = setOf(
        this::privateThrowingProperty,
        this::throwingProperty,
    )

    companion object {
        fun kProperties1(): Set<KProperty1<ThrowingClass, Any?>> = setOf(
            ThrowingClass::privateThrowingProperty,
            ThrowingClass::throwingProperty,
        )
    }
}

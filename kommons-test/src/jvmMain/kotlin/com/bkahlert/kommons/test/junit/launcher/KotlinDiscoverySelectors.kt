package com.bkahlert.kommons.test.junit.launcher

import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.engine.discovery.MethodSelector
import org.junit.platform.engine.discovery.NestedClassSelector
import org.junit.platform.engine.discovery.NestedMethodSelector
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

/**
 * Collection of Kotlin specific factory methods for creating
 * instances of [DiscoverySelector].
 */
public object KotlinDiscoverySelectors {

    /**
     * Create a [ClassSelector] for the supplied [kClass].
     * @see DiscoverySelectors.selectClass
     */
    public fun selectKotlinClass(
        kClass: KClass<*>,
    ): ClassSelector = DiscoverySelectors.selectClass(
        kClass.java
    )

    /**
     * Create a [MethodSelector] for the supplied [kClass] and [kFunction].
     * @see DiscoverySelectors.selectMethod
     */
    public fun selectKotlinMemberFunction(
        kClass: KClass<*>,
        kFunction: KFunction<*>,
    ): MethodSelector = DiscoverySelectors.selectMethod(
        kClass.java,
        kFunction.javaMethod,
    )

    /**
     * Create a [NestedClassSelector] for the supplied [nestedKClass] and its [enclosingKClasses].
     * @see DiscoverySelectors.selectNestedClass
     */
    @Suppress("GrazieInspection")
    public fun selectNestedKotlinClass(
        enclosingKClasses: List<KClass<*>>,
        nestedKClass: KClass<*>,
    ): NestedClassSelector = DiscoverySelectors.selectNestedClass(
        enclosingKClasses.map { it.java },
        nestedKClass.java,
    )

    /**
     * Create a [NestedClassSelector] for the supplied [nestedKClass], and [enclosingKClasses] (ordered bottom-up)
     * @see DiscoverySelectors.selectNestedClass
     */
    @Suppress("GrazieInspection")
    public fun selectNestedKotlinClass(
        nestedKClass: KClass<*>,
        vararg enclosingKClasses: KClass<*>,
    ): NestedClassSelector = DiscoverySelectors.selectNestedClass(
        enclosingKClasses.reversed().map { it.java },
        nestedKClass.java,
    )

    /**
     * Create a [NestedMethodSelector] for the supplied [nestedKClass], its [enclosingKClasses], and [kFunction].
     * @see DiscoverySelectors.selectNestedMethod
     */
    public fun selectNestedKotlinMemberFunction(
        enclosingKClasses: List<KClass<*>>,
        nestedKClass: KClass<*>,
        kFunction: KFunction<*>,
    ): NestedMethodSelector = DiscoverySelectors.selectNestedMethod(
        enclosingKClasses.map { it.java },
        nestedKClass.java,
        kFunction.javaMethod,
    )

    /**
     * Create a [NestedMethodSelector] for the supplied [kFunction], its [nestedKClass], and [enclosingKClasses] (ordered bottom-up).
     * @see DiscoverySelectors.selectNestedMethod
     */
    public fun selectNestedKotlinMemberFunction(
        kFunction: KFunction<*>,
        nestedKClass: KClass<*>,
        vararg enclosingKClasses: KClass<*>,
    ): NestedMethodSelector = DiscoverySelectors.selectNestedMethod(
        enclosingKClasses.reversed().map { it.java },
        nestedKClass.java,
        kFunction.javaMethod,
    )

    /**
     * Create a [ClassSelector] or a [NestedClassSelector] depending on the specified [kClass].
     * @see selectKotlinClass
     * @see selectNestedKotlinClass
     */
    public fun select(
        kClass: KClass<*>,
    ): DiscoverySelector = kClass.innerEnclosingKClasses.takeUnless { it.isEmpty() }
        ?.let { selectNestedKotlinClass(kClass, *it.toTypedArray()) }
        ?: selectKotlinClass(kClass)


    /**
     * Create a [MethodSelector] or a [NestedMethodSelector] depending on the specified [kFunction].
     * @see selectKotlinMemberFunction
     * @see selectNestedKotlinMemberFunction
     */
    public fun select(
        kFunction: KFunction<*>,
    ): DiscoverySelector = when (val kClassifier = kFunction.parameters.first().type.classifier) {
        is KClass<*> -> kClassifier.innerEnclosingKClasses.takeUnless { it.isEmpty() }
            ?.let { selectNestedKotlinMemberFunction(kFunction, kClassifier, *it.toTypedArray()) }
            ?: selectKotlinMemberFunction(kClassifier, kFunction)
        else -> throw IllegalArgumentException("$kFunction must be a member function")
    }
}

private val KClass<*>.innerEnclosingKClasses: List<KClass<*>>
    get() = if (isInner) enclosingKClasses.let {
        val innerKClasses = it.takeWhile(KClass<*>::isInner)
        if (it.size > innerKClasses.size) buildList { addAll(innerKClasses);add(it[innerKClasses.size]) }
        else innerKClasses
    } else emptyList()

private val KClass<*>.enclosingKClasses: List<KClass<*>>
    get() = enclosingKClass?.let { listOf(it) + it.enclosingKClasses } ?: emptyList()
private val KClass<*>.enclosingKClass: KClass<*>?
    get() = java.enclosingClass?.takeUnless { it.name.endsWith("Kt") }?.kotlin

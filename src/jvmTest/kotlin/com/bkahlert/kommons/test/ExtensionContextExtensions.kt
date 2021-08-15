package com.bkahlert.kommons.test

import com.bkahlert.kommons.runtime.ancestor
import com.bkahlert.kommons.runtime.ancestors
import com.bkahlert.kommons.runtime.orNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.util.Optional
import kotlin.reflect.KClass

/**
 * Checks if the test class, method, test template, etc. of the current scope fulfills the requirements implemented by the provided [tester].
 */
fun ExtensionContext.element(tester: AnnotatedElement.() -> Boolean): Boolean = element.map(tester).orElse(false)

/**
 * Gets the result of the test or container associated with this [ExtensionContext], that is,
 * in case of success the [Result] contains [Unit] and in case of failure the thrown exception.
 */
val ExtensionContext.executionResult: Result<Unit>
    get() = executionException.orNull()?.let { Result.failure(it) } ?: Result.success(Unit)

/**
 * The container of this test method.
 */
val ExtensionContext.ancestor: Class<*>?
    get() = when (val el = element.orElse(null)) {
        is Method -> el.ancestor
        is Class<*> -> el.ancestor
        else -> null
    }

/**
 * Contains the ancestors, that is this test's parent container, the parent's parent container, … up to the root.
 */
val ExtensionContext.ancestors: List<Class<*>>
    get() = when (val el = element.orElse(null)) {
        is Method -> el.ancestors.drop(1).map { it as Class<*> }
        is Class<*> -> el.ancestors.drop(1)
        else -> emptyList()
    }

/**
 * Contains the ancestor's tests.
 */
val ExtensionContext.allTests: List<Method>
    get() {
        val root = ancestors.lastOrNull() ?: testClass.orElse(null)
        return root?.descendentContainers?.flatMap {
            it.declaredMethods.toList()
        } ?: emptyList()
    }


/**
 * Contains the ancestors, that is this test's parent container, the parent's parent container, … up to the root.
 */
val Class<*>.descendentContainers: List<Class<*>>
    get() = mutableListOf(this) + declaredClasses.flatMap { it.descendentContainers }


/**
 * Whether this [Method] is a [Test].
 */
val Optional<Method>?.isTest get() = this?.orElse(null).isTest

/**
 * Whether this [Method] is a [Test].
 */
val Method?.isTest get() = isA<Test>() || isA<TestFactory>() || isA<TestTemplate>()


/**
 * Puts the given [obj] in this [Store] using the objects [KClass] as its key.
 */
inline fun <reified T> Store.put(obj: T): Unit = put(T::class.java, obj)

/**
 * Gets the previously [put] instance of type [T] from this [Store].
 *
 * ***Important:** [T] must be the exact same type as used in [put].*
 */
inline fun <reified T> Store.get(): T? = get(T::class.java, T::class.java)

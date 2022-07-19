package com.bkahlert.kommons.runtime

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method

/**
 * Enclosing class of this class, if any. `null` otherwise.
 */
public val Class<*>.ancestor: Class<*>? get() = enclosingClass

/**
 * All ancestors of this class, **including this class itself** (≙ ancestor of zeroth degree).
 */
public val Class<*>.ancestors: List<Class<*>> get() = generateSequence(this) { it.ancestor }.toList()

/**
 * Declaring class of this method.
 */
public val Method.ancestorx: Class<*> get() = declaringClass

/**
 * All ancestors of this method, that is, this method itself (≙ ancestor of zeroth degree),
 * its declaring class and the declaring class's ancestors.
 */
public val Method.ancestorsx: List<AnnotatedElement> get() = listOf(this, *ancestorx.ancestors.toList().toTypedArray())

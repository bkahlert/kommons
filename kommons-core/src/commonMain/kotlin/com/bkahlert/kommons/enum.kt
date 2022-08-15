package com.bkahlert.kommons

import kotlin.reflect.KProperty1

/** Returns the first enum entry matched by the specified [filter], or `null` otherwise. */
public inline fun <reified E : Enum<E>> firstEnumValueOfOrNull(filter: (E) -> Boolean): E? =
    enumValues<E>().firstOrNull(filter)

/** Returns the first enum entry of which the specified [property] is matched by the specified [filter], or `null` otherwise. */
public inline fun <reified E : Enum<E>, T> firstEnumValueOfOrNull(property: KProperty1<in E, T>, filter: (T) -> Boolean): E? =
    enumValues<E>().firstOrNull { filter(property.get(it)) }

/** Returns the first enum entry of which the specified [property] matches the specified [probe], or `null` otherwise. */
public inline fun <reified E : Enum<E>, T> firstEnumValueOfOrNull(property: KProperty1<in E, T>, probe: T): E? =
    enumValues<E>().firstOrNull { property.get(it) == probe }

/** Returns the first enum entry matched by the specified [filter], or throws a [NoSuchElementException] otherwise. */
public inline fun <reified E : Enum<E>> firstEnumValueOf(filter: (E) -> Boolean): E =
    firstEnumValueOfOrNull(filter)
        ?: throw NoSuchElementException("${E::class.simpleName} contains no value matching the predicate.")

/** Returns the first enum entry of which the specified [property] is matched by the specified [filter], or throws a [NoSuchElementException] otherwise. */
public inline fun <reified E : Enum<E>, T> firstEnumValueOf(property: KProperty1<in E, T>, filter: (T) -> Boolean): E =
    firstEnumValueOfOrNull(property, filter)
        ?: throw NoSuchElementException("${E::class.simpleName} contains no value of which the property ${property.name.quoted} matches the predicate.")

/** Returns the first enum entry of which the specified [property] matches the specified [probe], or throws a [NoSuchElementException] otherwise. */
public inline fun <reified E : Enum<E>, T> firstEnumValueOf(property: KProperty1<in E, T>, probe: T): E =
    firstEnumValueOfOrNull(property, probe)
        ?: throw NoSuchElementException("${E::class.simpleName} contains no value of which the property ${property.name.quoted} is ${probe.toString().quoted}.")

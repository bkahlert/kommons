package com.bkahlert.kommons

import kotlin.reflect.KProperty1

/** Returns the first enum entry matched by the specified [filter], or `null` otherwise. */
public inline fun <reified E : Enum<E>> firstEnumValueOfOrNull(filter: (E) -> Boolean): E? =
    enumValues<E>().firstOrNull(filter)

/** Returns the first enum entry of which the specified [property] is matched by the specified [filter], or `null` otherwise. */
public inline fun <reified E : Enum<E>, V> firstEnumValueOfOrNull(property: KProperty1<in E, V>, filter: (V) -> Boolean): E? =
    enumValues<E>().firstOrNull { filter(property.get(it)) }

/** Returns the first enum entry of which the specified [property] matches the specified [probe], or `null` otherwise. */
public inline fun <reified E : Enum<E>, V> firstEnumValueOfOrNull(property: KProperty1<in E, V>, probe: V): E? =
    enumValues<E>().firstOrNull { property.get(it) == probe }

/** Returns the first enum entry matched by the specified [filter], or throws a [NoSuchElementException] otherwise. */
public inline fun <reified E : Enum<E>> firstEnumValueOf(filter: (E) -> Boolean): E =
    firstEnumValueOfOrNull(filter)
        ?: throw NoSuchElementException("${E::class.simpleName} contains no value matching the predicate.")

/** Returns the first enum entry of which the specified [property] is matched by the specified [filter], or throws a [NoSuchElementException] otherwise. */
public inline fun <reified E : Enum<E>, V> firstEnumValueOf(property: KProperty1<in E, V>, filter: (V) -> Boolean): E =
    firstEnumValueOfOrNull(property, filter)
        ?: throw NoSuchElementException("${E::class.simpleName} contains no value of which the property ${property.name.quoted} matches the predicate.")

/** Returns the first enum entry of which the specified [property] matches the specified [probe], or throws a [NoSuchElementException] otherwise. */
public inline fun <reified E : Enum<E>, V> firstEnumValueOf(property: KProperty1<in E, V>, probe: V): E =
    firstEnumValueOfOrNull(property, probe)
        ?: throw NoSuchElementException("${E::class.simpleName} contains no value of which the property ${property.name.quoted} is ${probe.toString().quoted}.")


/**
 * The enum constant [E] with the [Enum.ordinal]
 * being this constant's ordinal -1.
 *
 * If this enum constant is the first constant,
 * the last one is returned.
 */
public inline val <reified E : Enum<E>> E.predecessor: E
    get() {
        val enumValues = enumValues<E>()
        return enumValues[(ordinal - 1).mod(enumValues.size)]
    }

/**
 * The enum constant [E] with the [Enum.ordinal]
 * being this constant's ordinal +1.
 *
 * If this enum constant is the last constant,
 * the first one is returned.
 */
public inline val <reified E : Enum<E>> E.successor: E
    get() {
        val enumValues = enumValues<E>()
        return enumValues[(ordinal + 1).mod(enumValues.size)]
    }

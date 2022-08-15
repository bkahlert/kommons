package com.bkahlert.kommons

import kotlin.reflect.KClass

/** The list of all subclasses if this class is a sealed class, or an empty list otherwise. */
public val <T : Any> KClass<out T>.allSealedSubclasses: List<KClass<out T>>
    get() = buildList {
        sealedSubclasses.forEach { sealedSubclass ->
            add(sealedSubclass)
            addAll(sealedSubclass.allSealedSubclasses)
        }
    }

/** The list of all subclasses that are singletons if this class is a sealed class, or an empty list otherwise. */
public val <T : Any> KClass<out T>.allSealedObjectInstances: List<T>
    get() = allSealedSubclasses.mapNotNull { it.objectInstance }

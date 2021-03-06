package koodies.concurrent

import java.util.Collections
import java.util.NavigableMap
import java.util.NavigableSet
import java.util.SortedMap
import java.util.SortedSet

@JvmName("synchronizedCollection")
@Deprecated("use explicit locking")
public fun <T> MutableCollection<T>.synchronized(): MutableCollection<T> = Collections.synchronizedCollection(this)

@JvmName("synchronizedSet")
@Deprecated("use explicit locking")
public fun <T> MutableSet<T>.synchronized(): MutableSet<T> = Collections.synchronizedSet(this)

@JvmName("synchronizedSortedSet")
@Deprecated("use explicit locking")
public fun <T> SortedSet<T>.synchronized(): SortedSet<T> = Collections.synchronizedSortedSet(this)

@JvmName("synchronizedNavigableSet")
@Deprecated("use explicit locking")
public fun <T> NavigableSet<T>.synchronized(): NavigableSet<T> = Collections.synchronizedNavigableSet(this)

@JvmName("synchronizedList")
@Deprecated("use explicit locking")
public fun <T> MutableList<T>.synchronized(): MutableList<T> = Collections.synchronizedList(this)

@JvmName("synchronizedMap")
@Deprecated("use explicit locking")
public fun <T, U> MutableMap<T, U>.synchronized(): MutableMap<T, U> = Collections.synchronizedMap(this)

@JvmName("synchronizedSortedMap")
@Deprecated("use explicit locking")
public fun <T, U> SortedMap<T, U>.synchronized(): SortedMap<T, U> = Collections.synchronizedSortedMap(this)

@JvmName("synchronizedNavigableMap")
@Deprecated("use explicit locking")
public fun <T, U> NavigableMap<T, U>.synchronized(): NavigableMap<T, U> = Collections.synchronizedNavigableMap(this)

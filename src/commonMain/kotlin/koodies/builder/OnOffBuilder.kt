package koodies.builder

import koodies.builder.BooleanBuilder.BooleanValue
import kotlin.DeprecationLevel.ERROR

/**
 * `On`/`Off` style builder for [Boolean].
 *
 * @sample OnOffBuilderSamples.directUse
 * @sample OnOffBuilderSamples.indirectUse
 */
@Deprecated("delete", level = ERROR)
typealias OnOffBuilder = BooleanBuilder.OnOff

@Deprecated("delete", level = ERROR)
fun (BooleanBuilder.OnOff.Context.() -> BooleanValue).buildIf(): Boolean =
    BooleanBuilder.OnOff { this@buildIf() }

@Deprecated("delete", level = ERROR)
fun (BooleanBuilder.OnOff.Context.() -> BooleanValue).buildIf(collection: MutableCollection<Boolean>): Boolean = TODO()

@Deprecated("delete", level = ERROR)
fun <T> (BooleanBuilder.OnOff.Context.() -> BooleanValue).buildIf(transformOnTrue: () -> T): T? = TODO()

@Deprecated("delete", level = ERROR)
fun <T> (BooleanBuilder.OnOff.Context.() -> BooleanValue).buildIfTo(collection: MutableCollection<T>, transformOnTrue: () -> T): T? = TODO()

@Deprecated("delete", level = ERROR)
fun <K, V> (BooleanBuilder.OnOff.Context.() -> BooleanValue).buildIfTo(map: MutableMap<K, V>, transformOnTrue: () -> Pair<K, V>): Pair<K, V>? = TODO()

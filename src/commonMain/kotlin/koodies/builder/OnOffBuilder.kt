package koodies.builder

import koodies.builder.BooleanBuilder.BooleanValue

/**
 * `On`/`Off` style builder for [Boolean].
 *
 * @sample OnOffBuilderSamples.directUse
 * @sample OnOffBuilderSamples.indirectUse
 */
@Deprecated("use BooleanBuilder instead")
typealias OnOffBuilder = BooleanBuilder.OnOff

@Deprecated("use BooleanBuilder instead")
fun (BooleanBuilder.OnOff.Context.() -> BooleanValue).buildIf(): Boolean =
    BooleanBuilder.OnOff { this@buildIf() }

@Deprecated("use BooleanBuilder instead")
fun (BooleanBuilder.OnOff.Context.() -> BooleanValue).buildIf(collection: MutableCollection<Boolean>): Boolean =
    buildIf().also { collection.add(it) }

@Deprecated("use BooleanBuilder instead")
fun <T> (BooleanBuilder.OnOff.Context.() -> BooleanValue).buildIf(transformOnTrue: () -> T): T? =
    buildIf().let { if (it) transformOnTrue() else null }

@Deprecated("use BooleanBuilder instead")
fun <T> (BooleanBuilder.OnOff.Context.() -> BooleanValue).buildIfTo(collection: MutableCollection<T>, transformOnTrue: () -> T): T? =
    buildIf(transformOnTrue)?.also { collection.add(it) }

@Deprecated("use BooleanBuilder instead")
fun <K, V> (BooleanBuilder.OnOff.Context.() -> BooleanValue).buildIfTo(map: MutableMap<K, V>, transformOnTrue: () -> Pair<K, V>): Pair<K, V>? =
    buildIf().let { if (it) transformOnTrue() else null }?.also { map[it.first] = it.second }

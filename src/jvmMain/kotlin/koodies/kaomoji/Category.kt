package koodies.kaomoji

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

public open class Category : AbstractList<Kaomoji>() {
    private val _list = mutableListOf<Kaomoji>()

    override val size: Int get() = _list.size
    override fun get(index: Int): Kaomoji = _list[index]

    /**
     * Creates a new Kaomoji property based on the property's name.
     *
     * If the kaomoji contains illegal characters [manualKaomojiString] can be used to specify the correct one
     * while using a similar kaomoji with only legal characters as the property name itself.
     */
    public fun auto(manualKaomojiString: String? = null): PropertyDelegateProvider<Category, KaomojiDelegate> = PropertyDelegateProvider { category, property ->
        val kaomojiString = manualKaomojiString ?: property.name
        KaomojiDelegate(Kaomoji.parse(kaomojiString)?.also { category._list.add(it) }
            ?: throw IllegalStateException("Kaomoji $kaomojiString could not be parsed."))
    }

    /**
     * Creates a new Kaomoji property based the property's name
     * and the given [leftArm], [leftEye],[mouth],[rightEye],[rightArm] and [accessory] ranges.
     */
    public fun parts(
        leftArm: IntRange = IntRange.EMPTY,
        leftEye: IntRange = IntRange.EMPTY,
        mouth: IntRange = IntRange.EMPTY,
        rightEye: IntRange = IntRange.EMPTY,
        rightArm: IntRange = IntRange.EMPTY,
        accessory: IntRange = IntRange.EMPTY,
    ): PropertyDelegateProvider<Category, KaomojiDelegate> = PropertyDelegateProvider { category, property ->
        KaomojiDelegate(Kaomoji(property.name, leftArm, leftEye, mouth, rightEye, rightArm, accessory).also { category._list.add(it) })
    }

    /**
     * Creates a new Kaomoji property based on the given [leftArm], [leftEye],[mouth],[rightEye],[rightArm] and [accessory].
     */
    public fun parts(
        leftArm: CharSequence,
        leftEye: CharSequence,
        mouth: CharSequence,
        rightEye: CharSequence,
        rightArm: CharSequence,
        accessory: CharSequence = "",
    ): PropertyDelegateProvider<Category, KaomojiDelegate> = PropertyDelegateProvider { category, _ ->
        KaomojiDelegate(Kaomoji(leftArm, leftEye, mouth, rightEye, rightArm, accessory).also { category._list.add(it) })
    }

    @JvmInline
    public value class KaomojiDelegate(public val kaomoji: Kaomoji) : ReadOnlyProperty<Category, Kaomoji> {
        override fun getValue(thisRef: Category, property: KProperty<*>): Kaomoji = kaomoji
    }
}

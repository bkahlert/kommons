package koodies.kaomoji

import kotlin.properties.PropertyDelegateProvider

public open class Category : AbstractList<Kaomoji>() {
    private val _list = mutableListOf<Kaomoji>()

    override val size: Int get() = _list.size
    override fun get(index: Int): Kaomoji = _list[index]

    /**
     * Creates a new Kaomoji property based on the property's name.
     *
     * If the kaomoji contains illegal characters [kaomoji] can be used to specify the correct one
     * while using a similar kaomoji with only legal characters as the property name itself.
     */
    public fun auto(manualKaomojiString: String? = null): PropertyDelegateProvider<Category, Kaomoji> = PropertyDelegateProvider { category, property ->
        val kaomojiString = manualKaomojiString ?: property.name
        val kaomoji = Kaomoji.parse(kaomojiString) ?: throw IllegalStateException("Kaomoji $kaomojiString could not be parsed.")
        kaomoji.also { category._list.add(it) }
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
    ): PropertyDelegateProvider<Category, Kaomoji> = PropertyDelegateProvider { category, property ->
        val kaomoji = Kaomoji(property.name, leftArm, leftEye, mouth, rightEye, rightArm, accessory)
        kaomoji.also { category._list.add(it) }
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
    ): PropertyDelegateProvider<Category, Kaomoji> = PropertyDelegateProvider { category, property ->
        val kaomoji = Kaomoji(leftArm, leftEye, mouth, rightEye, rightArm, accessory)
        kaomoji.also { category._list.add(it) }
    }
}

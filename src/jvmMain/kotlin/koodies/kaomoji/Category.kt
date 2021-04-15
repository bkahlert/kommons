package koodies.kaomoji

import koodies.text.CodePoint
import koodies.text.asCodePointSequence
import koodies.text.charCount
import kotlin.properties.PropertyDelegateProvider

public open class Category : AbstractList<Kaomojis.Kaomoji>() {
    private val _list = mutableListOf<Kaomojis.Kaomoji>()

    override val size: Int get() = _list.size
    override fun get(index: Int): Kaomojis.Kaomoji = _list[index]

    /**
     * Creates a new Kaomoji property based on the property's name.
     *
     * If the kaomoji contains illegal characters [kaomoji] can be used to specify the correct one
     * while using a similar kaomoji with only legal characters as the property name itself.
     */
    public fun auto(kaomoji: String? = null): PropertyDelegateProvider<Category, Kaomojis.Kaomoji> =
        PropertyDelegateProvider { category, property ->
            val template = kaomoji ?: property.name
            val parts = template.asCodePointSequence().toMutableList()
            val ranges = parts.runningFold(IntRange(0, -1)) { previousRange: IntRange, codePoint: CodePoint ->
                (previousRange.last + 1)..(previousRange.last + codePoint.charCount)
            }.drop(1)

            var leftArmRange: IntRange? = null
            var leftEyeRange: IntRange? = null
            var mouthRange: IntRange? = null
            var rightEyeRange: IntRange? = null
            var rightArmRange: IntRange? = null

            when (parts.size) {
                3 -> {
                    leftEyeRange = ranges.getOrNull(0)
                    mouthRange = ranges.getOrNull(1)
                    rightEyeRange = ranges.getOrNull(2)
                }
                4 -> {
                    leftEyeRange = ranges.getOrNull(0)
                    mouthRange = ranges.getOrNull(1)
                    rightEyeRange = ranges.getOrNull(2)
                    rightArmRange = ranges.getOrNull(3)
                }
                5 -> {
                    leftArmRange = ranges.getOrNull(0)
                    leftEyeRange = ranges.getOrNull(1)
                    mouthRange = ranges.getOrNull(2)
                    rightEyeRange = ranges.getOrNull(3)
                    rightArmRange = ranges.getOrNull(4)
                }
            }

            Kaomojis.Kaomoji(
                template,
                leftArmRange = leftArmRange,
                leftEyeRange = leftEyeRange,
                mouthRange = mouthRange,
                rightEyeRange = rightEyeRange,
                rightArmRange = rightArmRange,
            ).also {
                category._list.add(it)
            }
        }
}

package com.bkahlert.kommons.kaomoji

import kotlin.properties.PropertyDelegateProvider

/**
 * Collection of related [Kaomoji].
 */
public open class Category(
    private val elements: MutableList<Lazy<Kaomoji>> = mutableListOf(),
) : AbstractList<Kaomoji>() {
    private fun add(block: () -> Kaomoji): Lazy<Kaomoji> = lazy(block).also { elements.add(it) }

    override val size: Int get() = elements.size
    override fun get(index: Int): Kaomoji = elements[index].value

    /**
     * Creates a new [Kaomoji] property based on the property's name.
     */
    protected fun parsing(): PropertyDelegateProvider<Category, Lazy<Kaomoji>> = PropertyDelegateProvider { _, property ->
        add { Kaomoji.parse(property.name) }
    }

    /**
     * Creates a new [Kaomoji] property based on the specified [text].
     */
    protected fun parsing(text: String): Lazy<Kaomoji> = add { Kaomoji.parse(text) }

    /**
     * Creates a new [Kaomoji] property based the property's name
     * and the given [leftArm], [leftEye],[mouth],[rightEye],[rightArm] and [accessory] ranges.
     */
    protected fun parts(
        leftArm: IntRange = IntRange.EMPTY,
        leftEye: IntRange = IntRange.EMPTY,
        mouth: IntRange = IntRange.EMPTY,
        rightEye: IntRange = IntRange.EMPTY,
        rightArm: IntRange = IntRange.EMPTY,
        accessory: IntRange = IntRange.EMPTY,
    ): PropertyDelegateProvider<Category, Lazy<Kaomoji>> = PropertyDelegateProvider { _, property ->
        add { Kaomoji(property.name, leftArm, leftEye, mouth, rightEye, rightArm, accessory) }
    }

    /**
     * Creates a new [Kaomoji] property based on the given [leftArm], [leftEye],[mouth],[rightEye],[rightArm] and [accessory].
     */
    protected fun parts(
        leftArm: String,
        leftEye: String,
        mouth: String,
        rightEye: String,
        rightArm: String,
        accessory: String = "",
    ): Lazy<Kaomoji> = add { Kaomoji(leftArm, leftEye, mouth, rightEye, rightArm, accessory) }
}

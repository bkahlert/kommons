package com.bkahlert.kommons.kaomoji

public enum class Generator(
    public val leftArm: List<String>,
    public val rightArm: List<String>,
    public val leftEye: List<String>,
    public val rightEye: List<String>,
    public val mouth: List<String>,
) {
    INDIFFERENCE(
        leftArm = listOf("ヽ", "┐", "╮", "ᕕ", "¯\\_"),
        rightArm = listOf("ノ", "┌", "╭", "ᕗ", "_/¯"),
        leftEye = listOf("ー", " ´ ", "︶", "￣", "´", " ˘ ", "‘"),
        rightEye = listOf("ー", " ` ", "︶", "￣", "´", " ˘ ", "` "),
        mouth = listOf("_", "ヘ", "～", "д", "▽", "ヮ", "ー", "︿", "､")
    ),
    JOY(
        leftArm = listOf("╰", "＼", "٩", "<"),
        rightArm = listOf("ﾉ", "ノ", "o", "／"),
        leftEye = listOf("▔", "^", "¯", "☆"),
        rightEye = listOf("▔", "^", "¯", "☆"),
        mouth = listOf("▽", "ω", "ヮ", "∀")
    ),
    LOVE(
        leftArm = listOf("", "♡╰", "ヽ", "♡＼", "٩", "❤ "),
        rightArm = listOf("", "ノ", "♡", "╯♡", " ♡", " ❤", "/ ♡", "ノ～ ♡", "۶"),
        leftEye = listOf("─", "´ ", "• ", "*", "˘", "μ", "￣", " ◡", "°", "♡", "◕", "˙", "❤", "´• ", "≧"),
        rightEye = listOf("─", " `", "• ", "*", "˘", "μ", "￣", " ◡", "°", "♡", "◕", "˙", "❤", " •`", "≦"),
        mouth = listOf("з", "_", "‿‿", "ω", "︶", "◡", "▽", "ε", "∀", "ᵕ", "‿", "³")
    ),
    SADNESS(
        leftArm = listOf("", "o", ".･ﾟﾟ･", "。゜゜", "｡･ﾟﾟ*", "｡･ﾟ", ".｡･ﾟﾟ･", "｡ﾟ", "･ﾟ･", "｡ﾟ･ "),
        rightArm = listOf("", "o", "･ﾟﾟ･.", " ゜゜。", "*ﾟﾟ･｡", "･｡", "･ﾟﾟ･｡.", "･ﾟ･", "･ﾟ｡"),
        leftEye = listOf("μ", "T", "╥", "〒", "-", " ; ", "个", "╯", "ಥ", ">", "｡•́", "╯"),
        rightEye = listOf("μ", "T", "╥", "〒", "-", " ; ", "个", "╯", "ಥ", "<。", "•̀｡", "<、"),
        mouth = listOf("_", "ヘ", "ω", "﹏", "Д", "︿", "-ω-", "︵", "╭╮", "Ｏ", "><")
    ),
    ;

    public fun random(
        leftArm: String? = null,
        leftEye: String? = null,
        mouth: String? = null,
        rightEye: String? = null,
        rightArm: String? = null,
        accessory: String = "",
    ): Kaomoji = Kaomoji(
        leftArm ?: this.leftArm.random(),
        leftEye ?: this.leftEye.random(),
        mouth ?: this.mouth.random(),
        rightEye ?: this.rightEye.random(),
        rightArm ?: this.rightArm.random(),
        accessory
    )

    public companion object {
        public val leftArms: List<String> = values().flatMap { it.leftArm }
        public val rightArms: List<String> = values().flatMap { it.rightArm }
        public val leftEyes: List<String> = values().flatMap { it.leftEye }
        public val rightEyes: List<String> = values().flatMap { it.rightEye }
        public val mouths: List<String> = values().flatMap { it.mouth }

        public fun CharSequence.removeRightArm(): CharSequence {
            val rightArm = rightArms.dropWhile { !this.endsWith(it) }
            return if (rightArm.isNotEmpty()) this.removeSuffix(rightArm.first()) else this
        }

        public fun random(
            leftArm: String? = null,
            leftEye: String? = null,
            mouth: String? = null,
            rightEye: String? = null,
            rightArm: String? = null,
            accessory: String = "",
        ): Kaomoji = values().random().random(leftArm, leftEye, mouth, rightEye, rightArm, accessory)
    }
}

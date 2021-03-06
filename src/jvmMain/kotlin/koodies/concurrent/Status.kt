package koodies.concurrent

import koodies.logging.ReturnValue
import koodies.number.toPositiveInt

public inline class Status(public val status: Byte) : ReturnValue {
    public constructor(status: Int) : this(status.toByte())

    override val successful: Boolean get() = this == SUCCESS
    public val failed: Boolean get() = this != SUCCESS

    override fun format(): CharSequence = toString()
    override fun toString(): String = "${status.toPositiveInt().toSansSerifBoldString()}↩"

    public companion object {
        private val SANS_SERIF_BOLD_DIGITS = arrayOf("𝟬", "𝟭", "𝟮", "𝟯", "𝟰", "𝟱", "𝟲", "𝟳", "𝟴", "𝟵")
        private val MONOSPACE_DIGITS = arrayOf("𝟶", "𝟷", "𝟸", "𝟹", "𝟺", "𝟻", "𝟼", "𝟽", "𝟾", "𝟿")
        private fun Int.toSansSerifBoldString() = toString().map {
            val digit = it.toString().toInt()
            MONOSPACE_DIGITS[digit]
        }.joinToString("")

        public val SUCCESS: Status = Status(0)
        public val FAILURE: Status = Status(1)
    }
}

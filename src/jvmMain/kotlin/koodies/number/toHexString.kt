package koodies.number

val hexChars = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

fun Int.toHexString(pad: Boolean = false): String {
    var rem: Int
    var decimal = this
    val hex = StringBuilder()
    if (decimal == 0) return if (pad) "00" else "0"
    while (decimal > 0) {
        rem = decimal % 16
        hex.append(hexChars[rem])
        decimal /= 16
    }
    return hex.reversed().toString().let {
        if (it.length.mod(2) == 1 && pad) "0$it" else it
    }
}

package koodies.number

object Constants {

    const val TWO_POW_128_PLUS_1_HEX_STRING =
        "01" +
            "00" + "00" + "00" + "00" +
            "00" + "00" + "00" + "00" +
            "00" + "00" + "00" + "00" +
            "00" + "00" + "00" + "00"
    const val TWO_POW_128_PLUS_1_DEC_STRING =
        "340282366920938463463374607431768211456"
    const val TWO_POW_128_PLUS_1_BIN_STRING =
        "00000001" +
            "00000000" + "00000000" + "00000000" + "00000000" +
            "00000000" + "00000000" + "00000000" + "00000000" +
            "00000000" + "00000000" + "00000000" + "00000000" +
            "00000000" + "00000000" + "00000000" + "00000000"
    val TWO_POW_128_PLUS_1_BYTES = byteArrayOf(
        0x1,
        0x0, 0x0, 0x0, 0x0,
        0x0, 0x0, 0x0, 0x0,
        0x0, 0x0, 0x0, 0x0,
        0x0, 0x0, 0x0, 0x0,
    )
    val TWO_POW_128_PLUS_1_UBYTES = ubyteArrayOf(
        0x1u,
        0x0u, 0x0u, 0x0u, 0x0u,
        0x0u, 0x0u, 0x0u, 0x0u,
        0x0u, 0x0u, 0x0u, 0x0u,
        0x0u, 0x0u, 0x0u, 0x0u,
    )

    const val TWO_POW_128_HEX_STRING = "" +
        "ff" + "ff" + "ff" + "ff" +
        "ff" + "ff" + "ff" + "ff" +
        "ff" + "ff" + "ff" + "ff" +
        "ff" + "ff" + "ff" + "ff"
    const val TWO_POW_128_DEC_STRING =
        "340282366920938463463374607431768211455"
    const val TWO_POW_128_BIN_STRING = "" +
        "11111111" + "11111111" + "11111111" + "11111111" +
        "11111111" + "11111111" + "11111111" + "11111111" +
        "11111111" + "11111111" + "11111111" + "11111111" +
        "11111111" + "11111111" + "11111111" + "11111111"
    val TWO_POW_128_BYTES = byteArrayOf(
        -0x01, -0x01, -0x01, -0x01,
        -0x01, -0x01, -0x01, -0x01,
        -0x01, -0x01, -0x01, -0x01,
        -0x01, -0x01, -0x01, -0x01,
    )
    val TWO_POW_128_UBYTES = ubyteArrayOf(
        0xffu, 0xffu, 0xffu, 0xffu,
        0xffu, 0xffu, 0xffu, 0xffu,
        0xffu, 0xffu, 0xffu, 0xffu,
        0xffu, 0xffu, 0xffu, 0xffu,
    )
}

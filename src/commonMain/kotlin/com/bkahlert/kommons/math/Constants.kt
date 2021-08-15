package com.bkahlert.kommons.math

public object Constants {

    public const val TWO_POW_128_PLUS_1_HEX_STRING: String =
        "01" +
            "00" + "00" + "00" + "00" +
            "00" + "00" + "00" + "00" +
            "00" + "00" + "00" + "00" +
            "00" + "00" + "00" + "00"
    public const val TWO_POW_128_PLUS_1_DEC_STRING: String =
        "340282366920938463463374607431768211456"
    public const val TWO_POW_128_PLUS_1_BIN_STRING: String =
        "00000001" +
            "00000000" + "00000000" + "00000000" + "00000000" +
            "00000000" + "00000000" + "00000000" + "00000000" +
            "00000000" + "00000000" + "00000000" + "00000000" +
            "00000000" + "00000000" + "00000000" + "00000000"
    public val TWO_POW_128_PLUS_1_BYTES: ByteArray = byteArrayOf(
        0x1,
        0x0, 0x0, 0x0, 0x0,
        0x0, 0x0, 0x0, 0x0,
        0x0, 0x0, 0x0, 0x0,
        0x0, 0x0, 0x0, 0x0,
    )
    public val TWO_POW_128_PLUS_1_UBYTES: UByteArray = ubyteArrayOf(
        0x1u,
        0x0u, 0x0u, 0x0u, 0x0u,
        0x0u, 0x0u, 0x0u, 0x0u,
        0x0u, 0x0u, 0x0u, 0x0u,
        0x0u, 0x0u, 0x0u, 0x0u,
    )

    public const val TWO_POW_128_HEX_STRING: String = "" +
        "ff" + "ff" + "ff" + "ff" +
        "ff" + "ff" + "ff" + "ff" +
        "ff" + "ff" + "ff" + "ff" +
        "ff" + "ff" + "ff" + "ff"
    public const val TWO_POW_128_DEC_STRING: String =
        "340282366920938463463374607431768211455"
    public const val TWO_POW_128_BIN_STRING: String = "" +
        "11111111" + "11111111" + "11111111" + "11111111" +
        "11111111" + "11111111" + "11111111" + "11111111" +
        "11111111" + "11111111" + "11111111" + "11111111" +
        "11111111" + "11111111" + "11111111" + "11111111"
    public val TWO_POW_128_BYTES: ByteArray = byteArrayOf(
        -0x01, -0x01, -0x01, -0x01,
        -0x01, -0x01, -0x01, -0x01,
        -0x01, -0x01, -0x01, -0x01,
        -0x01, -0x01, -0x01, -0x01,
    )
    public val TWO_POW_128_UBYTES: UByteArray = ubyteArrayOf(
        0xffu, 0xffu, 0xffu, 0xffu,
        0xffu, 0xffu, 0xffu, 0xffu,
        0xffu, 0xffu, 0xffu, 0xffu,
        0xffu, 0xffu, 0xffu, 0xffu,
    )
}

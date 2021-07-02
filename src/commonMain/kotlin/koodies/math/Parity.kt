package koodies.math

/** `true` if even. */
public val Byte.isEven: Boolean get():Boolean = this % 2 == 0

/** `true` if even. */
public val Short.isEven: Boolean get():Boolean = this % 2 == 0

/** `true` if even. */
public val Int.isEven: Boolean get():Boolean = this % 2 == 0

/** `true` if even. */
public val BigInteger.isEven: Boolean get():Boolean = takeLowestOneBit() != 0


/** `true` if odd. */
public val Byte.isOdd: Boolean get():Boolean = this % 2 != 0

/** `true` if odd. */
public val Short.isOdd: Boolean get():Boolean = this % 2 != 0

/** `true` if odd. */
public val Int.isOdd: Boolean get():Boolean = this % 2 != 0

/** `true` if odd. */
public val BigInteger.isOdd: Boolean get():Boolean = takeLowestOneBit() == 0

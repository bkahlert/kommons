package koodies.text

import koodies.unit.Size
import koodies.unit.bytes

public val CharSequence.utf8Bytes: Size get() = utf8.size.bytes

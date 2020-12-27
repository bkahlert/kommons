package koodies.text

import koodies.unit.Size
import koodies.unit.Size.Companion.bytes

val CharSequence.utf8Bytes: Size get() = utf8.size.bytes

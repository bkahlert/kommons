package com.bkahlert.kommons.text

import com.bkahlert.kommons.unit.Size
import com.bkahlert.kommons.unit.bytes

public val CharSequence.utf8Bytes: Size get() = utf8.size.bytes

package com.bkahlert.kommons.test

import com.bkahlert.kommons.createTempDirectory
import com.bkahlert.kommons.io.path.renameTo
import com.bkahlert.kommons.test.fixtures.HtmlDocumentFixture
import com.bkahlert.kommons.test.fixtures.UnicodeTextDocumentFixture
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.listDirectoryEntries

public object Fixtures {

    public fun Path.directoryWithTwoFiles(): Path = createTempDirectory().also {
        HtmlDocumentFixture.copyToDirectory(it)
        UnicodeTextDocumentFixture.copyToDirectory(it.resolve("sub-dir").createDirectory()).renameTo("config.txt")
    }.apply { check(listDirectoryEntries().size == 2) { "Failed to provide directory with two files." } }
}

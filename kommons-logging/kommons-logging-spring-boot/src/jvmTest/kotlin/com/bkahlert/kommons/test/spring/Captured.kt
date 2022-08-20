package com.bkahlert.kommons.test.spring

import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.springframework.boot.test.system.OutputCaptureExtension

/**
 * Annotation that can be used to annotate a test parameter the same
 * way as it's done with [TempDir].
 */
@ExtendWith(OutputCaptureExtension::class)
annotation class Captured

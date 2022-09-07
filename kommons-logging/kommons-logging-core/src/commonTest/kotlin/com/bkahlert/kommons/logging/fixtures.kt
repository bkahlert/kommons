package com.bkahlert.kommons.logging

import mu.KotlinLogging

class ClassWithDerivedLoggerField {
    val logger by KotlinLogging
}

class ClassWithCompanionWithDerivedLoggerField {
    companion object {
        val logger by KotlinLogging
    }
}

class ClassWithNamedCompanionWithDerivedLoggerField {
    companion object Named {
        val logger by KotlinLogging
    }
}

object SingletonWithDerivedLoggerField {
    val logger by KotlinLogging
}

val logger by KotlinLogging

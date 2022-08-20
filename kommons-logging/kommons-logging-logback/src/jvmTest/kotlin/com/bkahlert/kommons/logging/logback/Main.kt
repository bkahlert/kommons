package com.bkahlert.kommons.logging.logback

import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger(LoggerFactory::class.java)
    logger.info("Hello World")
}

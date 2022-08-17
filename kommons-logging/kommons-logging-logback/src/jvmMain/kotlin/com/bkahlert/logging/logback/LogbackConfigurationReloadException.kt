package com.bkahlert.logging.logback

public class LogbackConfigurationReloadException(cause: Throwable?) :
    RuntimeException("Logback logger context failed to reload. Check the README.md to find out how to enable debug mode.", cause)

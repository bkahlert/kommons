package com.bkahlert.kommons.logging.logback

import ch.qos.logback.classic.spi.ILoggingEvent
import org.springframework.boot.ansi.AnsiOutput
import org.springframework.boot.logging.logback.ColorConverter

/**
 * A color converter that delegates to [ColorConverter] but pretends
 * an existing console to enable ANSI color output in development environments
 * where the support detection of ANSI escape sequences is typically poor.
 */
public class ConsolePretendingColorConverter : ColorConverter() {
    /**
     * Delegates to [ColorConverter.transform] but also sets [AnsiOutput.consoleAvailable]
     * to `true`.
     */
    protected override fun transform(event: ILoggingEvent?, `in`: String?): String {
        AnsiOutput.setConsoleAvailable(true)
        return super.transform(event, `in`)
    }
}

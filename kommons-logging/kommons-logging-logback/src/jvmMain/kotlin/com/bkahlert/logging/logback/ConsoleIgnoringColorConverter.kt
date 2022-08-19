package com.bkahlert.logging.logback

import ch.qos.logback.classic.spi.ILoggingEvent
import org.springframework.boot.ansi.AnsiElement
import org.springframework.boot.ansi.AnsiOutput
import org.springframework.boot.logging.logback.ColorConverter

/**
 * Color converter that ignores the existence of a console in order to allow for
 * ANSI formatted logging.
 */
public class ConsoleIgnoringColorConverter : ColorConverter() {
    // TODO make configurable
    protected override fun transform(event: ILoggingEvent?, `in`: String?): String {
        AnsiOutput.setConsoleAvailable(true)
        return super.transform(event, `in`)
    }

    override fun toAnsiString(`in`: String?, element: AnsiElement?): String? {
        AnsiOutput.setConsoleAvailable(true)
        return AnsiOutput.toString(element, `in`)
    }
}

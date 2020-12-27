package koodies.terminal

import com.github.ajalt.mordant.AnsiColorCode
import com.github.ajalt.mordant.TermColors

typealias ColorProvider = (TermColors.() -> AnsiColorCode)

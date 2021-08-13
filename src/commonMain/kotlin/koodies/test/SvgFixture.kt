package koodies.test

import koodies.io.InMemoryImage

/**
 * A SVG file [InMemoryFile] showing the Koodies logo.
 */
public object SvgFixture : Fixture<ByteArray>, InMemoryImage(
    "koodies.svg", """
        <svg xmlns="http://www.w3.org/2000/svg" aria-label="Koodies—Random Kotlin Goodies" role="img" viewBox="0 0 60 60" style="cursor: default;">
            <path fill="url(#C)" d="M60 30v30H30z"/>
            <path fill="url(#A)" d="M0 60l27.5-30L55 60z"/>
            <linearGradient id="A" gradientUnits="userSpaceOnUse" x1="0" y1="70" x2="30" y2="40">
                <stop offset=".3" stop-color="#00dada"/>
                <stop offset="1" stop-color="#dd4f92"/>
            </linearGradient>
            <path fill="url(#B)" d="M32.5 27.5L60 55V30H35z"/>
            <linearGradient id="B" gradientUnits="userSpaceOnUse" x1="0" y1="30" x2="60" y2="90" gradientTransform="matrix(0 1 1 0 3 9)">
                <stop offset=".1" stop-color="#f1ea00"/>
                <stop offset=".8" stop-color="#ec5e58"/>
            </linearGradient>
            <path fill="url(#C)" d="M30 0h30L0 60V30z"/>
            <linearGradient id="C" gradientUnits="userSpaceOnUse" x1="0" y1="55" x2="45" y2="10">
                <stop offset=".1" stop-color="#c757bc"/>
                <stop offset=".2" stop-color="#d0609a"/>
                <stop offset=".4" stop-color="#e1725c"/>
                <stop offset=".6" stop-color="#ee7e2f"/>
                <stop offset=".7" stop-color="#f58613"/>
                <stop offset=".8" stop-color="#f88909"/>
            </linearGradient>
            <path fill="url(#D)" d="M0 0v30L30 0z"/>
            <linearGradient id="D" gradientUnits="userSpaceOnUse" x1="0" y1="10" x2="20" y2="-5">
                <stop offset=".118" stop-color="#0095d5"/>
                <stop offset=".418" stop-color="#3c83dc"/>
                <stop offset=".696" stop-color="#6d74e1"/>
                <stop offset=".833" stop-color="#806ee3"/>
            </linearGradient>
        </svg>
    """.trimIndent()) {
    override val contents: ByteArray get() = data
}

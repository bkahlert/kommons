package com.bkahlert.kommons.test

import com.bkahlert.kommons.io.InMemoryImage

/**
 * A SVG file [InMemoryFile] showing the Kommons logo.
 */
public object SvgFixture : Fixture<ByteArray>, InMemoryImage(
    "kommons.svg", """
        <svg version="1.1" xmlns="http://www.w3.org/2000/svg" aria-label="Kommons" role="img" viewBox="0 0 60 60" style="cursor: default;">
            <defs>
                <linearGradient id="upper-k" x1="-720" y1="780" x2="0" y2="60">
                    <stop offset="0" stop-color="#29abe2"/>
                    <stop offset=".1" stop-color="#a35ebb"/>
                    <stop offset=".2" stop-color="#0090aa"/>
                    <stop offset=".3" stop-color="#01818f"/>
                    <stop offset=".3" stop-color="#01818f"/>
                    <stop offset=".45" stop-color="#00978b"/>
                    <stop offset=".6" stop-color="#648be0"/>
                    <stop offset=".7" stop-color="#009ec6"/>
                    <stop offset=".8" stop-color="#29abe2"/>
                    <stop offset=".9" stop-color="#04a971"/>
                    <stop offset="1" stop-color="#04a971"/>
                    <animate attributeName="x1" dur="60s" values="0; -720; 0" repeatCount="indefinite"/>
                    <animate attributeName="y1" dur="60s" values="60; 780; 60" repeatCount="indefinite"/>
                    <animate attributeName="x2" dur="60s" values="720; 0; 720" repeatCount="indefinite"/>
                    <animate attributeName="y2" dur="60s" values="-660; 60; -660" repeatCount="indefinite"/>
                </linearGradient>
                <linearGradient id="strip" x1="0" y1="60" x2="300" y2="-240">
                    <stop offset="0" stop-color="#C757BC"/>
                    <stop offset=".025" stop-color="#D0609A"/>
                    <stop offset=".05" stop-color="#E1725C"/>
                    <stop offset=".075" stop-color="#EE7E2F"/>
                    <stop offset=".1" stop-color="#F58613"/>
                    <stop offset=".2" stop-color="#F88909"/>
                    <stop offset=".5" stop-color="#ff9234"/>
                    <stop offset="1" stop-color="#ebb21d"/>
                    <!--            <stop offset=".95" stop-color="#F58613"/>-->
                    <!--            <stop offset=".975" stop-color="#f74f57"/>-->
                    <!--            <stop offset="1" stop-color="#c21e73"/>-->
                    <animate attributeName="x1" dur="20s" values="0; -300; 0" repeatCount="indefinite"/>
                    <animate attributeName="y1" dur="20s" values="60; 360; 60" repeatCount="indefinite"/>
                    <animate attributeName="x2" dur="20s" values="300; 0; 300" repeatCount="indefinite"/>
                    <animate attributeName="y2" dur="20s" values="-240; 60; -240" repeatCount="indefinite"/>
                </linearGradient>
                <linearGradient id="lower-k" x1="0" y1="60" x2="600" y2="-540">
                    <stop offset="0" stop-color="#29abe2"/>
                    <stop offset=".1" stop-color="#a35ebb"/>
                    <stop offset=".2" stop-color="#0090aa"/>
                    <stop offset=".3" stop-color="#01818f"/>
                    <stop offset=".45" stop-color="#00978b"/>
                    <stop offset=".6" stop-color="#648be0"/>
                    <stop offset=".7" stop-color="#009ec6"/>
                    <stop offset=".8" stop-color="#29abe2"/>
                    <stop offset=".9" stop-color="#04a971"/>
                    <stop offset="1" stop-color="#04a971"/>
                    <animate attributeName="x1" dur="80s" values="0; -600; 0" repeatCount="indefinite"/>
                    <animate attributeName="y1" dur="80s" values="60; 660; 60" repeatCount="indefinite"/>
                    <animate attributeName="x2" dur="80s" values="600; 0; 600" repeatCount="indefinite"/>
                    <animate attributeName="y2" dur="80s" values="-540; 60; -540" repeatCount="indefinite"/>
                </linearGradient>
            </defs>
            <polygon points="0,0 30.5,0 0,30.5" fill="url(#upper-k)"/>
            <polygon points="30.5,0 0,30.5 0,60 60,0" fill="url(#strip)"/>
            <polygon points="0,60 30,30 60,60" fill="url(#lower-k)"/>
        </svg>
    """.trimIndent()) {
    override val contents: ByteArray get() = data
}

package com.bkahlert.kommons.test.fixtures

/**
 * A [TextResourceFixture] encompassing a Scalable Vector Graphic (SVG)
 * showing the [Kommons](https://github.com/bkahlert/kommons) logo.
 */
public object SvgImageFixture : TextResourceFixture(
    "kommons.svg",
    "image/svg+xml",
    """
    <svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" aria-label="Kommons" role="img" viewBox="0 0 375 60" style="cursor: default;">
        <style>
            path { fill: #231F20; }
            @media (prefers-color-scheme: dark) { text { color: #DCE0DF; } }
        </style>
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
            <path id="o"
                  d="M73.9,60c-27.6,2.9-27.6-49.8,0-47C101.4,10.1,101.4,62.9,73.9,60z
                     M65.7,47.7c3.1,4.8,13.2,4.8,16.3,0 c3.6-4.2,3.5-18.2,0.1-22.3c-3-4.8-13.3-4.8-16.3,0C62.5,29.6,62.1,43.5,65.7,47.7z"/>
            <path id="m"
                  d="M104.4,58.9V14.1h7.6l0.7,5.3c6.1-7.9,19.8-9.2,24.4,0c5.9-7.1,17.7-9,23.6-2.3c6.2,6,2.6,33.6,3.5,41.7h-9.5V30.7
                     c0.5-10.5-10.6-10.8-15.6-3.5c0.2,0.1,0.1,30.9,0.1,31.8h-9.5V30.8c0.4-11-10.8-10.9-15.6-2.8v30.9H104.4L104.4,58.9z"/>
        </defs>
        <polygon points="0,0 30.5,0 0,30.5" fill="url(#upper-k)"/>
        <polygon points="30.5,0 0,30.5 0,60 60,0" fill="url(#strip)"/>
        <polygon points="0,60 30,30 60,60" fill="url(#lower-k)"/>
        <use xlink:href="#o"/>
        <use xlink:href="#m"/>
        <use xlink:href="#m" x="71.5"/>
        <use xlink:href="#o" x="192.3"/>
        <path d="M296.2,58.9V14.1h7.6l0.7,5.3c10.4-11,29.5-8.1,28.2,10.6c0,0,0,28.9,0,28.9h-9.5c-0.6-5.2,1.6-30.4-1.7-34.4
                 c-4.2-4.6-12.1-1.9-15.8,3.4V59L296.2,58.9L296.2,58.9z"/>
        <path d="M357.4,60c-6.7,0-12-1.2-15.9-3.7v-9.2c5.9,3.8,16.1,6.2,22.5,3.3c6.8-7.6-4.7-9.3-10.5-10.4c-7.8-1.8-11.2-4.9-11.4-12.7
                 c-1.8-15,20.4-16.9,30.4-11.5V25c-17-9.7-30.3,2.9-14.7,6.4c4.5,1,12.3,2.9,14.4,5.8C379.3,47.6,372.7,61.2,357.4,60z"/>
    </svg>
    """.trimIndent()
)

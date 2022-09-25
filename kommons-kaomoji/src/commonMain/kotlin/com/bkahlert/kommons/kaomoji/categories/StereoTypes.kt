@file:Suppress(
    "KDocMissingDocumentation",
    "ObjectPropertyName",
    "RemoveRedundantBackticks",
    "unused",
    "NonAsciiCharacters",
    "SpellCheckingInspection"
)

package com.bkahlert.kommons.kaomoji.categories

import com.bkahlert.kommons.kaomoji.Category
import com.bkahlert.kommons.kaomoji.Kaomoji

public object StereoTypes : Category() {

    public val ORLY: Kaomoji by parsing(
        """
        ,___,
        [O.o] - O RLY?
        /)__)
        -"--"-
    """.trimIndent()
    )

    public val Hare: Kaomoji by parsing(
        """
        (\_/)
        (O.o)
        (> <)
        /_|_\
    """.trimIndent()
    )

    public val Cthulhu: Kaomoji by parsing(
        """
          (jIj)
          (;,;)
         (o,.,O)
        Y(O,,,,O)Y
    """.trimIndent()
    )

    /**
     * Stereotypical Korean character
     */
    public val Nidā: Kaomoji by parsing(
        """
        <丶｀∀´>
    """.trimIndent()
    )

    /**
     * Stereotypical North Korean character
     */
    public val Kigā: Kaomoji by parsing(
        """
        ［　(★)　］
         <丶´Д｀>
    """.trimIndent()
    )

    /**
     * Stereotypical Japanese character
     */
    public val Monā: Kaomoji by parsing(
        """
            ∧＿∧
        （ ；´Д｀）
        """.trimIndent()
    )

    /**
     * Stereotypical Chinese Korean character
     */
    public val Sinā: Kaomoji by parsing(
        """
            ∧∧
          ／ 中＼
        （ 　｀ハ´）
        """.trimIndent()
    )

    /**
     * Stereotypical Taiwanese character
     */
    public val Wanā: Kaomoji by parsing(
        """
              ∧∧
           ／　台＼
         （　＾∀＾）
        """.trimIndent()
    )

    /**
     * Stereotypical Vietamese character
     */
    public val Venā: Kaomoji by parsing(
        """
           ∧∧
         ／ 越 ＼
        （ ・∀・ ）
        """.trimIndent()
    )

    /**
     * Stereotypical Indian character
     */
    public val Monastē: Kaomoji by parsing(
        """ 
          γ~三ヽ 
         (三彡０ﾐ) 
        （　´∀｀）
        """.trimIndent()
    )

    /**
     * Stereotypical American character
     */
    public val Samū: Kaomoji by parsing(
        """ 
          |￣￣| 
         ＿☆☆☆＿ 
        （ ´_⊃｀）
        """.trimIndent()
    )

    /**
     * Stereotypical Jewish character
     */
    public val Yudā: Kaomoji by parsing(
        """
            ┏━┓
           ━━━━━━
           ﾐΘc_Θ-ﾐ
        """.trimIndent()
    )

    /**
     * Stereotypical English character
     */
    public val Jakū: Kaomoji by parsing(
        """ 
           ＿＿ 
          │〓.│ 
          ━━━━━
        ﾐ　´_＞｀）
        """.trimIndent()
    )

    /**
     * Stereotypical French character
     */
    public val Torirī: Kaomoji by parsing(
        """  
          ____ 
        （〓__＞
        ξ ・_>・）
        """.trimIndent()
    )

    /**
     * Stereotypical German character
     */
    public val Gerumandamu: Kaomoji by parsing(
        """ 
         _、,_ 
        ﾐ　　_⊃）
        """.trimIndent()
    )

    /**
     * Stereotypical Austrian character
     */
    public val Osutō: Kaomoji by parsing(
        """  
           ≡≡彡
        彡 ´_)｀ ）
        """.trimIndent()
    )

    /**
     * Stereotypical Russian character
     */
    public val Rosukī: Kaomoji by parsing(
        """
         ,,,,,,,,,,,,, 
         ﾐ;;;,,,,,,,ﾐ　 
          （　｀_っ´）
        """.trimIndent()
    )

    /**
     * Stereotypical Mexican character
     */
    public val Amīgo: Kaomoji by parsing(
        """ 
            _γ⌒ヽ_
          lXXXXXXXXl
         （　´ｍ｀）
        """.trimIndent()
    )

    /**
     * Stereotypical Persian character
     */
    public val Jujø: Kaomoji by parsing(
        """
                 _
           <(o0o)>
        (>ミ — ミ)>
    """.trimIndent()
    )
}

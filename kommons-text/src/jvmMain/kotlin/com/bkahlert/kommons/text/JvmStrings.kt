package com.bkahlert.kommons.text

import com.ibm.icu.text.Transliterator

/**
 * Returns this string transformed according to the specified [id].
 *
 * **Example:**
 * Use `Latin-ASCII` to convert non-ASCII-range punctuation, symbols,
 * and Latin letters in an approximate ASCII-range equivalent (e.g. `©` to `(C)` and `Æ` to `AE`).
 *
 * @see <a href="https://unicode-org.github.io/icu/userguide/transforms/general/#formal-id-syntax">Formal ID Syntax</a>
 * @see <a href="https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/">ICU4J API Specification</a>
 */
public fun String.transform(id: String): String =
    Transliterator.getInstance(id).transliterate(this)

/**
 * Returns this string transformed according to a
 * [compound ID](https://unicode-org.github.io/icu/userguide/transforms/general/#compound-ids)
 * built using the specified [ids],
 * the optional [globalFilter] (e.g. `[:Latin:]`, `[\p{Lu}]`) and
 * the optional [globalInverseFilter].
 *
 * **Example:**
 * Use `Latin-ASCII` to convert non-ASCII-range punctuation, symbols,
 * and Latin letters in an approximate ASCII-range equivalent (e.g. `©` to `(C)` and `Æ` to `AE`).
 *
 * @see <a href="https://unicode-org.github.io/icu/userguide/transforms/general/#compound-ids">Compound IDs</a>
 * @see <a href="https://unicode-org.github.io/icu-docs/apidoc/released/icu4j/">ICU4J API Specification</a>
 */
public fun String.transform(
    vararg ids: String,
    globalFilter: String? = null,
    globalInverseFilter: String? = null,
): String = transform(
    listOfNotNull(
        globalFilter,
        *ids,
        globalInverseFilter?.let { "($it)" },
    ).joinToString("; ", postfix = ";")
)

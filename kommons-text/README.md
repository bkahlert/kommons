# Kommons Text

## About

**Kommons Text** is a Kotlin Multiplatform Library that offers:

1. the [Unicode-aware string abstraction Text](#unicode-operations)
2. a couple of [string operations](#string-operations)
3. [regex operations](#regular-operations) such as the possibility to use glob patterns

## Installation / setup

This library is hosted on GitHub with releases provided on Maven Central.

* **Gradle** `implementation("com.bkahlert.kommons:kommons-text:2.1.0")

* **Maven**
  ```xml
  <dependency>
      <groupId>com.bkahlert.kommons</groupId>
      <artifactId>kommons-text</artifactId>
      <version>2.1.0</version>
  </dependency>
  ```

## Features

### Unicode Operations

Handling user input requires functions to handle Unicode correctly,
unless you're not afraid of the following:

```kotlin
"👨🏾‍🦱".substring(0, 3) // "👨?", skin tone gone, curly hair gone
"👩‍👩‍👦‍👦".substring(1, 7) // "?‍👩‍?", wife gone, kids gone
```

Decode any string to a sequence / list of code points using `String.asCodePointSequence` / `String.toCodePointList`.

Decode any string to a sequence / list of graphemes using `String.asGraphemeSequence` / `String.toGraphemeList`.

Use `truncate`/`truncateStart`/`truncateEnd` for reduce the number of characters, codepoints or graphemes.

Transliterations and transforms can be done using `String.transform`.

#### Examples

```kotlin
"a".asCodePoint().name     // "LATIN SMALL LETTER A"
"a𝕓c̳🔤".toCharArray()      // "a", "?", "?", "c", "̳", "?", "?"
"a𝕓c̳🔤".toCodePointList()  // "a", "𝕓", "c", "̳", "🫠"
"a𝕓c̳🔤".toGraphemeList()   // "a", "𝕓", "c̳", "🫠"

"a𝕓🫠🇩🇪👨🏾‍🦱👩‍👩‍👦‍👦".length                    // 27 (= number of Java chars)
"a𝕓🫠🇩🇪👨🏾‍🦱👩‍👩‍👦‍👦".asText(CodePoint).length  // 16 (= number of Unicode code points)
"a𝕓🫠🇩🇪👨🏾‍🦱👩‍👩‍👦‍👦".asText(Grapheme).length   //  6 (= visually perceivable units)

"a𝕓🫠🇩🇪👨🏾‍🦱👩‍👩‍👦‍👦".truncate(7.characters)  // "a\uD835 … 👦"
"a𝕓🫠🇩🇪👨🏾‍🦱👩‍👩‍👦‍👦".truncate(7.codePoints)  // "a𝕓 … ‍👦"
"a𝕓🫠🇩🇪👨🏾‍🦱👩‍👩‍👦‍👦".truncate(7.graphemes)   // "a𝕓 … 👨🏾‍🦱👩‍👩‍👦‍👦"

"© А-З Ä-ö-ß".transform("de_DE", "de_DE-ASCII")  // "(C) A-Z AE-oe-ss"
```

#### UTF-16 Char *vs* Code Point *vs* Grapheme Cluster

|                 UTF-16                  | Char<br/>(Java, JavaScript, Kotlin, ...)             | Unicode<br/>Code Point                    | Unicode<br/>Grapheme Cluster |
|:---------------------------------------:|------------------------------------------------------|-------------------------------------------|------------------------------|
|                 \u0061                  | a (LATIN SMALL LETTER A)                             | a                                         | a                            |
|            \uD835<br/>\uDD53            | 𝕓 (MATHEMATICAL DOUBLE-STRUCK SMALL B)              | 𝕓                                        | 𝕓                           |
| \uD83E<br/>\uDEE0<br/>\uD83C<br/>\uDDE9 | ? (HIGH SURROGATES D83E)<br/>? (LOW SURROGATES DEE0) | 🫠 (MELTING FACE EMOJI)                   | 🫠                           |
|            \uD83C<br/>\uDDE9            | ? (HIGH SURROGATES D83C)<br/>? (LOW SURROGATES DDE9) | \[D] (REGIONAL INDICATOR SYMBOL LETTER D) | 🇩🇪                         |
|            \uD83C<br/>\uDDEA            | ? (HIGH SURROGATES D83C)<br/>? (LOW SURROGATES DDEA) | \[E] (REGIONAL INDICATOR SYMBOL LETTER E) |                              |
|            \uD83D<br/>\uDC68            | ? (HIGH SURROGATES D83D)<br/>? (LOW SURROGATES DC68) | 👨 (MAN)                                  | 👨🏾‍🦱                      |
|            \uD83C<br/>\uDFFE            | ? (HIGH SURROGATES D83C)<br/>? (LOW SURROGATES DFFE) | 🏾 (EMOJI MODIFIER FITZPATRICK TYPE-5)    |                              |
|                 \u200D                  | \[ZWJ] (ZERO WIDTH JOINER)                           | \[ZWJ] (ZERO WIDTH JOINER)                |                              |
|            \uD83E<br/>\uDDB1            | ? (HIGH SURROGATES D83E)<br/>? (LOW SURROGATES DDB1) | 🦱 (EMOJI COMPONENT CURLY HAIR)           |                              |
|            \uD83D<br/>\uDC69            | ? (HIGH SURROGATES D83D)<br/>? (LOW SURROGATES DC69) | 👩 (WOMAN)                                | 👩‍👩‍👦‍👦                  |
|                 \u200D                  | \[ZWJ] (ZERO WIDTH JOINER)                           | \[ZWJ] (ZERO WIDTH JOINER)                |                              |
|            \uD83D<br/>\uDC69            | ? (HIGH SURROGATES D83D)<br/>? (LOW SURROGATES DC69) | 👩 (WOMAN)                                |                              |
|                 \u200D                  | \[ZWJ] (ZERO WIDTH JOINER)                           | \[ZWJ] (ZERO WIDTH JOINER)                |                              |
|            \uD83D<br/>\uDC66            | ? (HIGH SURROGATES D83D)<br/>? (LOW SURROGATES DC66) | 👦 (BOY)                                  |                              |
|                 \u200D                  | \[ZWJ] (ZERO WIDTH JOINER)                           | \[ZWJ] (ZERO WIDTH JOINER)                |                              |
|            \uD83D<br/>\uDC66            | ? (HIGH SURROGATES D83D)<br/>? (LOW SURROGATES DC66) | 👦 (BOY)                                  |                              |

### String Operations

- `spaced`/`startSpaced`/`endSpaced`: adds a space before and/or after a string if there isn't already one
- `toIdentifier`: create an identifier from any string that resembles it
- [LineSeparators](src/commonMain/kotlin/com/bkahlert/kommons/text/LineSeparators.kt): many extension functions to work with usual and exotic Unicode line
  breaks.

#### Examples

```kotlin
"string".quoted              // "string"
"""{ bar: "baz" }""".quoted  // "{ bar: \"baz\" }"

"""
line 1
"line 2"
""".quoted                   // "line1\n\"line2\""

"\u001B[1mbold \u001B[34mand blue\u001B[0m".ansiRemoved
// "bold and blue"

"\u001B[34m↗\u001B(B\u001B[m \u001B]8;;https://example.com\u001B\\link\u001B]8;;\u001B\\".ansiRemoved
// "↗ link"

"string".spaced              // " string "

"bar".withPrefix("foo")      // "foobar"
"foo bar".withPrefix("foo")  // "foo bar"
"foo".withSuffix("bar")      // "foobar"

"1👋 xy-z".toIdentifier()     // "i__xy-z3"

randomString()
// returns "Ax-212kss0-xTzy5" (16 characters by default) 
```

Capitalize / decapitalize strings using `capitalize`/`decapitalize` or
manipulate the case style using `toCasesString` or any of its specializations.

#### Examples

```kotlin
"fooBar".capitalize()    // "FooBar"
"FooBar".decapitalize()  // "fooBar"

"FooBar".toCamelCasedString()           // "fooBar"
"FooBar".toPascalCasedString()          // "FooBar"
"FooBar".toScreamingSnakeCasedString()  // "FOO_BAR"
"FooBar".toKebabCasedString()           // "foo-bar"
"FooBar".toTitleCasedString()           // "Foo Bar"

enum class FooBar { FooBaz }

FooBar::class.simpleCamelCasedName           // "fooBar"
FooBar::class.simplePascalCasedName          // "FooBar"
FooBar::class.simpleScreamingSnakeCasedName  // "FOO_BAR"
FooBar::class.simpleKebabCasedName           // "foo-bar"
FooBar::class.simpleTitleCasedName           // "Foo Bar"

FooBar.FooBaz.camelCasedName           // "fooBaz"
FooBar.FooBaz.pascalCasedName          // "FooBaz"
FooBar.FooBaz.screamingSnakeCasedName  // "FOO_BAZ"
FooBar.FooBaz.kebabCasedName           // "foo-baz"
FooBar.FooBaz.titleCasedName           // "Foo Baz
```

Easily check edge-case with a fluent interface as does `requireNotNull` does:

#### Examples

```kotlin
requireNotEmpty("abc")  // passes and returns "abc"
requireNotBlank("   ")  // throws IllegalArgumentException
checkNotEmpty("abc")    // passes and returns "abc"
checkNotBlank("   ")    // throws IllegalStateException
"abc".takeIfNotEmpty()  // returns "abc"
"   ".takeIfNotBlank()  // returns null
"abc".takeUnlessEmpty() // returns "abc"
"   ".takeUnlessBlank() // returns null
```

### Regular Operations

`Regex` can be authored as follows:

```kotlin
Regex("foo") + Regex("bar")      // Regex("foobar") 
Regex("foo") + "bar"             // Regex("foobar")

Regex("foo") or Regex("bar")     // Regex("foo|bar") 
Regex("foo") or "bar"            // Regex("foo|bar")

Regex.fromLiteralAlternates(     // Regex("\\[foo\\]|bar\\?")
    "[foo]", "bar?"
)

Regex("foo").optional()          // Regex("(?:foo)?") 
Regex("foo").repeatAny()         // Regex("(?:foo)*") 
Regex("foo").repeatAtLeastOnce() // Regex("(?:foo)+") 
Regex("foo").repeat(2, 5)        // Regex("(?:foo){2,5}") 

Regex("foo").group()             // Regex("(?:foo)") 
Regex("foo").group("name")       // Regex("(?<name>foo)") 
```

Find matches easier:

```kotlin
// get group by name
Regex("(?<name>ba.)")
    .findAll("foo bar baz")
    .mapNotNull { it.groups["name"]?.value } // "bar", "baz"

// get group value by name
Regex("(?<name>ba.)")
    .findAll("foo bar baz")
    .map { it.groupValue("name") }           // "bar", "baz"

// find all values
Regex("(?<name>ba.)")
    .findAllValues("foo bar baz")            // "bar", "baz"

// match URLs / URIs
Regex.UrlRegex.findAll(/* ... */)
Regex.UriRegex.findAll(/* ... */)
```

Match multiline strings with simple glob patterns:

```kotlin
// matching within lines with wildcard
"foo.bar()".matchesGlob("foo.*")  // ✅

// matching across lines with multiline wildcard
"""
foo
  .bar()
  .baz()
""".matchesGlob(
    """
    foo
      .**()
    """.trimIndent()              // ✅
)

"""
foo
  .bar()
  .baz()
""".matchesGlob(
    """
    foo
      .*()
    """.trimIndent()              // ❌ (* doesn't match across lines)
)
```

Or, you can use `matchesCurly` if you prefer SLF4J / Logback style
wildcards `{}` and `{{}}`.


## Contributing

Want to contribute? Awesome! The most basic way to show your support is to star the project, or to raise issues. You can also support this project by making
a [PayPal donation](https://www.paypal.me/bkahlert) to ensure this journey continues indefinitely!

Thanks again for your support, it is much appreciated! :pray:

## License

MIT. See [LICENSE](LICENSE) for more details.

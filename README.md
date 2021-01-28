![Koodies — Random Kotlin Goodies](./assets/Koodies-header.svg)

# Koodies [![Download from Maven Central](https://img.shields.io/maven-central/v/com.bkahlert.koodies/koodies?color=FFD726&label=Maven%20Central&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4KPCEtLSBHZW5lcmF0b3I6IEFkb2JlIElsbHVzdHJhdG9yIDI1LjEuMCwgU1ZHIEV4cG9ydCBQbHVnLUluIC4gU1ZHIFZlcnNpb246IDYuMDAgQnVpbGQgMCkgIC0tPgo8c3ZnIHZlcnNpb249IjEuMSIgaWQ9IkxheWVyXzEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgeG1sbnM6eGxpbms9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGxpbmsiIHg9IjBweCIgeT0iMHB4IgoJIHZpZXdCb3g9IjAgMCA1MTIgNTEyIiBzdHlsZT0iZW5hYmxlLWJhY2tncm91bmQ6bmV3IDAgMCA1MTIgNTEyOyIgeG1sOnNwYWNlPSJwcmVzZXJ2ZSI%2BCjxnPgoJPGRlZnM%2BCgkJPHBhdGggaWQ9IlNWR0lEXzFfIiBkPSJNMTAxLjcsMzQ1LjJWMTY3TDI1Niw3Ny45TDQxMC40LDE2N3YxNzguMkwyNTYsNDM0LjNMMTAxLjcsMzQ1LjJ6IE0yNTYsNkwzOS42LDEzMS4ydjI0OS45TDI1Niw1MDYKCQkJbDIxNi40LTEyNC45VjEzMS4yTDI1Niw2eiIvPgoJPC9kZWZzPgoJPHVzZSB4bGluazpocmVmPSIjU1ZHSURfMV8iICBzdHlsZT0ib3ZlcmZsb3c6dmlzaWJsZTtmaWxsOiNGRkZGRkY7Ii8%2BCgk8Y2xpcFBhdGggaWQ9IlNWR0lEXzJfIj4KCQk8dXNlIHhsaW5rOmhyZWY9IiNTVkdJRF8xXyIgIHN0eWxlPSJvdmVyZmxvdzp2aXNpYmxlOyIvPgoJPC9jbGlwUGF0aD4KPC9nPgo8L3N2Zz4K)](https://search.maven.org/search?q=g:com.bkahlert.koodies%20AND%20a:koodies) [![Download from Bintray JCenter](https://img.shields.io/bintray/v/bkahlert/koodies/koodies?color=69B745&label=Bintray%20JCenter&logo=JFrog-Bintray&logoColor=fff&style=round)](https://bintray.com/bkahlert/koodies/koodies/_latestVersion) [![Download from GitHub Packages](https://img.shields.io/github/v/release/bkahlert/koodies?color=01818F&label=GitHub&logo=GitHub&logoColor=fff&style=round)](https://github.com/bkahlert/koodies/releases/latest) [![Repository Size](https://img.shields.io/github/repo-size/bkahlert/koodies?color=29ABE2&label=Repo%20Size&logo=Git&logoColor=fff)](https://github.com/bkahlert/koodies) [![Repository Size](https://img.shields.io/github/license/bkahlert/koodies?color=C21E73&label=License&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA1OTAgNTkwIiAgeG1sbnM6dj0iaHR0cHM6Ly92ZWN0YS5pby9uYW5vIj48cGF0aCBkPSJNMzI4LjcgMzk1LjhjNDAuMy0xNSA2MS40LTQzLjggNjEuNC05My40UzM0OC4zIDIwOSAyOTYgMjA4LjljLTU1LjEtLjEtOTYuOCA0My42LTk2LjEgOTMuNXMyNC40IDgzIDYyLjQgOTQuOUwxOTUgNTYzQzEwNC44IDUzOS43IDEzLjIgNDMzLjMgMTMuMiAzMDIuNCAxMy4yIDE0Ny4zIDEzNy44IDIxLjUgMjk0IDIxLjVzMjgyLjggMTI1LjcgMjgyLjggMjgwLjhjMCAxMzMtOTAuOCAyMzcuOS0xODIuOSAyNjEuMWwtNjUuMi0xNjcuNnoiIGZpbGw9IiNmZmYiIHN0cm9rZT0iI2ZmZiIgc3Ryb2tlLXdpZHRoPSIxOS4yMTIiIHN0cm9rZS1saW5lam9pbj0icm91bmQiLz48L3N2Zz4%3D)](https://github.com/bkahlert/koodies/blob/master/LICENSE)

Koodies is a random selection of utility goodies to make a Kotlin developer's life easier*

*See [RELEASING.md](RELEASING.md) for information on how to release versions.*
*`*`(Even easier than it already is compared to Java developers.)*

## Install

### Maven Central

* **Gradle** `implementation("com.bkahlert.koodies:koodies:1.3.0")`

* **Maven**
  ```xml
  <dependency>
      <groupId>com.bkahlert.koodies</groupId>
      <artifactId>koodies</artifactId>
      <version>1.3.0</version>
  </dependency>
  ```

### Bintray JCenter

* **Gradle**
    * add Maven repository
      `repositories { maven("https://dl.bintray.com/bkahlert/koodies") }`
    * add dependency `implementation("com.bkahlert.koodies:koodies:1.3.0")`

## Features

### Builders

```kotlin
enum class Features {
    FeatureA, FeatureB, FeatureC
}

val features = EnumSetBuilder.build<Features> {
    +Features.FeatureA + Features.FeatureC
}
```

```kotlin
fun buildList(init: ListBuilder<String>.() -> Unit) {
    val list = init.build()
}

buildList {
    +"element"
    +existingList
}
```

### Processes

#### Running a Process

```kotlin
val process = process("echo 'Hello World!'") { io ->
    println("Process did output $io") // process the I/O of any process
}.start()

println(process.exitValue) // 0

println(process.logged(OUT)) // Hello World! 
```

#### Running a Shell Script

```kotlin
script {
    shebang()
    line("some command")
    !"""
    a \
    multi-line \
    command
    """
    deleteOnCompletion()
    comment("leave no trace")
}
```

#### Automatically Captured I/O

```kotlin
println(process.ioLog)
```

```shell
Executing /some/where/koodies.process.bka.sh
file:///some/where/koodies.process.bka.sh // <- simply click on it in your IDE
starting to install
installing...
completed.
```

### Docker Runner

```kotlin
Docker.busybox("""
  while true; do
  echo "looping"
  sleep 1
  done
""").execute()
```

... and if something goes wrong, easy to read error message:

```shell
ϟ ProcessExecutionException: Process 67008 terminated with exit code 2. Expected 0. at.(ManagedProcess.kt:126)
  ➜ A dump has been written to:
    - file:///var/folders/hh/739sq9w11lv2hvgh7ymlwwzr20wd76/T/X2rjjlE-tmp/koodies.dump.PLn.log (unchanged)
    - file:///var/folders/hh/739sq9w11lv2hvgh7ymlwwzr20wd76/T/X2rjjlE-tmp/koodies.dump.PLn.no-ansi.log (ANSI escape/control sequences removed)
  ➜ The last 6 lines are:
    🐳 docker attach "download-latest-bkahlert_koodies"
    Executing docker run --name download-latest-bkahlert_koodies --rm -i --mount type=bind,source=/var/folders/hh/739sq9w11lv2hvgh7ymlwwzr20wd76/T,target=/tmp zero88/ghrd --regex bkahlert/koodies
    Searching release 'latest' in repository 'bkahlert/koodies'...
    Not Found artifact '' with regex option 'on'
    Process 67008 terminated with exit code 2. Expected 0.
```

### Improved Java NIO 2 Integration

#### Access the Class Path

```kotlin
classPath("file.svg").copyTo(somewhere)
```

#### Copy and Delete Recursively

```kotlin
directory.copyRecursivelyTo(somewhere)
```

```kotlin
directory.deleteRecursively()
```

#### Fluent API

```kotlin
if (path.notExists()) path.withDirectoriesCreated().createFile()
```

#### Non-Blocking DynamicInputStream & DynamicReadableByteChannel

```kotlin
val inputStream = DynamicInputStream()
// Data can be yielded everytime you want until you call close().
inputStream.yield("Hello World!".toByteArray())
// Stream effectively closes only after all data have been read.
inputStream.close()

val bytes = inputStream.readBytes()

expectThat(bytes).isEqualTo("Hello World!".toByteArray())
```

Same functionality provided by `DynamicReadableByteChannel`.

### Units

#### Decimal and Binary Bytes

```kotlin
10.Yobi.bytes > 10.Yotta.bytes
```

#### Arithmetics

```kotlin
3.Tera.bytes + 200.Giga.bytes == 3.2.Tera.bytes
2 * 3.Kibi.bytes == 6.Kibi.bytes
```

#### File and Directory Size

```kotlin
Path.of("/tmp").size // 1.9 TB
listOf(largeFile, smallFile, mediumFile).sortedBy { it.size }
```

#### From and to String

```kotlin
1.25.Mega.bytes.toString() == "1.25 MB"
"1.25 MB".toSize() == 1.25.Mega.bytes
4_200_000.Yobi.bytes.toString(BinaryPrefix.Mebi, 4) == "4.84e+24 MiB"
```

#### Useless Nerd Stuff

```kotlin
4.2.hecto.bytes == 42.deca.bytes == 420.bytes
```

### Kaomoji

```kotlin
Kaomojis.`(#-_-)o´・━・・━・━━・━☆`.random()
```

### Borders, Boxes, ...

```shell
 ╭───────────────────────────────────────────────────╮ 
 │                                                   │ 
 │        Done. All tests passed within 1.20s        │ 
 │  All test containers running in CONCURRENT mode.  │ 
 │                                                   │ 
 ╰───────────────────────────────────────────────────╯ 
```

```shell
█ ▉ ▊ ▋ ▌ ▍ ▎ ▏ PILLARS  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
```

### More...

* Logging
  ```kotlin
  logging {
    logLine { "lazy log message" }
  }
  ```

* Fixtures

  **In-Memory**
  ```kotlin
  object HtmlFile : Fixture by TextFixture("example.html", 
    """
      <html>
      ...
      </html>
    """.trimIndent())
  
  HtmlFile.copyTo(Locations.Temp)
  ```

  **Embedded in Jar / Class Path**
  ```kotlin
  object META_INF : ClassPathDirectoryFixture("META-INF") {
      object Services : Dir("services") {
          object JUnitExtensions : File("org.junit.jupiter.api.extension.Extension")
      }
  }
  
  println(META_INF.Services.JUnitExtensions.text)
  ```

* Constrained Functions
  ```kotlin
  class A {
    val work by callable(atMost=2) {
      doSomething()
    }
  }
  
  A().apply {
    work() // calls doSomething()
    work() // calls doSomething()
    work() // only returns result of last call
  }
  ```

* Time
  ```kotlin
  Now.emoji // :clock230: (= emoji showing the correct time) 
  ```

  ```kotlin
  if(file.age > 3.minutes) ...
  ```

* Unicode, Code Points & Graphemes

  **Named Characters and Dictionary**
  ```kotlin
  Unicode.BoxDrawings.asTable()
  ```
  ```shell
  ─	BOX DRAWINGS LIGHT HORIZONTAL
  ━	BOX DRAWINGS HEAVY HORIZONTAL
  │	BOX DRAWINGS LIGHT VERTICAL
  ┃	BOX DRAWINGS HEAVY VERTICAL
  ...
  ```  

  **Process Each Actual Character** (and not each `char`)
  ```kotlin
  "aⒷ☷\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC67".asCodePointSequence() -> "a", "Ⓑ", "☷", ":woman:" ZWJ, ":woman:", ZWJ, ":girl:", ZWJ, ":girl:"
  ```

  **Process Each Actual Grapheme** (that is, what users perceive as a character)
  ```kotlin
  "aⒷ☷\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC67‍‍".asGraphemeSequence() -> "a", "Ⓑ", "☷", ":family-woman-woman-girl-girl:"
  ```

* Colors & Formatting
  ```shell
  "string in".cyan() + "or" + "bold".bold()
  ```

* JUnit Extensions, e.g.
  ```kotlin
   @Test
   fun InMemoryLogger.`should work`() {
       logLine { "｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ" }
   }
  ```
  leaves the console clean.  
  Whereas if run with `@Debug` annotated or simply as the single test, the logger prints to the console.

  ```kotlin
   @Debug @Test
   fun InMemoryLogger.`should work`() {
       logLine { "｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ" }
   }
  ```

  ```shell
  ╭─────╴MyTest ➜ should work
  │   ｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ
  ╰─────╴✔
  ```

  Use `debug` to check what's actually inside a `String`:
  ```kotlin
  "a  b\n".debug // a ❲THREE-PER-EM SPACE❳ b ⏎␊
  "�" // D800▌﹍ (low surrogate with a missing high surrogate)
  ```

* Line Separators

  Finally constants for common and uncommon line separators
  ```kotlin
  LineSeparators.toList() == listOf(
    LineSeparators.CRLF, // carriage return + line feed (\r\n)
    LineSeparators.LF,   // line feed (\n)
    LineSeparators.CR,   // carriage return (\r)
    LineSeparators.LS,   // line separator
    LineSeparators.PS,   // paragraph separator 
    LineSeparators.NL,   // next line 
  )
  ```

  Split string into its lines...
  ```kotlin
  """
  line 1
  line 2
  
  """.lines() // line 1, line 2 
  ```

  Split string into its lines lazily and keep the line separator...
    ```kotlin
    """
    line 1
    line 2
  
    """.lineSequence(keepDelimiters=true) // line 1␤, line 2␍␊ 
    ```
  

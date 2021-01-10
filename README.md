![Koodies — Random Kotlin Goodies](./assets/Koodies-header.svg)

*See [RELEASING.md](RELEASING.md) for information on how to release versions.*

## Install

### Add Maven Repository

Add `https://dl.bintray.com/bkahlert/koodies` as a Maven repository, e.g.

```kotlin
repositories {
    ...
    maven("https://dl.bintray.com/bkahlert/koodies")
    ...
}
```

### Add Dependency

Add `com.bkahlert.koodies:koodies:VERSION.GOES.HERE` as a dependency, e.g.

```kotlin
dependencies {
    ...
    implementation("com.bkahlert.koodies:koodies:1.1.0")
    ...
}
```

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
  ```kotlin
  HtmlFile.copyTo(Locations.Temp)
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

  Split strings into its lines...
  ```kotlin
  """
  line 1
  line 2
  """.lines() // line 1, line 2 
  ```

  Split strings into its lines lazily...
  ```kotlin
  """
  line 1
  line 2
  """.lineSequence() // line 1, line 2 
  ```

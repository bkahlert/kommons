![Koodies](assets/Koodies-logo.svg)

# Koodies [![Download](https://api.bintray.com/packages/bkahlert/koodies/koodies/images/download.svg)](https://bintray.com/bkahlert/koodies/koodies/_latestVersion)

**Random Kotlin Goodies**

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

#### Copy Recursively

```kotlin
directory.copyRecursivelyTo(somewhere)
```

#### Fluent API

```kotlin
if (path.notExists()) path.withDirectoriesCreated().createFile()
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

* Logger
  ```kotlin
  logging {
    logLine { "lazy log message" }
  }
  ```

* Fixture
  ```kotlin
  HtmlFile.copyTo(Locations.Temp)
  ```

* Time
  ```kotlin
  Now.emoji
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
  "�" // D800▌﹍ (low surrogate with a missing high surroage)
  ```

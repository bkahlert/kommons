# Releasing

New versions are released using the [Nebula Release Plugin](https://github.com/nebula-plugins/nebula-release-plugin) which
uses [grgit](https://github.com/ajoberstar/grgit) to interact with the Git repository.

**All examples assume version 2.1.0 was successfully released with you working on future version 2.2.0.**

## Version Types

|           	| Example                                            | Format                                                                    | Gradle Task            |
|-----------	|------------------------------------------------	|-----------------------------------------------------------------------	|----------------------	|
| Final        | 2.2.0                                            | &lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;                                                | final                    |
| Candidate    | 2.2.0-rc.1                                        | &lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;-rc.#                                            | candidate                |
| Snapshot    | 2.2.0-dev.2.uncommitted+d953d49<br>2.2.0-SNAPSHOT    | &lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;-dev.#+&lt;hash&gt;<br>&lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;-SNAPSHOT    | devSnapshot<br>snapshot    |

## Development Life Cycle

1. Implement, fix, do whatever change you feel a need for.
2. Test
2. Commit

## Cheat Sheet

### Test Uncommited Changes on a Dependant Project

1. Change dependency in dependent project to `implementation("com.bkahlert.koodies:koodies-jvm:2.2.0-SNAPSHOT")`.
2. Publish changes to local Maven repository using `./gradlew snapshot publishToMavenLocal -Prelease.scope=minor`.

### Release a Candidate

1. Commit your changes using `git commit`.
2. Release candidate using `./gradlew candidate -Prelease.scope=minor`.
3. Publish

### Increase Version Number

```shell
./gradlew <snapshot|devSnapshot|candidate|final> -Prelease.scope=<major|minor|patch>
```

Example—Increase major version: `./gradlew <snapshot|devSnapshot|candidate|final> -Prelease.scope=patch`

### Last Tag

Create a tag locally using `git tag v2.2.0` (leading `v`  is important) and `./gradlew -Prelease.useLastTag=true final` in a CI to make the latter use a
pre-defined version.

### Override Version

```shell
./gradlew -Prelease.version=1.2.3 final
```

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

## Processes

### Running a Process

```kotlin
val process = process("echo 'Hello World!'") { io ->
    println("Process did output $io") // process the I/O of any process
}.start()

println(process.exitValue) // 0

println(process.logged(OUT)) // Hello World! 
```

### Running a Shell Script

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

### Automatically Captured I/O

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

## Docker Runner

```kotlin
Docker.busybox("""
  while true; do
  echo "looping"
  sleep 1
  done
""").execute()
```

## Improved Java NIO 2 Integration

### Access the Class Path

```kotlin
classPath("file.svg").copyTo(somewhere)
```

### Copy Recursively

```kotlin
directory.copyRecursivelyTo(somewhere)
```

### Fluent API

```kotlin
if (path.notExists()) path.withDirectoriesCreated().createFile()
```

## Kaomoji

```kotlin
Kaomojis.`(#-_-)o´・━・・━・━━・━☆`.random()
```

## Borders, Boxes, ...

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

## More

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

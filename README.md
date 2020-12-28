![Koodies](assets/Koodies-logo.svg)

# Koodies

**Random Kotlin Goodies**

## Builders

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

```kotlin
val process = process("echo 'Hello World!'") { io ->
  println("Process did output $io")
}.start()

println(process.exitValue)

println(process.logged(ERR))
```

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
}.start()
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

## Java NIO

```kotlin
classPath("file.svg").copyTo(somewhere)
```

## Kaomoji

```kotlin
Kaomojis.`(#-_-)o´・━・・━・━━・━☆`.random()
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

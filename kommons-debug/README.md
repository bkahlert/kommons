# Kommons Debug

## About

**Kommons Debug** is a Kotlin Multiplatform Library for print debugging. It adds:

1. powerful print debugging functions:
    - [trace](#anytrace--anyinspect)
    - [inspect](#anytrace--anyinspect)
    - [render](#anyrender--anyasstring)
    - [renderType](#anyrendertype)
2. a platform-independent [stack trace](#stack-trace)

## Installation / setup

This library is hosted on GitHub with releases provided on Maven Central.

* **Gradle** `implementation("com.bkahlert.kommons:kommons-debug:2.6.0") { because("print debugging") }`

* **Maven**
  ```xml
  <dependency>
      <groupId>com.bkahlert.kommons</groupId>
      <artifactId>kommons-debug</artifactId>
      <version>2.6.0</version>
  </dependency>
  ```

## Features

### Any?.trace / Any?.inspect

Print tracing information and easily cleanup afterward using
IntelliJ's code cleanup feature.

#### Example

```kotlin
data class Foo(val bar: String = "baz") {
    private val baz = 42.0
    fun compute() // used to demonstrate that trace/inspect return their argument unchanged
}

Foo().trace.compute()
// output: (sample.kt:5) ⟨ Foo(bar=baz) ⟩

Foo().trace("caption").compute()
// output: (sample.kt:8) caption ⟨ Foo(bar=baz) ⟩

Foo().trace("details") { it.bar.reversed() }.compute()
// output: (sample.kt:11) details ⟨ Foo(bar=baz) ⟩ { "zab" }

Foo().inspect.compute()
// output: (sample.kt:14) ⟨ !Foo { baz: !Double 42.0, bar: !String "baz" } ⟩

Foo().inspect("caption").compute()
// output: (sample.kt:17) caption ⟨ !Foo { baz: !Double 42.0, bar: !String "baz" } ⟩

Foo().inspect("details") { it.bar.reversed() }.compute()
// output: (sample.kt:20) details ⟨ !Foo { baz: !Double 42.0, bar: !String "baz" } ⟩ { !String "zab" }
```

![docs/trace-cleanup.png](docs/trace-cleanup.png)

The examples above also work in browsers:

![docs/trace-browser-console.png](docs/trace-browser-console.png)

![docs/trace-browser-sources.png](docs/trace-browser-sources.png)

### Any.renderType()

Renders any object's type

#### Examples

```kotlin
"string".renderType()               // String

class Foo(val bar: Any = "baz")
foo().renderType()                  // Foo

val lambda: (String) -> Unit = {}
lambda.renderType()                 // (String)->Unit
```

### Any?.render() / Any.asString()

Renders any object depending on whether its `toString()` is overridden:

- If there is a custom `toString()` it's simply used.
- if there is *no custom* `toString()` the object is serialized in the form structurally

#### Examples

```kotlin
"string".render()                              // string

class Foo(val bar: Any = "baz")

foo().render()                                 // { bar: "baz" }
foo(foo()).render(typed = true)                // Foo { bar: Foo { bar: "baz" } }

foo().asString()                               // { bar: "baz" }
foo(null).asString(excludeNullValues = false)  // { }
```

### Any.properties

Contains a map of the object's properties with each entry representing
the name and value of a property.

#### Examples

```kotlin
"string".properties               // { length: 6 }

class Foo(val bar: Any = "baz")
foo().properties                  // { bar: "baz" }
foo(foo()).properties             // { bar: { bar: "baz" } }
```

### URL / URI / Path / File // open / locate \[only JVM\]

Any `URL`, `URI`, `Path` and `File` can be opened locally using `open`.

```kotlin
URL("file:///home/john/dev/project/src/jvm/kotlin/packages/source.kt").open()
```

To only open the directory containing an above-mentioned resource
`locate` can be used.

```kotlin
URL("file:///home/john/dev/project/src/jvm/kotlin/packages/source.kt").locate()
```

### Stack Trace

Access the current stack trace by a simple call to `StackTrace.get()`
or locate a specific caller using `StackTrace.get().findLastKnownCallOrNull`.

#### Examples

```kotlin
fun foo(block: () -> StackTraceElement?) = block()
fun bar(block: () -> StackTraceElement?) = block()

foo { bar { StackTrace.findLastKnownCallOrNull("bar") } }?.function  // "foo"
foo { bar { StackTrace.findLastKnownCallOrNull(::bar) } }?.function  // "foo"
```

## Contributing

Want to contribute?
Awesome!
The most basic way to show your support is to star the project or to raise issues.
You can also support this project by making a [PayPal donation](https://www.paypal.me/bkahlert) to ensure this journey continues indefinitely!

Thanks again for your support, it's much appreciated! :pray:

## License

MIT. See [LICENSE](../LICENSE) for more details.

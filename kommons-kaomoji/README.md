# Kommons Kaomoji

## About

**Kommons Kaomoji** is a Kotlin Multiplatform Library that offers
[Japanese style emoticon](https://en.wikipedia.org/wiki/Emoticon#Japanese_style) constants such as `(つ◕౪◕)つ━☆ﾟ.*･｡ﾟ`

## Installation / setup

This library is hosted on GitHub with releases provided on Maven Central.

* **Gradle** `implementation("com.bkahlert.kommons:kommons-kaomoji:2.1.0")

* **Maven**
  ```xml
  <dependency>
      <groupId>com.bkahlert.kommons</groupId>
      <artifactId>kommons-kaomoji</artifactId>
      <version>2.1.0</version>
  </dependency>
  ```

## Features

```kotlin
// (つ◕౪◕)つ━☆ﾟ.*･｡ﾟ
println(
    Kaomoji.Wizards.`（つ◕౪◕）つ━☆ﾟ․＊･｡ﾟ`
)

// ᕕ ˘ ▽` )o/￣￣￣❮°«⠶＞˝
println(
    Kaomoji.random().fishing(Kaomoji.Fish.`❮°«⠶＞˝`)
)

//                              ⎛ ▄▄▄▄              ▄▄▄  ▄   ▄  ▄ ▄▄    ⎞
//                              ⎜ ▐█ ▀█             ▀▄ █ ▄▄  █▌▐█▐█ ▀   ⎟
//                              ⎜ ▐█▀▀█▄ ▄█▀▄  ▄█▀▄ ▐▀▀▄ ▐█ ▐█▐▐▌▄█ ▀█▄ ⎟
//                              ⎜ ██▄ ▐█▐█▌ ▐▌▐█▌ ▐▌▐█•█▌▐█▌██▐█▌▐█▄ ▐█ ⎟
//                         ̣ ˱ ❨ ⎝  ▀▀▀▀  ▀█▄▀  ▀█▄▀  ▀  ▀▀▀▀▀▀ █  ▀▀▀▀  ⎠
// 【 TV 】      -o(.￣ )
println(
    Kaomoji.TV.random().thinking(
        """
        ▄▄▄▄              ▄▄▄  ▄   ▄  ▄ ▄▄ 
        ▐█ ▀█             ▀▄ █ ▄▄  █▌▐█▐█ ▀
        ▐█▀▀█▄ ▄█▀▄  ▄█▀▄ ▐▀▀▄ ▐█ ▐█▐▐▌▄█ ▀█▄
        ██▄ ▐█▐█▌ ▐▌▐█▌ ▐▌▐█•█▌▐█▌██▐█▌▐█▄ ▐█
         ▀▀▀▀  ▀█▄▀  ▀█▄▀  ▀  ▀▀▀▀▀▀ █  ▀▀▀▀ 
        """.trimIndent()
    )
)
```

## Contributing

Want to contribute?
Awesome!
The most basic way to show your support is to star the project or to raise issues.
You can also support this project by making a [PayPal donation](https://www.paypal.me/bkahlert) to ensure this journey continues indefinitely!

Thanks again for your support, it's much appreciated! :pray:

## License

MIT. See [LICENSE](../LICENSE) for more details.

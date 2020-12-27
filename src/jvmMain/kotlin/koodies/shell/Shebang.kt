package koodies.shell

import koodies.io.path.asString
import java.nio.file.Path

class Shebang(private val sh: ShellScript) {
    private var interpreter: Path = Path.of("/bin/sh")
    private fun reset() = with(sh.lines) {
        firstOrNull()?.also { if (it.startsWith("#!")) remove(it) }
        add(0, "#!${interpreter.asString()}")
        this@Shebang
    }

    init {
        reset()
    }

    operator fun invoke(interpreter: String = "/bin/sh"): Shebang = also { invoke(Path.of(interpreter)) }.reset()
    operator fun invoke(interpreter: Path = Path.of("/bin/sh")): Shebang = also { interpreter.let { this.interpreter = it } }.reset()
    operator fun not(): Unit = run { reset() }

}

val ShellScript.`#!`: Shebang get() = Shebang(this)

val ShellScript.shebang: Shebang get() = Shebang(this)

fun String.isShebang() = startsWith("#!")

package koodies.shell

import koodies.io.path.asString
import java.nio.file.Path

class Shebang(private val contents: MutableList<String>) {

    private var interpreter: String = "/bin/sh"
    
    private fun updateContents() = with(contents) {
        firstOrNull()?.also { if (it.startsWith("#!")) remove(it) }
        add(0, "#!$interpreter")
        this@Shebang
    }

    init {
        updateContents()
    }

    operator fun invoke(interpreter: String = "/bin/sh", vararg args: String): Shebang = also { it.interpreter = interpreter }.updateContents()
    operator fun invoke(interpreter: Path, vararg args: String): Shebang = invoke(interpreter.asString(), *args)
    operator fun not(): Unit = run { updateContents() }
}

fun String.isShebang() = startsWith("#!")

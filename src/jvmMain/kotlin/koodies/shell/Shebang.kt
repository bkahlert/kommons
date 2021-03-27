package koodies.shell

import koodies.io.path.asString
import java.nio.file.Path

public class Shebang(private val contents: MutableList<String>) {

    private var interpreter: String = "/bin/sh"

    private fun updateContents() = with(contents) {
        firstOrNull()?.also { if (it.startsWith("#!")) remove(it) }
        add(0, "#!$interpreter")
        this@Shebang
    }

    init {
        updateContents()
    }

    public operator fun invoke(interpreter: String = "/bin/sh"): Shebang = also { it.interpreter = interpreter }.updateContents()
    public operator fun invoke(interpreter: Path): Shebang = invoke(interpreter.asString())
    public operator fun not(): Unit = run { updateContents() }
}

public fun String.isShebang(): Boolean = startsWith("#!")

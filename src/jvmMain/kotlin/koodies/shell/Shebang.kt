package koodies.shell

import koodies.io.path.pathString
import java.nio.file.Path

public class Shebang(private val contents: MutableList<String>) {

    private var interpreter: String = "/bin/sh"
    private val arguments: MutableList<String> = mutableListOf()

    private fun updateContents() = with(contents) {
        firstOrNull()?.also { if (it.startsWith("#!")) remove(it) }
        add(0, "#!$interpreter" + arguments.joinToString("") { " $it" })
        this@Shebang
    }

    init {
        updateContents()
    }

    public operator fun invoke(interpreter: String = "/bin/sh", vararg arguments: String): Shebang = also {
        it.interpreter = interpreter
        with(this.arguments) {
            clear()
            addAll(arguments)
        }
    }.updateContents()

    public operator fun invoke(interpreter: Path, vararg arguments: String): Shebang = invoke(interpreter.pathString, *arguments)
    public operator fun not(): Unit = run { updateContents() }
}

public fun String.isShebang(): Boolean = startsWith("#!")

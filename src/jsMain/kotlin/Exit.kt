package koodies.runtime


public actual object Program {
    public actual val isDebugging: Boolean
        get() = TODO("Not yet implemented")

    public actual fun <T : OnExitHandler> onExit(handler: T): T {
        TODO("Not yet implemented")
    }
}

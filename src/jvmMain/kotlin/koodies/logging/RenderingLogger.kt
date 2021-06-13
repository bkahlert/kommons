package koodies.logging

public interface RenderingLogger {

    /**
     * The name of this
     */
    public val name: String
    public val parent: SimpleRenderingLogger?

    public val started: Boolean

    public fun onStart(): Unit = Unit

    /**
     * Contains whether this logger is open, that is,
     * at least one logging call was received but no result, yet.
     */
    public val open: Boolean // TODO rename to started

    /**
     * Contains whether this logger is closed, that is,
     * the logging span was finished with a logged result.
     */
    public val closed: Boolean // TODO rename to ended

    public fun log(lazyMessage: () -> String)
    public fun <T> close(result: Result<T>)
}

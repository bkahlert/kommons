package koodies.test

/**
 * Defined fixed test resource.
 */
public interface Fixture<T> {
    /**
     * Name of this fixture.
     */
    public val name: String

    /**
     * Content of this fixture.
     */
    public val contents: T
}

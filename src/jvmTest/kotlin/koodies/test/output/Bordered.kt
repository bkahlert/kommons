package koodies.test.output

/**
 * Annotated instances of [InMemoryLogger] are rendered bordered
 * depending on [value].
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Bordered(val value: Boolean)

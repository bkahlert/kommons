package koodies.test.output

/**
 * Annotated instances of [InMemoryLogger] use the number of columns
 * specified by [value] instead of the default number.
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Columns(val value: Int)

package com.bkahlert.kommons.test

import io.kotest.assertions.print.print
import io.kotest.matchers.ComparableMatcherResult
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import io.kotest.mpp.bestName

/**
 * The root cause of this [Throwable], that is,
 * the throwable that was thrown the first.
 */
public val Throwable.rootCause: Throwable
    get() {
        var rootCause: Throwable = this
        while (rootCause.cause != null && rootCause.cause !== rootCause) {
            rootCause = rootCause.cause ?: error("Must not happen.")
        }
        return rootCause
    }

public infix fun Throwable.shouldHaveRootCauseMessage(rootCauseMessage: String): Unit = this should haveRootCauseMessage(rootCauseMessage)
public infix fun Throwable.shouldNotHaveRootCauseMessage(rootCauseMessage: String): Unit = this shouldNot haveRootCauseMessage(rootCauseMessage)

public fun haveRootCauseMessage(rootCauseMessage: String): Matcher<Throwable> = object : Matcher<Throwable> {
    override fun test(value: Throwable) = ComparableMatcherResult(
        value.rootCause.message?.trim() == rootCauseMessage.trim(),
        {
            "Throwable should have root cause message:\n${rootCauseMessage.trim().print().value}\n\nActual was:\n${
                value.rootCause.message?.trim().print().value
            }\n"
        },
        {
            "Throwable should not have rootCauseMessage:\n${rootCauseMessage.trim().print().value}"
        },
        actual = value.rootCause.message?.trim().print().value,
        expected = rootCauseMessage.trim().print().value,
    )
}

public infix fun Throwable.shouldHaveRootCauseMessage(rootCauseMessage: Regex): Unit = this should haveRootCauseMessage(rootCauseMessage)
public infix fun Throwable.shouldNotHaveRootCauseMessage(rootCauseMessage: Regex): Unit = this shouldNot haveRootCauseMessage(rootCauseMessage)

public fun haveRootCauseMessage(regex: Regex): Matcher<Throwable> = object : Matcher<Throwable> {
    override fun test(value: Throwable) = MatcherResult(
        value.rootCause.message?.matches(regex) ?: false,
        { "Throwable should match regex: ${regex.print().value}\nActual was:\n${value.rootCause.message?.trim().print().value}\n" },
        { "Throwable should not match regex: ${regex.print().value}" })
}


public fun Throwable.shouldHaveRootCause(block: (Throwable) -> Unit = {}) {
    this should haveRootCause()
    block.invoke(rootCause)
}

public fun Throwable.shouldNotHaveRootCause(): Unit = this shouldNot haveRootCause()
public fun haveRootCause(): Matcher<Throwable> = object : Matcher<Throwable> {
    override fun test(value: Throwable) = resultForThrowable(value.rootCause)
}

public inline fun <reified T : Throwable> Throwable.shouldHaveRootCauseInstanceOf(): Unit = this should haveRootCauseInstanceOf<T>()
public inline fun <reified T : Throwable> Throwable.shouldNotHaveRootCauseInstanceOf(): Unit = this shouldNot haveRootCauseInstanceOf<T>()
public inline fun <reified T : Throwable> haveRootCauseInstanceOf(): Matcher<Throwable> = object : Matcher<Throwable> {
    override fun test(value: Throwable): MatcherResult {
        val rootCause = value.rootCause
        return MatcherResult(
            rootCause is T,
            { "Throwable root cause should be of type ${T::class.bestName()} or it's descendant, but instead got ${rootCause::class.bestName()}" },
            { "Throwable root cause should not be of type ${T::class.bestName()} or it's descendant" })
    }
}

public inline fun <reified T : Throwable> Throwable.shouldHaveRootCauseOfType(): Unit = this should haveRootCauseOfType<T>()
public inline fun <reified T : Throwable> Throwable.shouldNotHaveRootCauseOfType(): Unit = this shouldNot haveRootCauseOfType<T>()
public inline fun <reified T : Throwable> haveRootCauseOfType(): Matcher<Throwable> = object : Matcher<Throwable> {
    override fun test(value: Throwable): MatcherResult {
        val rootCause = value.rootCause
        return MatcherResult(
            rootCause::class == T::class,
            { "Throwable root cause should be of type ${T::class.bestName()}, but instead got ${rootCause::class.bestName()}" },
            { "Throwable root cause should not be of type ${T::class.bestName()}" })
    }
}

@PublishedApi
internal fun resultForThrowable(value: Throwable?): MatcherResult = MatcherResult(
    value != null,
    { "Throwable should have a root cause" },
    { "Throwable should not have a root cause" })

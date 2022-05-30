@file:Suppress("DEPRECATION")

package com.bkahlert.kommons.debug

import com.bkahlert.kommons.asString
import com.bkahlert.kommons.math.BigDecimal
import com.bkahlert.kommons.math.precision
import com.bkahlert.kommons.math.scale
import com.bkahlert.kommons.math.toAtMostDecimalsString
import com.bkahlert.kommons.math.toExactDecimalsString
import com.bkahlert.kommons.math.toScientificString
import com.bkahlert.kommons.regex.groupValue
import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.joinLinesToString
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock

public val Thread.highlightedName: String
    get() = toString().replace(Regex("(?<prefix>.*?\\[)(?<details>.*)(?<suffix>].*?)")) {
        val prefix = it.groupValue("prefix")
        val suffix = it.groupValue("suffix")
        val details = it.groupValue("details")?.split(",")?.joinToString(", ") { detail -> detail.formattedAs.debug }
        prefix + details + suffix
    }

public val StackTraceElement.highlightedMethod: String
    get() = toString().replace(Regex("(?<prefix>.*\\.)(?<method>.*?)(?<suffix>\\(.*)")) {
        val prefix = it.groupValue("prefix")
        val suffix = it.groupValue("suffix")
        val methodName = it.groupValue("method")?.formattedAs?.debug
        prefix + methodName + suffix
    }

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by printing this thread and highlighting its details.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public val <T : Thread> T.trace: Thread
    get() = trace()

/**
 * Helper function that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * passing this thread and this thread applied to the given [transform] to [println]
 * while still returning this.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public fun <T : Thread> T.trace(description: CharSequence? = null, transform: (T.() -> Any)? = null): T =
    also { println(xray(description, { highlightedName }, transform)) }

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by printing this stacktrace and highlighting the method names.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public val Array<StackTraceElement>.trace: Array<StackTraceElement>
    get() = trace()

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by printing this stacktrace and highlighting the method names.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public fun Array<StackTraceElement>.trace(
    description: CharSequence? = null,
    transform: (Array<StackTraceElement>.() -> Any)? = null,
): Array<StackTraceElement> = also {
    println(
        xray(
            description,
            { joinToString("${LineSeparators.LF}\t${"at".formattedAs.debug} ", postfix = LineSeparators.LF) { it.highlightedMethod } },
            transform
        )
    )
}

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by printing this lock properly with details.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public val ReentrantLock.trace: ReentrantLock
    get() = also {
        println(asString {
            put("isFair", isFair)
            put("isLocked", isLocked)
            put("isHeldByCurrentThread", isHeldByCurrentThread)
        })
    }

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by printing this lock properly with details.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public val ReentrantReadWriteLock.trace: ReentrantReadWriteLock
    get() = also {
        println(asString {
            put("isFair", isFair)
            put("readLockCount", readLockCount)
            put("readHoldCount", readHoldCount)
            put("writeHoldCount", writeHoldCount)
            put("isWriteLocked", isWriteLocked)
            put("isWriteLockedByCurrentThread", isWriteLockedByCurrentThread)
        })
    }

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by printing this match result properly with details.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public val <T : MatchResult?> T.trace: T
    get() = this?.apply {
        val matchedGroups = groups.filterNotNull()
        println("Regular Expression Matched ${matchedGroups.size.toString().formattedAs.debug} group(s)")
        println(matchedGroups.joinLinesToString { "${it.range}: ".padStart(8) + it.value.formattedAs.debug })
    } ?: apply { println("Regular Expression Did Not Match".formattedAs.warning) }


/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * by printing this big decimal with details.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public val BigDecimal.trace: BigDecimal
    get() = also {
        println(asString {
            put("double", it.toDouble())
            put("string value", it.toString())
            put("at most 3 decimals", it.toAtMostDecimalsString(3))
            put("exact 3 decimals", it.toExactDecimalsString(3))
            put("scientific", it.toScientificString())
            put("engineering", it.toEngineeringString())
            put("plain", it.toPlainString())
            put("scale", it.scale)
            put("precision", it.precision)
        })
    }

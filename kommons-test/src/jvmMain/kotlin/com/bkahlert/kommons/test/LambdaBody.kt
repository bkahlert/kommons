package com.bkahlert.kommons.test

import com.bkahlert.kommons.text.LineSeparators.isMultiline
import com.bkahlert.kommons.text.indexOfOrNull

/** The body of a lambda function. */
@JvmInline
public value class LambdaBody internal constructor(
    /** The body of this lambda function. */
    public val body: String,
) : CharSequence by body {
    override fun toString(): String = body

    /** The outer body of this lambda function, that is, the [body] surrounded by [Brackets]. */
    public val outerBody: String
        get() = Brackets.let { (left, right) ->
            if (body.isMultiline()) {
                buildString {
                    appendLine(left)
                    appendLine(body.prependIndent("    "))
                    append(right)
                }
            } else {
                "$left $body $right"
            }
        }

    /** The outer body of this lambda function, that is, the specified [name] followed by the [body] surrounded by [Brackets]. */
    public fun outerBody(name: String): String = "$name $outerBody"

    public companion object {
        /** The brackets surrounding a lambda function. */
        public val Brackets: Pair<Char, Char> = '{' to '}'

        /** Returns the name of the lambda contained in the specified [code] if any, or `null` otherwise. */
        public fun guessName(code: CharSequence): String? = code
            .indexOfOrNull(Brackets.first)
            ?.let { code.subSequence(0, it) }
            ?.reversed()
            ?.trimStart()
            ?.let { subSequenceWithoutLeadingBrackets(it, ')' to '(').trimStart() }
            ?.let { subSequenceWithoutLeadingBrackets(it, '>' to '<').trimStart() }
            ?.takeWhile(Char::isJavaIdentifierPart)
            ?.reversed()
            ?.takeIf { it.firstOrNull()?.isJavaIdentifierStart() == true }
            ?.toString()

        /** Attempts to parse the body of a lambda with the specified [name] in the specified [code]. */
        public fun parseOrNull(name: String, code: String): LambdaBody? {
            val firstPossibleBracket = code.indexOfOrNull(name) ?: return null
            val firstBracket = code.indexOfOrNull(Brackets.first, firstPossibleBracket)?.let { it + 1 } ?: return null
            val subjectEnd = findMatchingClosingBracket(code, firstBracket) ?: return null
            val body = code.substring(firstBracket, subjectEnd)
            val bodyWithoutStartAndEndLines = body.lines().dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }.joinToString("\n")
            return bodyWithoutStartAndEndLines.run {
                LambdaBody(if (isMultiline()) trimIndent() else trim())
            }
        }

        /**
         * Attempts to parse the body of a lambda with
         * the name based on the optionally specifiable [methodNameHints] (default: [StackTraceElement.methodName] and guessed name),
         * and the code retrieved from the actual source code file.
         */
        public fun parseOrNull(
            stackTraceElement: StackTraceElement,
            vararg methodNameHints: String,
        ): LambdaBody? = FilePeekMPP.getCallerFileInfo(stackTraceElement)
            ?.zoomOutSequence()
            ?.firstNotNullOfOrNull { fileInfo ->
                val code = fileInfo.code
                val methodNames = methodNameHints.takeUnless { it.isEmpty() }?.toList() ?: listOfNotNull(stackTraceElement.methodName, guessName(code))
                val methodName = methodNames.firstOrNull { hint ->
                    code.indexOfOrNull(hint)?.let { guessName(code.subSequence(it, code.length)) == hint } == true
                }
                methodName?.let { parseOrNull(it, code) }
            }

        private fun findMatchingClosingBracket(code: CharSequence, offset: Int, brackets: Pair<Char, Char> = Brackets): Int? {
            val codeLength = code.length
            var nesting = 0
            var pos = offset
            while (pos < codeLength) {
                when (code[pos]) {
                    brackets.first -> nesting++
                    brackets.second -> when (nesting) {
                        0 -> return pos
                        else -> nesting--
                    }
                }
                pos++
            }
            return null
        }

        private fun subSequenceWithoutLeadingBrackets(code: CharSequence, brackets: Pair<Char, Char>): CharSequence =
            code.takeIf {
                it.startsWith(brackets.first)
            }?.let {
                findMatchingClosingBracket(it, 1, brackets)
            }?.let {
                code.subSequence(it + 1, code.length)
            } ?: code
    }
}


/**
 * Attempts to parse the body of a lambda with
 * the name based on the optionally specifiable [methodNameHints] (default: [StackTraceElement.methodName] and guessed name),
 * and the code retrieved from the actual source code file.
 */
public fun StackTraceElement.getLambdaBodyOrNull(
    vararg methodNameHints: String,
): LambdaBody? = LambdaBody.parseOrNull(this, *methodNameHints)

/**
 * Attempts to parse the body of a lambda with
 * the name based on the optionally specifiable [methodNameHints] (default: [StackTraceElement.methodName] and guessed name),
 * and the code retrieved from the actual source code file.
 */
public fun Throwable.getLambdaBodyOrNull(
    vararg methodNameHints: String,
): LambdaBody? = stackTrace.first().getLambdaBodyOrNull(*methodNameHints)

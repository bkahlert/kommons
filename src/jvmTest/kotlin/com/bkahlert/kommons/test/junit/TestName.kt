package com.bkahlert.kommons.test.junit

import org.junit.jupiter.api.extension.ExtensionContext

class TestName private constructor(
    val value: String,
) : CharSequence by value {

    override fun toString(): String = value

    companion object {

        /**
         * Name of the current test.
         */
        val ExtensionContext.testName: String
            get() :String {
                val separator = " ➜ "
                val name = element.map { parent.map { it.testName }.orElse("") + separator + displayName }.orElse("")
                return if (name.startsWith(separator)) name.substring(separator.length) else name
            }

        fun from(context: ExtensionContext) = TestName(context.testName)
    }
}

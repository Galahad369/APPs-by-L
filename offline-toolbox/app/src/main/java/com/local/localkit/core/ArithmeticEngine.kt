package com.local.localkit.core

import kotlin.math.*

/** Small deterministic parser. It never evaluates code or touches the network. */
object ArithmeticEngine {
    fun evaluate(expression: String): Double = Parser(expression.replace('×', '*').replace('÷', '/')).parse()

    private class Parser(private val source: String) {
        private var index = 0

        fun parse(): Double {
            val value = expression()
            skipSpaces()
            require(index == source.length) { "Unexpected '${source[index]}'" }
            require(value.isFinite()) { "Result is not finite" }
            return value
        }

        private fun expression(): Double {
            var value = term()
            while (true) {
                skipSpaces()
                value = when {
                    take('+') -> value + term()
                    take('-') -> value - term()
                    else -> return value
                }
            }
        }

        private fun term(): Double {
            var value = power()
            while (true) {
                skipSpaces()
                value = when {
                    take('*') -> value * power()
                    take('/') -> value / power()
                    take('%') -> value % power()
                    else -> return value
                }
            }
        }

        private fun power(): Double {
            val value = unary()
            skipSpaces()
            return if (take('^')) value.pow(power()) else value
        }

        private fun unary(): Double {
            skipSpaces()
            if (take('+')) return unary()
            if (take('-')) return -unary()
            return primary()
        }

        private fun primary(): Double {
            skipSpaces()
            if (take('(')) {
                val value = expression()
                require(take(')')) { "Missing )" }
                return value
            }
            if (index < source.length && source[index].isLetter()) {
                val name = buildString { while (index < source.length && source[index].isLetter()) append(source[index++]) }
                return when (name.lowercase()) {
                    "pi" -> Math.PI
                    "e" -> Math.E
                    else -> {
                        require(take('(')) { "Expected ( after $name" }
                        val value = expression()
                        require(take(')')) { "Missing )" }
                        when (name.lowercase()) {
                            "sqrt" -> sqrt(value)
                            "sin" -> sin(Math.toRadians(value))
                            "cos" -> cos(Math.toRadians(value))
                            "tan" -> tan(Math.toRadians(value))
                            "ln" -> ln(value)
                            "log" -> log10(value)
                            "abs" -> abs(value)
                            else -> error("Unknown function $name")
                        }
                    }
                }
            }
            val start = index
            while (index < source.length && (source[index].isDigit() || source[index] == '.')) index++
            require(index > start) { "Expected a number" }
            return source.substring(start, index).toDouble()
        }

        private fun skipSpaces() { while (index < source.length && source[index].isWhitespace()) index++ }
        private fun take(char: Char): Boolean = if (index < source.length && source[index] == char) { index++; true } else false
    }
}


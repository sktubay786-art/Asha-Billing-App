package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode

object SafeCalculator {
    private val tokenRegex = Regex("""\d*\.?\d+|[()+\-*/%]""")

    fun evaluate(expression: String): Double {
        val normalized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(Regex("""\s+"""), "")
        if (normalized.isEmpty()) throw IllegalArgumentException("Empty expression")

        val tokens = tokenRegex.findAll(normalized).map { it.value }.toList()
        if (tokens.isEmpty() || tokens.joinToString("") != normalized) {
            throw IllegalArgumentException("Invalid expression")
        }

        val output = mutableListOf<String>()
        val ops = ArrayDeque<String>()
        val precedence = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2, "%" to 3, "u-" to 4)
        var prev = "start"

        for (rawToken in tokens) {
            var token = rawToken
            when {
                token.first().isDigit() || token.first() == '.' -> {
                    if (token.count { it == '.' } > 1) throw IllegalArgumentException("Invalid number")
                    output.add(token)
                    prev = "num"
                }
                token == "(" -> {
                    ops.addLast(token)
                    prev = "("
                }
                token == ")" -> {
                    while (ops.isNotEmpty() && ops.last() != "(") {
                        output.add(ops.removeLast())
                    }
                    if (ops.isEmpty() || ops.removeLast() != "(") {
                        throw IllegalArgumentException("Bracket mismatch")
                    }
                    prev = ")"
                }
                else -> {
                    if (token == "-" && (prev == "start" || prev == "(" || prev == "op")) {
                        token = "u-"
                    }
                    val tokenPrec = precedence[token] ?: throw IllegalArgumentException("Invalid operator")
                    while (ops.isNotEmpty() && ops.last() != "(") {
                        val top = ops.last()
                        val topPrec = precedence[top] ?: 0
                        if (topPrec > tokenPrec || (topPrec == tokenPrec && token != "u-")) {
                            output.add(ops.removeLast())
                        } else {
                            break
                        }
                    }
                    ops.addLast(token)
                    prev = "op"
                }
            }
        }

        while (ops.isNotEmpty()) {
            val op = ops.removeLast()
            if (op == "(") throw IllegalArgumentException("Bracket mismatch")
            output.add(op)
        }

        val stack = ArrayDeque<Double>()
        for (token in output) {
            val num = token.toDoubleOrNull()
            if (num != null) {
                stack.addLast(num)
            } else if (token == "u-") {
                if (stack.isEmpty()) throw IllegalArgumentException("Invalid expression")
                stack.addLast(-stack.removeLast())
            } else {
                if (stack.size < 2) throw IllegalArgumentException("Invalid expression")
                val b = stack.removeLast()
                val a = stack.removeLast()
                val res = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> {
                        if (b == 0.0) throw IllegalArgumentException("Cannot divide by zero")
                        a / b
                    }
                    "%" -> {
                        if (b == 0.0) throw IllegalArgumentException("Cannot divide by zero")
                        a % b
                    }
                    else -> throw IllegalArgumentException("Invalid operator")
                }
                stack.addLast(res)
            }
        }

        if (stack.size != 1 || !stack.first().isFinite()) {
            throw IllegalArgumentException("Invalid expression")
        }
        return stack.first()
    }

    fun formatResult(value: Double): String {
        if (!value.isFinite()) return "Error"
        val bd = BigDecimal(value).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros()
        return bd.toPlainString()
    }
}

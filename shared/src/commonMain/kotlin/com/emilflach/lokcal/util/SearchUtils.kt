package com.emilflach.lokcal.util

/**
 * Shared helpers for order-agnostic tokenized search across entities (e.g., foods, meals).
 *
 * Centralized here to keep repository code lean and the behavior consistent.
 *
 * Concepts implemented:
 * - Tokenization: split a query into lowercase tokens by whitespace.
 * - exactMatch/prefixMatch: case-insensitive checks across a list of fields.
 * - containsPos: best (lowest) position where the full query appears in any field; 9999 if absent.
 * - tokensPresent: each token must be present in at least one field (order agnostic, cross-field).
 * - tokensPosSum: sum of the best positions for each token across fields; lower is better.
 */
object SearchUtils {
    /** Split already-lowercased query into tokens. */
    fun tokenize(qLower: String): List<String> = qLower.split(Regex("\\s+")).filter { it.isNotBlank() }

    /** Any field equals query (case-insensitive). */
    fun exactMatch(fields: List<String>, q: String): Boolean = fields.any { it.equals(q, ignoreCase = true) }

    /** Any field starts with query (case-insensitive). */
    fun prefixMatch(fields: List<String>, q: String): Boolean = fields.any { it.startsWith(q, ignoreCase = true) }

    /** Best position (lowest index) of the lowercase query within lowercase fields. Returns 9999 if not found. */
    fun containsPos(fields: List<String>, qLower: String): Int {
        var best = Int.MAX_VALUE
        for (s in fields) {
            val idx = s.lowercase().indexOf(qLower)
            if (idx >= 0 && idx < best) best = idx
        }
        return if (best == Int.MAX_VALUE) 9999 else best
    }

    /** True if every token is contained in at least one field (case-insensitive, order-agnostic, cross-field). */
    fun tokensPresent(fields: List<String>, tokens: List<String>): Boolean {
        if (tokens.isEmpty()) return true
        val lowers = fields.map { it.lowercase() }
        return tokens.all { t -> lowers.any { s -> s.contains(t) } }
    }

    /** Sum of best positions over tokens; uses lowercase fields; 9999 per token if missing. */
    fun tokensPosSum(fields: List<String>, tokens: List<String>): Int {
        if (tokens.isEmpty()) return 0
        val lowers = fields.map { it.lowercase() }
        var sum = 0
        for (t in tokens) {
            var best = Int.MAX_VALUE
            for (s in lowers) {
                val idx = s.indexOf(t)
                if (idx >= 0 && idx < best) best = idx
            }
            sum += if (best == Int.MAX_VALUE) 9999 else best
        }
        return sum
    }

    /** Longest token convenience. Returns null when tokens is empty. */
    fun longestToken(tokens: List<String>): String? = tokens.maxByOrNull { it.length }
}

fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    val n = a.length
    val m = b.length
    val dp = IntArray(m + 1) { it }
    var prevDiag: Int
    var prev: Int
    for (i in 1..n) {
        prev = dp[0]
        dp[0] = i
        for (j in 1..m) {
            prevDiag = prev
            prev = dp[j]
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            dp[j] = minOf(
                dp[j] + 1,        // deletion
                dp[j - 1] + 1,    // insertion
                prevDiag + cost   // substitution
            )
        }
    }
    return dp[m]
}
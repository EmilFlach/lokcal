package com.emilflach.lokcal.util

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

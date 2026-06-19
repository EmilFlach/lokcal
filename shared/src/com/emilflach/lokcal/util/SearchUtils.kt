package com.emilflach.lokcal.util

private val charNormMap: Map<Char, String> = mapOf(
    // Diacritics
    'à' to "a", 'á' to "a", 'â' to "a", 'ã' to "a", 'ä' to "a", 'å' to "a",
    'æ' to "ae",
    'ç' to "c",
    'è' to "e", 'é' to "e", 'ê' to "e", 'ë' to "e",
    'ì' to "i", 'í' to "i", 'î' to "i", 'ï' to "i",
    'ð' to "d",
    'ñ' to "n",
    'ò' to "o", 'ó' to "o", 'ô' to "o", 'õ' to "o", 'ö' to "o", 'ø' to "o",
    'ù' to "u", 'ú' to "u", 'û' to "u", 'ü' to "u",
    'ý' to "y", 'ÿ' to "y",
    'þ' to "th",
    'ß' to "ss",
    'œ' to "oe",
    // Punctuation equivalences
    '&' to "and",  // "Ben & Jerry's" ↔ "ben and jerry"
    '\'' to "",    // strip ASCII apostrophe: "Jerry's" → "Jerrys"
    '\u2019' to "", // strip right single quote ('): "Jerry's" → "Jerrys"
)

/** Normalizes characters for search: strips diacritics, maps "&" → "and", strips apostrophes. Input should be lowercased first. */
fun normalize(s: String): String = buildString(s.length) {
    for (c in s) append(charNormMap[c] ?: c)
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
                dp[j] + 1,
                dp[j - 1] + 1,
                prevDiag + cost
            )
        }
    }
    return dp[m]
}

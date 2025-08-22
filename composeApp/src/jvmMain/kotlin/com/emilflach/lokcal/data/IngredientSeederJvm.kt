package com.emilflach.lokcal.data

import java.io.File

actual fun loadIngredientsJsonText(): String? {
    // Try classpath resources
    val cl = Thread.currentThread().contextClassLoader
        ?: IngredientSeeder::class.java.classLoader
    val candidates = listOf(
        "ingredients.json",
        "com/emilflach/lokcal/data/ingredients.json"
    )
    for (p in candidates) {
        val stream = cl?.getResourceAsStream(p)
        if (stream != null) {
            return stream.bufferedReader().use { it.readText() }
        }
    }
    // Dev fallback: read from source tree when running from repo
    val fileCandidates = listOf(
        "composeApp/src/commonMain/resources/ingredients.json",
        "composeApp/src/commonMain/kotlin/com/emilflach/lokcal/data/ingredients.json"
    )
    for (fp in fileCandidates) {
        val f = File(fp)
        if (f.exists()) return f.readText()
    }
    return null
}

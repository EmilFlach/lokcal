package com.emilflach.lokcal.data

actual fun loadIngredientsJsonText(): String? {
    // Try classpath resources packaged (not typical on Android). Return null if not found.
    val cl = IngredientSeeder::class.java.classLoader
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
    // Android-specific JSON is provided via IngredientSeeder.provideJsonText set from DriverFactory.android
    return null
}

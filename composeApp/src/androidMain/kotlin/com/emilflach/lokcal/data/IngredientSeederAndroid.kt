package com.emilflach.lokcal.data

actual fun loadIngredientsJsonText(): String? {
    // Try classpath resources packaged from commonMain/resources
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
    // If not found, return null (no seeding on Android until file is bundled as a resource)
    return null
}

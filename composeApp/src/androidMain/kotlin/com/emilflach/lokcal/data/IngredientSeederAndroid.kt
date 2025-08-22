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
    // Android fallback: provide a tiny built-in seed so the app is not empty on first run.
    // This keeps the change minimal without requiring Android-specific asset wiring.
    val fallback = """
        [
          {"id":"builtin-1","name":"Apple","description":"Raw apple","pluralName":"Apples","labelId":"builtin","onHand":false,
            "extras":{"kcal":"52","servingSize":"100","englishName":"Apple","dutchName":"Appel","source":"builtin"}},
          {"id":"builtin-2","name":"Banana","description":"Raw banana","pluralName":"Bananas","labelId":"builtin","onHand":false,
            "extras":{"kcal":"89","servingSize":"100","englishName":"Banana","dutchName":"Banaan","source":"builtin"}},
          {"id":"builtin-3","name":"Bread","description":"White bread","pluralName":"Bread","labelId":"builtin","onHand":false,
            "extras":{"kcal":"265","servingSize":"30","englishName":"Bread","dutchName":"Brood","source":"builtin"}}
        ]
    """.trimIndent()
    return fallback
}

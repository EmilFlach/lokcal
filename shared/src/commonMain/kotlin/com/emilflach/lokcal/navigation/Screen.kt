package com.emilflach.lokcal.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
internal sealed interface Screen : NavKey {
    fun browserInfo(): Pair<String, Map<String, String>>

    @Serializable
    data class Main(val dateIso: String) : Screen {
        override fun browserInfo() = "main" to mapOf("date" to dateIso)
    }
    @Serializable
    data class MealTime(val mealType: String, val dateIso: String, val highlightLatest: Boolean = false) : Screen {
        override fun browserInfo() = "meal_time" to mapOf("type" to mealType, "date" to dateIso)
    }
    @Serializable
    data class Intake(val mealType: String, val dateIso: String) : Screen {
        override fun browserInfo() = "intake" to mapOf("type" to mealType, "date" to dateIso)
    }
    @Serializable
    data class EditMeal(val mealId: Long, val returnMealType: String, val dateIso: String) : Screen {
        override fun browserInfo() = "edit_meal" to mapOf("id" to mealId.toString(), "date" to dateIso)
    }
    // Settings flow
    @Serializable
    data object Settings : Screen {
        override fun browserInfo() = "settings" to emptyMap<String, String>()
    }
    @Serializable
    data class MealsManage(val dateIso: String) : Screen {
        override fun browserInfo() = "meals_manage" to mapOf("date" to dateIso)
    }
    @Serializable
    data class FoodManage(val dateIso: String) : Screen {
        override fun browserInfo() = "food_manage" to mapOf("date" to dateIso)
    }
    @Serializable
    data class FoodEdit(val foodId: Long?, val dateIso: String) : Screen {
        override fun browserInfo() = "food_edit" to mapOf("id" to (foodId?.toString() ?: ""), "date" to dateIso)
    }
    @Serializable
    data class EditMealFromList(val mealId: Long, val dateIso: String) : Screen {
        override fun browserInfo() = "edit_meal_from_list" to mapOf("id" to mealId.toString(), "date" to dateIso)
    }
    // Exercise flow
    @Serializable
    data class ExerciseList(val dateIso: String) : Screen {
        override fun browserInfo() = "exercise_list" to mapOf("date" to dateIso)
    }
    // Weight flow
    @Serializable
    sealed interface ReturnTo {
        @Serializable
        data object Settings : ReturnTo
        @Serializable
        data class Main(val dateIso: String) : ReturnTo
    }
    @Serializable
    data class WeightList(val openAdd: Boolean = false, val returnTo: ReturnTo) : Screen {
        override fun browserInfo() = "weight_list" to emptyMap<String, String>()
    }
    // Stats flow
    @Serializable
    data object Statistics : Screen {
        override fun browserInfo() = "statistics" to emptyMap<String, String>()
    }
}

internal val navigationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Screen.Main::class, Screen.Main.serializer())
            subclass(Screen.MealTime::class, Screen.MealTime.serializer())
            subclass(Screen.Intake::class, Screen.Intake.serializer())
            subclass(Screen.EditMeal::class, Screen.EditMeal.serializer())
            subclass(Screen.Settings::class, Screen.Settings.serializer())
            subclass(Screen.MealsManage::class, Screen.MealsManage.serializer())
            subclass(Screen.FoodManage::class, Screen.FoodManage.serializer())
            subclass(Screen.FoodEdit::class, Screen.FoodEdit.serializer())
            subclass(Screen.EditMealFromList::class, Screen.EditMealFromList.serializer())
            subclass(Screen.ExerciseList::class, Screen.ExerciseList.serializer())
            subclass(Screen.WeightList::class, Screen.WeightList.serializer())
            subclass(Screen.Statistics::class, Screen.Statistics.serializer())
        }
    }
}

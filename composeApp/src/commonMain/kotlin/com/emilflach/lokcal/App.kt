package com.emilflach.lokcal

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.emilflach.lokcal.data.*
import com.emilflach.lokcal.theme.AppTheme
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.screens.*
import com.emilflach.lokcal.util.SystemBackHandler
import com.emilflach.lokcal.util.currentDateIso
import com.emilflach.lokcal.viewmodel.*

private sealed class Screen {
    data class Main(val dateIso: String) : Screen()
    data class MealTime(val mealType: String, val dateIso: String, val highlightLatest: Boolean = false) : Screen()
    data class Intake(val mealType: String, val dateIso: String) : Screen()
    data class EditMeal(val mealId: Long, val returnMealType: String, val dateIso: String) : Screen()
    // Settings flow
    data object Settings : Screen()
    data object MealsList : Screen()
    data object FoodManage : Screen()
    data class FoodEdit(val foodId: Long?) : Screen()
    data class EditMealFromList(val mealId: Long) : Screen()
    // Exercise flow
    data class ExerciseList(val dateIso: String) : Screen()
    data class ExerciseAdd(val dateIso: String) : Screen()
    data class EditExercise(val exerciseId: Long, val dateIso: String) : Screen()
    // Weight flow
    sealed class ReturnTo {
        data object Settings : ReturnTo()
        data class Main(val dateIso: String) : ReturnTo()
    }
    data class WeightList(val openAdd: Boolean = false, val returnTo: ReturnTo) : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun App(sqlDriverFactory: SqlDriverFactory) = AppTheme {
    // Database and repositories
    val database = remember(sqlDriverFactory) { createDatabase(sqlDriverFactory) }
    val foodRepo = remember(database) { FoodRepository(database) }
    val intakeRepo = remember(database) { IntakeRepository(database) }
    val mealRepo = remember(database) { MealRepository(database) }
    val exerciseRepo = remember(database) { ExerciseRepository(database) }
    val weightRepo = remember(database) { WeightRepository(database) }
    val settingsRepo = remember(database) { SettingsRepository(database) }

    var screen by remember { mutableStateOf<Screen>(Screen.Main(currentDateIso())) }
    var refreshToggle by remember { mutableStateOf(false) }

    // Handle Android system back: navigate back from Intake/Detail to previous
    SystemBackHandler(enabled = true) {
        when (val s = screen) {
            is Screen.Intake -> {
                // Go back to meal time from intake
                screen = Screen.MealTime(s.mealType, s.dateIso)
                refreshToggle = !refreshToggle
            }
            is Screen.EditMeal -> {
                screen = Screen.MealTime(s.returnMealType, s.dateIso)
                refreshToggle = !refreshToggle
            }
            is Screen.MealTime -> {
                screen = Screen.Main(s.dateIso)
                refreshToggle = !refreshToggle
            }
            is Screen.ExerciseList -> {
                screen = Screen.Main(s.dateIso)
                refreshToggle = !refreshToggle
            }
            is Screen.ExerciseAdd -> {
                screen = Screen.ExerciseList(s.dateIso)
                refreshToggle = !refreshToggle
            }
            is Screen.EditExercise -> {
                screen = Screen.ExerciseList(s.dateIso)
                refreshToggle = !refreshToggle
            }
            Screen.Settings -> {
                screen = Screen.Main(currentDateIso())
                refreshToggle = !refreshToggle
            }
            Screen.MealsList -> {
                screen = Screen.Settings
                refreshToggle = !refreshToggle
            }
            Screen.FoodManage -> {
                screen = Screen.Settings
                refreshToggle = !refreshToggle
            }
            is Screen.FoodEdit -> {
                screen = Screen.FoodManage
                refreshToggle = !refreshToggle
            }
            is Screen.EditMealFromList -> {
                screen = Screen.MealsList
                refreshToggle = !refreshToggle
            }
            is Screen.WeightList -> {
                when (val r = s.returnTo) {
                    is Screen.ReturnTo.Settings -> {
                        screen = Screen.Settings
                    }
                    is Screen.ReturnTo.Main -> {
                        screen = Screen.Main(r.dateIso)
                        // Refresh to update Thursday banner based on potential new weight
                        refreshToggle = !refreshToggle
                    }
                }
            }
            else -> {
                // On Main screen: allow default behavior (app can close). No-op here.
            }
        }
    }

    val recipesColors = LocalRecipesColors.current

    Surface(
        color = recipesColors.backgroundPage,
        contentColor = recipesColors.foregroundDefault
    ) {
        when (val s = screen) {
            is Screen.Main -> {
                // Recreate VM when refreshToggle changes
                val vm = remember(intakeRepo, exerciseRepo, weightRepo, settingsRepo, s.dateIso, refreshToggle) { MainViewModel(intakeRepo, exerciseRepo, weightRepo, settingsRepo, s.dateIso) }
                MainScreen(
                    viewModel = vm,
                    onOpenMeal = { meal, dateIso -> screen = Screen.MealTime(meal, dateIso) },
                    onOpenExercise = { dateIso -> screen = Screen.ExerciseList(dateIso) },
                    onOpenSettings = { screen = Screen.Settings },
                    onOpenWeightToday = { screen = Screen.WeightList(openAdd = true, returnTo = Screen.ReturnTo.Main(s.dateIso)) },
                    onOpenWeightList = { screen = Screen.WeightList(openAdd = false, returnTo = Screen.ReturnTo.Main(s.dateIso)) }
                )
            }
            is Screen.MealTime -> {
                val vm = remember(intakeRepo, s.mealType, s.dateIso, refreshToggle) { MealTimeViewModel(intakeRepo, s.mealType, s.dateIso) }
                MealTimeScreen(
                    viewModel = vm,
                    onBack = { screen = Screen.Main(s.dateIso); refreshToggle = !refreshToggle },
                    onAdd = { meal ->
                        screen = Screen.Intake(meal, s.dateIso)
                    },
                    shouldHighlightLatest = s.highlightLatest
                )
            }
            is Screen.Intake -> {
                val intakeVm = remember(foodRepo, intakeRepo, s.mealType, s.dateIso) { IntakeViewModel(foodRepo, intakeRepo, s.mealType, s.dateIso) }
                IntakeScreen(
                    viewModel = intakeVm,
                    onDone = { itemAdded ->
                        // Navigate back to meal time and refresh
                        screen = Screen.MealTime(s.mealType, s.dateIso, highlightLatest = itemAdded)
                        refreshToggle = !refreshToggle
                    },
                    autoFocusSearch = true
                )
            }
            is Screen.EditMeal -> {
                val editVm = remember(intakeRepo, s.mealId) { EditMealViewModel(mealRepo, s.mealId) }
                EditMealScreen(
                    viewModel = editVm,
                    onBack = { screen = Screen.MealTime(s.returnMealType, s.dateIso); refreshToggle = !refreshToggle },
                    onDeleted = { screen = Screen.MealTime(s.returnMealType, s.dateIso); refreshToggle = !refreshToggle }
                )
            }
            Screen.FoodManage -> {
                val foodVm = remember(foodRepo, refreshToggle) { FoodEditViewModel(foodRepo) }
                FoodManageScreen(
                    viewModel = foodVm,
                    onBack = { screen = Screen.Settings },
                    onOpenEdit = { id -> screen = Screen.FoodEdit(id) }
                )
            }
            is Screen.FoodEdit -> {
                val foodVm = remember(foodRepo, refreshToggle) { FoodEditViewModel(foodRepo) }
                FoodEditScreen(
                    viewModel = foodVm,
                    foodId = s.foodId,
                    onBack = { screen = Screen.FoodManage },
                    onSaved = { screen = Screen.FoodManage; refreshToggle = !refreshToggle },
                    onDeleted = { screen = Screen.FoodManage; refreshToggle = !refreshToggle }
                )
            }
            Screen.Settings -> {
                SettingsScreen(
                    onBack = { screen = Screen.Main(currentDateIso()) },
                    onOpenMealsList = { screen = Screen.MealsList },
                    onOpenWeightList = { screen = Screen.WeightList(returnTo = Screen.ReturnTo.Settings) },
                    onOpenFoodManage = { screen = Screen.FoodManage },
                    settingsRepo = settingsRepo
                )
            }
            Screen.MealsList -> {
                MealsListScreen(
                    repo = intakeRepo,
                    onBack = { screen = Screen.Settings },
                    onOpenMeal = { id -> screen = Screen.EditMealFromList(id) }
                )
            }
            is Screen.EditMealFromList -> {
                val editVm = remember(intakeRepo, s.mealId) { EditMealViewModel(mealRepo, s.mealId) }
                EditMealScreen(
                    viewModel = editVm,
                    onBack = { screen = Screen.MealsList; refreshToggle = !refreshToggle },
                    onDeleted = { screen = Screen.MealsList; refreshToggle = !refreshToggle }
                )
            }
            is Screen.ExerciseList -> {
                val vm = remember(exerciseRepo, s.dateIso, refreshToggle) {
                    ExerciseListViewModel(
                        exerciseRepo,
                        s.dateIso
                    )
                }
                ExerciseListScreen(
                    viewModel = vm,
                    onBack = { screen = Screen.Main(s.dateIso) },
                    onAdd = { screen = Screen.ExerciseAdd(s.dateIso) },
                    onEdit = { id -> screen = Screen.EditExercise(id, s.dateIso) }
                )
            }
            is Screen.ExerciseAdd -> {
                val exVm = remember(exerciseRepo, s.dateIso) {
                    ExerciseViewModel(
                        exerciseRepo,
                        s.dateIso
                    )
                }
                ExerciseScreen(
                    viewModel = exVm,
                    onBack = { screen = Screen.ExerciseList(s.dateIso) },
                    onSaved = { screen = Screen.ExerciseList(s.dateIso); refreshToggle = !refreshToggle }
                )
            }
            is Screen.EditExercise -> {
                val vm = remember(exerciseRepo, s.exerciseId, refreshToggle) {
                    EditExerciseViewModel(
                        exerciseRepo,
                        s.exerciseId
                    )
                }
                EditExerciseScreen(
                    viewModel = vm,
                    onBack = { screen = Screen.ExerciseList(s.dateIso) },
                    onSaved = { screen = Screen.ExerciseList(s.dateIso); refreshToggle = !refreshToggle },
                    onDeleted = { screen = Screen.ExerciseList(s.dateIso); refreshToggle = !refreshToggle }
                )
            }
            is Screen.WeightList -> {
                val vm = remember(weightRepo, s.openAdd, refreshToggle) {
                    WeightListViewModel(
                        weightRepo
                    )
                }
                val onBackAction: () -> Unit = when (val r = s.returnTo) {
                    is Screen.ReturnTo.Settings -> {
                        { screen = Screen.Settings }
                    }
                    is Screen.ReturnTo.Main -> {
                        { screen = Screen.Main(r.dateIso); refreshToggle = !refreshToggle }
                    }
                }
                WeightListScreen(
                    viewModel = vm,
                    onBack = onBackAction,
                    openAdd = s.openAdd
                )
            }
        }
    }
}

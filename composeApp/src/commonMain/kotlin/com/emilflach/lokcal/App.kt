package com.emilflach.lokcal

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.emilflach.lokcal.data.FoodRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.MealRepository
import com.emilflach.lokcal.data.SqlDriverFactory
import com.emilflach.lokcal.data.createDatabase
import com.emilflach.lokcal.theme.AppTheme
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.screens.EditMealScreen
import com.emilflach.lokcal.ui.screens.IntakeScreen
import com.emilflach.lokcal.ui.screens.MainScreen
import com.emilflach.lokcal.ui.screens.MealTimeScreen
import com.emilflach.lokcal.ui.screens.MealsListScreen
import com.emilflach.lokcal.ui.screens.SettingsScreen
import com.emilflach.lokcal.util.SystemBackHandler
import com.emilflach.lokcal.viewmodel.EditMealViewModel
import com.emilflach.lokcal.viewmodel.IntakeViewModel
import com.emilflach.lokcal.viewmodel.MainViewModel
import com.emilflach.lokcal.viewmodel.MealTimeViewModel

private sealed class Screen {
    data object Main : Screen()
    data class MealTime(val mealType: String) : Screen()
    data class Intake(val mealType: String) : Screen()
    data class EditMeal(val mealId: Long, val returnMealType: String) : Screen()
    // Settings flow
    data object Settings : Screen()
    data object MealsList : Screen()
    data class EditMealFromList(val mealId: Long) : Screen()
    // Exercise flow
    data object ExerciseList : Screen()
    data object ExerciseAdd : Screen()
    data class EditExercise(val exerciseId: Long) : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun App(sqlDriverFactory: SqlDriverFactory) = AppTheme {
    // Database and repositories
    val database = remember(sqlDriverFactory) { createDatabase(sqlDriverFactory) }
    val foodRepo = remember(database) { FoodRepository(database) }
    val intakeRepo = remember(database) { IntakeRepository(database) }
    val mealRepo = remember(database) { MealRepository(database) }
    val exerciseRepo = remember(database) { com.emilflach.lokcal.data.ExerciseRepository(database) }

    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var refreshToggle by remember { mutableStateOf(false) }

    // Handle Android system back: navigate back from Intake/Detail to previous
    SystemBackHandler(enabled = true) {
        when (val s = screen) {
            is Screen.Intake -> {
                // Go back to meal time from intake
                screen = Screen.MealTime(s.mealType)
                refreshToggle = !refreshToggle
            }
            is Screen.EditMeal -> {
                screen = Screen.MealTime(s.returnMealType)
                refreshToggle = !refreshToggle
            }
            is Screen.MealTime -> {
                screen = Screen.Main
                refreshToggle = !refreshToggle
            }
            Screen.ExerciseList -> {
                screen = Screen.Main
                refreshToggle = !refreshToggle
            }
            Screen.ExerciseAdd -> {
                screen = Screen.ExerciseList
                refreshToggle = !refreshToggle
            }
            is Screen.EditExercise -> {
                screen = Screen.ExerciseList
                refreshToggle = !refreshToggle
            }
            Screen.Settings -> {
                screen = Screen.Main
                refreshToggle = !refreshToggle
            }
            Screen.MealsList -> {
                screen = Screen.Settings
                refreshToggle = !refreshToggle
            }
            is Screen.EditMealFromList -> {
                screen = Screen.MealsList
                refreshToggle = !refreshToggle
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
            Screen.Main -> {
                // Recreate VM when refreshToggle changes
                val vm = remember(intakeRepo, exerciseRepo, refreshToggle) { MainViewModel(intakeRepo, exerciseRepo) }
                MainScreen(
                    viewModel = vm,
                    onOpenMeal = { meal -> screen = Screen.MealTime(meal) },
                    onOpenExercise = { screen = Screen.ExerciseList },
                    onOpenSettings = { screen = Screen.Settings }
                )
            }
            is Screen.MealTime -> {
                val vm = remember(intakeRepo, s.mealType, refreshToggle) { MealTimeViewModel(intakeRepo, s.mealType) }
                MealTimeScreen(
                    viewModel = vm,
                    onBack = { screen = Screen.Main; refreshToggle = !refreshToggle },
                    onAdd = { meal ->
                        screen = Screen.Intake(meal)
                    },
                    onEditMeal = { mealId ->
                        screen = Screen.EditMeal(mealId, s.mealType)
                    }
                )
            }
            is Screen.Intake -> {
                val intakeVm = remember(foodRepo, intakeRepo, s.mealType) { IntakeViewModel(foodRepo, intakeRepo, s.mealType) }
                IntakeScreen(
                    viewModel = intakeVm,
                    onDone = {
                        // Navigate back to meal time and refresh
                        screen = Screen.MealTime(s.mealType)
                        refreshToggle = !refreshToggle
                    },
                    autoFocusSearch = true
                )
            }
            is Screen.EditMeal -> {
                val editVm = remember(intakeRepo, s.mealId) { EditMealViewModel(mealRepo, s.mealId) }
                EditMealScreen(
                    viewModel = editVm,
                    onBack = { screen = Screen.MealTime(s.returnMealType); refreshToggle = !refreshToggle },
                    onDeleted = { screen = Screen.MealTime(s.returnMealType); refreshToggle = !refreshToggle }
                )
            }
            Screen.Settings -> {
                SettingsScreen(
                    onBack = { screen = Screen.Main },
                    onOpenMealsList = { screen = Screen.MealsList }
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
            Screen.ExerciseList -> {
                val vm = remember(exerciseRepo, refreshToggle) { com.emilflach.lokcal.viewmodel.ExerciseListViewModel(exerciseRepo) }
                com.emilflach.lokcal.ui.screens.ExerciseListScreen(
                    viewModel = vm,
                    onBack = { screen = Screen.Main },
                    onAdd = { screen = Screen.ExerciseAdd },
                    onEdit = { id -> screen = Screen.EditExercise(id) }
                )
            }
            Screen.ExerciseAdd -> {
                val exVm = remember(exerciseRepo) { com.emilflach.lokcal.viewmodel.ExerciseViewModel(exerciseRepo) }
                com.emilflach.lokcal.ui.screens.ExerciseScreen(
                    viewModel = exVm,
                    onBack = { screen = Screen.ExerciseList },
                    onSaved = { screen = Screen.ExerciseList; refreshToggle = !refreshToggle }
                )
            }
            is Screen.EditExercise -> {
                val vm = remember(exerciseRepo, s.exerciseId, refreshToggle) { com.emilflach.lokcal.viewmodel.EditExerciseViewModel(exerciseRepo, s.exerciseId) }
                com.emilflach.lokcal.ui.screens.EditExerciseScreen(
                    viewModel = vm,
                    onBack = { screen = Screen.ExerciseList },
                    onSaved = { screen = Screen.ExerciseList; refreshToggle = !refreshToggle },
                    onDeleted = { screen = Screen.ExerciseList; refreshToggle = !refreshToggle }
                )
            }
        }
    }
}

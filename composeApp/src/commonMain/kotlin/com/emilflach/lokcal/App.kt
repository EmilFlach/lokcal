package com.emilflach.lokcal

import androidx.compose.runtime.*
import com.emilflach.lokcal.data.FoodRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.SqlDriverFactory
import com.emilflach.lokcal.data.createDatabase
import com.emilflach.lokcal.theme.AppTheme
import com.emilflach.lokcal.ui.screens.IntakeScreen
import com.emilflach.lokcal.ui.screens.MainScreen
import com.emilflach.lokcal.ui.screens.MealDetailScreen
import com.emilflach.lokcal.viewmodel.IntakeViewModel
import com.emilflach.lokcal.viewmodel.MainViewModel
import com.emilflach.lokcal.viewmodel.MealDetailViewModel
import androidx.compose.material3.*
import com.emilflach.lokcal.util.SystemBackHandler

private sealed class Screen {
    data object Main : Screen()
    data class MealDetail(val mealType: String) : Screen()
    data class Intake(val mealType: String) : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun App(sqlDriverFactory: SqlDriverFactory) = AppTheme {
    // Database and repositories
    val database = remember(sqlDriverFactory) { createDatabase(sqlDriverFactory) }
    val foodRepo = remember(database) { FoodRepository(database) }
    val intakeRepo = remember(database) { IntakeRepository(database) }

    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var refreshToggle by remember { mutableStateOf(false) }

    // Handle Android system back: navigate back from Intake/Detail to previous
    SystemBackHandler(enabled = true) {
        when (val s = screen) {
            is Screen.Intake -> {
                // Go back to meal detail from intake
                screen = Screen.MealDetail(s.mealType)
                refreshToggle = !refreshToggle
            }
            is Screen.MealDetail -> {
                screen = Screen.Main
                refreshToggle = !refreshToggle
            }
            else -> {
                // On Main screen: allow default behavior (app can close). No-op here.
            }
        }
    }

    when (val s = screen) {
        Screen.Main -> {
            // Recreate VM when refreshToggle changes
            val vm = remember(intakeRepo, refreshToggle) { MainViewModel(intakeRepo) }
            MainScreen(
                viewModel = vm,
                onOpenMeal = { meal -> screen = Screen.MealDetail(meal) },
            )
        }
        is Screen.MealDetail -> {
            val vm = remember(intakeRepo, s.mealType, refreshToggle) { MealDetailViewModel(intakeRepo, s.mealType) }
            MealDetailScreen(
                viewModel = vm,
                onBack = { screen = Screen.Main; refreshToggle = !refreshToggle },
                onAdd = { meal ->
                    screen = Screen.Intake(meal)
                }
            )
        }
        is Screen.Intake -> {
            val intakeVm = remember(foodRepo, intakeRepo, s.mealType) { IntakeViewModel(foodRepo, intakeRepo, s.mealType) }
            IntakeScreen(
                viewModel = intakeVm,
                onDone = {
                    // Navigate back to meal detail and refresh
                    screen = Screen.MealDetail(s.mealType)
                    refreshToggle = !refreshToggle
                },
                autoFocusSearch = true,
                onChanged = {
                    // Live-refresh the detail/main when returning
                    refreshToggle = !refreshToggle
                }
            )
        }
    }
}

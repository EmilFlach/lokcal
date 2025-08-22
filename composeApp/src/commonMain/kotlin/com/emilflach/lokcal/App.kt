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
import androidx.compose.runtime.saveable.rememberSaveable
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
    var showIntakeSheet by rememberSaveable { mutableStateOf(false) }
    var intakeMealType by rememberSaveable { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Handle Android system back: close sheet if open, else navigate back from detail
    SystemBackHandler(enabled = true) {
        when {
            showIntakeSheet -> {
                // Close the intake sheet first
                showIntakeSheet = false
            }
            screen is Screen.MealDetail -> {
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
            // Recreate VM when refreshToggle changes to force reload logic in screen's LaunchedEffect
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
                    intakeMealType = meal
                    showIntakeSheet = true
                }
            )
        }
        is Screen.Intake -> {
            // Deprecated path: migrate to bottom sheet; keep fallback no-op.
        }
    }

    if (showIntakeSheet && intakeMealType != null) {
        val mealType = intakeMealType!!
        val intakeVm = remember(foodRepo, intakeRepo, mealType) { IntakeViewModel(foodRepo, intakeRepo, mealType) }
        ModalBottomSheet(
            onDismissRequest = { showIntakeSheet = false },
            sheetState = sheetState
        ) {
            // Ensure sheet opens fully
            LaunchedEffect(Unit) { sheetState.expand() }
            IntakeScreen(
                viewModel = intakeVm,
                onDone = {
                    showIntakeSheet = false
                    // Trigger refresh in the underlying screen (MealDetail/Main)
                    refreshToggle = !refreshToggle
                },
                autoFocusSearch = true,
                onChanged = {
                    // Live-refresh the underlying detail screen when items are added/updated
                    refreshToggle = !refreshToggle
                }
            )
        }
    }
}

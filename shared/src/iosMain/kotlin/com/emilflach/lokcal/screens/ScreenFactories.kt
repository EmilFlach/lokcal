package com.emilflach.lokcal.screens

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.emilflach.lokcal.data.*
import com.emilflach.lokcal.health.HealthManager
import com.emilflach.lokcal.theme.AppTheme
import com.emilflach.lokcal.ui.screens.*
import com.emilflach.lokcal.viewmodel.*
import kotlinx.datetime.LocalDate

// Global repositories (will be initialized from App)
private lateinit var globalFoodRepo: FoodRepository
private lateinit var globalIntakeRepo: IntakeRepository
private lateinit var globalMealRepo: MealRepository
private lateinit var globalExerciseRepo: ExerciseRepository
private lateinit var globalWeightRepo: WeightRepository
private lateinit var globalSettingsRepo: SettingsRepository

// Global ViewModels
private lateinit var globalMainViewModel: MainViewModel
private lateinit var globalMealsListViewModel: MealsListViewModel
private lateinit var globalFoodEditViewModel: FoodEditViewModel

/**
 * Initialize repositories and view models. Must be called before using any screen factories.
 */
fun initializeRepositories(
    foodRepo: FoodRepository,
    intakeRepo: IntakeRepository,
    mealRepo: MealRepository,
    exerciseRepo: ExerciseRepository,
    weightRepo: WeightRepository,
    settingsRepo: SettingsRepository,
    mainViewModel: MainViewModel,
    mealsListViewModel: MealsListViewModel,
    foodEditViewModel: FoodEditViewModel
) {
    globalFoodRepo = foodRepo
    globalIntakeRepo = intakeRepo
    globalMealRepo = mealRepo
    globalExerciseRepo = exerciseRepo
    globalWeightRepo = weightRepo
    globalSettingsRepo = settingsRepo
    globalMainViewModel = mainViewModel
    globalMealsListViewModel = mealsListViewModel
    globalFoodEditViewModel = foodEditViewModel
}

/**
 * Get the global MainViewModel to trigger refreshes from SwiftUI
 */
fun getGlobalMainViewModel(): MainViewModel = globalMainViewModel

fun getGlobalFoodEditViewModel(): FoodEditViewModel = globalFoodEditViewModel

fun getGlobalMealsListViewModel(): MealsListViewModel = globalMealsListViewModel

// Main Screen
fun MainViewController(
    dateIso: String,
    onOpenMeal: (String, String) -> Unit,
    onOpenExercise: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWeightToday: () -> Unit,
    onOpenWeightList: () -> Unit,
    onOpenStatistics: () -> Unit,
    refreshKey: Int = 0
) = ComposeUIViewController {
    AppTheme {
        LaunchedEffect(dateIso) {
            globalMainViewModel.loadFor(LocalDate.parse(dateIso))
        }
        LaunchedEffect(refreshKey) {
            globalMainViewModel.refresh()
        }
        MainScreen(
            viewModel = globalMainViewModel,
            onOpenMeal = onOpenMeal,
            onOpenExercise = onOpenExercise,
            onOpenSettings = onOpenSettings,
            onOpenWeightToday = onOpenWeightToday,
            onOpenWeightList = onOpenWeightList,
            onOpenStatistics = onOpenStatistics
        )
    }
}

// MealTime Screen
private var mealTimeViewModels = mutableMapOf<String, MealTimeViewModel>()

fun MealTimeViewController(
    mealType: String,
    dateIso: String,
    shouldHighlightLatest: Boolean,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    refreshKey: Int = 0
) = ComposeUIViewController {
    AppTheme {
        val key = "$mealType-$dateIso-$refreshKey"
        val vm = mealTimeViewModels.getOrPut(key) {
            MealTimeViewModel(globalIntakeRepo, mealType, dateIso)
        }
        MealTimeScreen(
            viewModel = vm,
            onBack = onBack,
            onAdd = onAdd,
            shouldHighlightLatest = shouldHighlightLatest
        )
    }
}

fun getMealTimeViewModel(mealType: String, dateIso: String, refreshKey: Int = 0): MealTimeViewModel {
    val key = "$mealType-$dateIso-$refreshKey"
    return mealTimeViewModels.getOrPut(key) {
        MealTimeViewModel(globalIntakeRepo, mealType, dateIso)
    }
}

// Intake Screen
private var intakeViewModels = mutableMapOf<String, IntakeViewModel>()

fun IntakeViewController(
    mealType: String,
    dateIso: String,
    onDone: (Boolean) -> Unit,
    autoFocusSearch: Boolean = true,
    searchQuery: String = ""
) = ComposeUIViewController {
    AppTheme {
        val key = "$mealType-$dateIso"
        val intakeVm = intakeViewModels.getOrPut(key) {
            IntakeViewModel(globalFoodRepo, globalIntakeRepo, globalMealRepo, globalSettingsRepo, mealType, dateIso)
        }

        // Sync search query from SwiftUI to ViewModel
        LaunchedEffect(searchQuery) {
            intakeVm.setQuery(searchQuery)
        }

        IntakeScreen(
            viewModel = intakeVm,
            onDone = onDone,
            autoFocusSearch = autoFocusSearch
        )
    }
}

fun getIntakeViewModel(mealType: String, dateIso: String): IntakeViewModel {
    val key = "$mealType-$dateIso"
    return intakeViewModels.getOrPut(key) {
        IntakeViewModel(globalFoodRepo, globalIntakeRepo, globalMealRepo, globalSettingsRepo, mealType, dateIso)
    }
}

// EditMeal Screen
fun EditMealViewController(
    mealId: Long,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) = ComposeUIViewController {
    AppTheme {
        val editVm = remember(globalMealRepo, globalFoodRepo, mealId) {
            EditMealViewModel(globalMealRepo, globalFoodRepo, mealId)
        }
        EditMealScreen(
            viewModel = editVm,
            onBack = onBack,
            onDeleted = onDeleted
        )
    }
}

// Settings Screen
fun SettingsViewController(
    onBack: () -> Unit,
    onOpenMealsList: () -> Unit,
    onOpenWeightList: () -> Unit,
    onOpenFoodManage: () -> Unit,
    onOpenSourcePreferences: () -> Unit
) = ComposeUIViewController {
    AppTheme {
        SettingsScreen(
            onBack = onBack,
            onOpenMealsList = onOpenMealsList,
            onOpenWeightList = onOpenWeightList,
            onOpenFoodManage = onOpenFoodManage,
            onOpenSourcePreferences = onOpenSourcePreferences,
            onRequestHealthPermissions = { HealthManager.requestPermissions() },
            settingsRepo = globalSettingsRepo
        )
    }
}

// SourcePreference Screen
fun SourcePreferenceViewController(
    onBack: () -> Unit
) = ComposeUIViewController {
    AppTheme {
        val viewModel = remember(globalSettingsRepo) {
            SourcePreferenceViewModel(globalSettingsRepo)
        }
        SourcePreferenceScreen(
            onBack = onBack,
            viewModel = viewModel
        )
    }
}

// MealsList Screen
fun MealsListViewController(
    onBack: () -> Unit,
    onOpenMeal: (Long) -> Unit,
    searchQuery: String = "",
    showMissingImages: Boolean = false
) = ComposeUIViewController {
    AppTheme {
        LaunchedEffect(searchQuery) {
            globalMealsListViewModel.setSearch(searchQuery)
        }
        LaunchedEffect(showMissingImages) {
            globalMealsListViewModel.setShowMissingImages(showMissingImages)
        }
        MealsListScreen(
            viewModel = globalMealsListViewModel,
            onBack = onBack,
            onOpenMeal = onOpenMeal
        )
    }
}

// EditMealFromList Screen
fun EditMealFromListViewController(
    mealId: Long,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) = ComposeUIViewController {
    AppTheme {
        val editVm = remember(globalMealRepo, globalFoodRepo, mealId) {
            EditMealViewModel(globalMealRepo, globalFoodRepo, mealId)
        }
        EditMealScreen(
            viewModel = editVm,
            onBack = onBack,
            onDeleted = onDeleted
        )
    }
}

// FoodManage Screen
fun FoodManageViewController(
    onBack: () -> Unit,
    onOpenEdit: (Long?) -> Unit,
    searchQuery: String = "",
    showMissingImages: Boolean = false
) = ComposeUIViewController {
    AppTheme {
        LaunchedEffect(searchQuery) {
            globalFoodEditViewModel.setSearch(searchQuery)
        }
        LaunchedEffect(showMissingImages) {
            globalFoodEditViewModel.setShowMissingImages(showMissingImages)
        }
        FoodManageScreen(
            viewModel = globalFoodEditViewModel,
            onBack = onBack,
            onOpenEdit = onOpenEdit
        )
    }
}

// FoodEdit Screen
fun FoodEditViewController(
    foodId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit
) = ComposeUIViewController {
    AppTheme {
        FoodEditScreen(
            viewModel = globalFoodEditViewModel,
            foodId = foodId,
            onBack = onBack,
            onSaved = onSaved,
            onDeleted = onDeleted
        )
    }
}

// ExerciseList Screen
fun ExerciseListViewController(
    dateIso: String,
    onBack: () -> Unit,
    refreshKey: Int = 0
) = ComposeUIViewController {
    AppTheme {
        val vm = remember(globalExerciseRepo, dateIso, refreshKey) {
            ExerciseListViewModel(globalExerciseRepo, dateIso)
        }
        ExerciseListScreen(
            viewModel = vm,
            onBack = onBack,
            onEnableHealth = { HealthManager.requestPermissions() }
        )
    }
}

// WeightList Screen
private var weightListViewModel: WeightListViewModel? = null

fun WeightListViewController(
    openAdd: Boolean,
    onBack: () -> Unit,
    refreshKey: Int = 0
) = ComposeUIViewController {
    AppTheme {
        val vm = weightListViewModel ?: WeightListViewModel(globalWeightRepo).also { weightListViewModel = it }
        WeightListScreen(
            viewModel = vm,
            onBack = onBack,
            openAdd = openAdd
        )
    }
}

fun getWeightListViewModel(): WeightListViewModel {
    return weightListViewModel ?: WeightListViewModel(globalWeightRepo).also { weightListViewModel = it }
}

// Statistics Screen
fun StatisticsViewController(
    onBack: () -> Unit
) = ComposeUIViewController {
    AppTheme {
        val vm = remember(globalIntakeRepo, globalExerciseRepo, globalSettingsRepo) {
            StatisticsViewModel(globalIntakeRepo, globalExerciseRepo, globalSettingsRepo)
        }
        StatisticsScreen(
            viewModel = vm,
            onBack = onBack
        )
    }
}

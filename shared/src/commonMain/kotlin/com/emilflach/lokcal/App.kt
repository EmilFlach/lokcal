package com.emilflach.lokcal

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import com.emilflach.lokcal.data.*
import com.emilflach.lokcal.navigation.AppNavigation
import com.emilflach.lokcal.theme.AppTheme
import com.emilflach.lokcal.ui.screens.AppLoadingScreen
import com.emilflach.lokcal.ui.util.LocalImageCache
import com.emilflach.lokcal.util.currentDateIso
import com.emilflach.lokcal.viewmodel.FoodEditViewModel
import com.emilflach.lokcal.viewmodel.MainViewModel
import com.emilflach.lokcal.viewmodel.MealsListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun App(sqlDriverFactory: SqlDriverFactory) = AppTheme {
    var seedingProgress by remember { mutableStateOf<Float?>(null) }
    val database by produceState<Database?>(null) {
        value = createDatabase(sqlDriverFactory, onProgress = { seedingProgress = it })
    }

    if (database == null) {
        AppLoadingScreen(seedingProgress)
        return@AppTheme
    }

    val foodRepo = remember(database) { FoodRepository(database!!) }
    val intakeRepo = remember(database) { IntakeRepository(database!!) }
    val mealRepo = remember(database) { MealRepository(database!!) }
    val exerciseRepo = remember(database) { ExerciseRepository(database!!) }
    val weightRepo = remember(database) { WeightRepository(database!!) }
    val settingsRepo = remember(database) { SettingsRepository(database!!) }
    val imageCacheRepo = remember(database) { ImageCacheRepository(database!!) }

    val mainViewModel = remember(intakeRepo, exerciseRepo, weightRepo, settingsRepo) {
        MainViewModel(intakeRepo, exerciseRepo, weightRepo, settingsRepo, currentDateIso())
    }
    val mealsListViewModel = remember(intakeRepo, mealRepo, imageCacheRepo) { MealsListViewModel(intakeRepo, mealRepo, imageCacheRepo) }
    val foodEditViewModel = remember(foodRepo, intakeRepo, mealRepo, imageCacheRepo) { FoodEditViewModel(foodRepo, intakeRepo, mealRepo, imageCacheRepo) }

    val onboardingComplete by produceState<Boolean?>(null, settingsRepo) {
        value = settingsRepo.isOnboardingComplete()
    }
    if (onboardingComplete == null) {
        AppLoadingScreen(null)
        return@AppTheme
    }

    CompositionLocalProvider(LocalImageCache provides imageCacheRepo) {
        AppNavigation(
            foodRepo = foodRepo,
            intakeRepo = intakeRepo,
            mealRepo = mealRepo,
            exerciseRepo = exerciseRepo,
            weightRepo = weightRepo,
            settingsRepo = settingsRepo,
            mainViewModel = mainViewModel,
            mealsListViewModel = mealsListViewModel,
            foodEditViewModel = foodEditViewModel,
            startOnboarding = onboardingComplete == false,
        )
    }
}

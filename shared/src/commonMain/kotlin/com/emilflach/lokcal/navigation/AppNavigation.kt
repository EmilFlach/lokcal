package com.emilflach.lokcal.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.emilflach.lokcal.data.*
import com.emilflach.lokcal.health.HealthManager
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.screens.*
import com.emilflach.lokcal.util.currentDateIso
import com.emilflach.lokcal.viewmodel.*
import kotlinx.datetime.LocalDate

@Composable
internal fun AppNavigation(
    foodRepo: FoodRepository,
    intakeRepo: IntakeRepository,
    mealRepo: MealRepository,
    exerciseRepo: ExerciseRepository,
    weightRepo: WeightRepository,
    settingsRepo: SettingsRepository,
    mainViewModel: MainViewModel,
    mealsListViewModel: MealsListViewModel,
    foodEditViewModel: FoodEditViewModel,
) {
    val colors = LocalRecipesColors.current
    var refreshToggle by remember { mutableStateOf(false) }
    val backStack = rememberNavBackStack(navigationConfig, Screen.Main(currentDateIso()))
    val mainUiStateHolder = mainViewModel.uiState.collectAsState()
    val effectiveCurrentDestination = remember {
        derivedStateOf {
            val dest = backStack.lastOrNull()
            if (dest is Screen.Main) Screen.Main(mainUiStateHolder.value.selectedDate.toString())
            else dest
        }
    }

    Surface(
        color = colors.backgroundPage,
        contentColor = colors.foregroundDefault
    ) {
        BrowserNavigationEffect(
            currentDestination = effectiveCurrentDestination,
            nameResolver = { key -> (key as? Screen)?.browserInfo() },
        )

        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
            },
            popTransitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
            },
            entryProvider = entryProvider {
                entry<Screen.Main> { s ->
                    LaunchedEffect(s.dateIso) {
                        mainViewModel.loadFor(LocalDate.parse(s.dateIso))
                    }
                    LaunchedEffect(refreshToggle) {
                        mainViewModel.refresh()
                    }
                    MainScreen(
                        viewModel = mainViewModel,
                        onOpenMeal = { meal, dateIso -> backStack.add(Screen.MealTime(meal, dateIso)) },
                        onOpenExercise = { dateIso -> backStack.add(Screen.ExerciseList(dateIso)) },
                        onOpenSettings = { backStack.add(Screen.Settings) },
                        onOpenWeightToday = { backStack.add(Screen.WeightList(openAdd = true, returnTo = Screen.ReturnTo.Main(s.dateIso))) },
                        onOpenWeightList = { backStack.add(Screen.WeightList(openAdd = false, returnTo = Screen.ReturnTo.Main(s.dateIso))) },
                        onOpenStatistics = { backStack.add(Screen.Statistics) }
                    )
                }
                entry<Screen.MealTime>(
                    metadata = NavDisplay.transitionSpec {
                        (scaleIn(
                            initialScale = 0.8f,
                            animationSpec = tween(200)
                        ) + fadeIn(
                            animationSpec = tween(200)
                        )) togetherWith fadeOut(animationSpec = tween(200))
                    } + NavDisplay.popTransitionSpec {
                        fadeIn(animationSpec = tween(200)) togetherWith
                            (scaleOut(
                                targetScale = 0.8f,
                                animationSpec = tween(200)
                            ) + fadeOut(animationSpec = tween(200)))
                    }
                ) { s ->
                    val vm = remember(intakeRepo, s.mealType, s.dateIso, refreshToggle) { MealTimeViewModel(intakeRepo, s.mealType, s.dateIso) }
                    MealTimeScreen(
                        viewModel = vm,
                        onBack = { backStack.removeLastOrNull(); refreshToggle = !refreshToggle },
                        onAdd = { meal -> backStack.add(Screen.Intake(meal, s.dateIso)) },
                        shouldHighlightLatest = s.highlightLatest
                    )
                }
                entry<Screen.Intake>(
                    metadata = NavDisplay.transitionSpec {
                        EnterTransition.None togetherWith ExitTransition.KeepUntilTransitionsFinished
                    } + NavDisplay.popTransitionSpec {
                        EnterTransition.None togetherWith ExitTransition.None
                    }
                ) { s ->
                    val intakeVm = remember(foodRepo, intakeRepo, mealRepo, settingsRepo, s.mealType, s.dateIso) {
                        IntakeViewModel(foodRepo, intakeRepo, mealRepo, settingsRepo, s.mealType, s.dateIso)
                    }
                    IntakeScreen(
                        viewModel = intakeVm,
                        onDone = { itemAdded ->
                            backStack.apply {
                                removeLastOrNull()
                                removeLastOrNull()
                                add(Screen.MealTime(s.mealType, s.dateIso, highlightLatest = itemAdded))
                            }
                            refreshToggle = !refreshToggle
                        },
                        autoFocusSearch = true
                    )
                }
                entry<Screen.EditMeal> { s ->
                    val editVm = remember(intakeRepo, s.mealId) { EditMealViewModel(mealRepo, foodRepo, s.mealId) }
                    EditMealScreen(
                        viewModel = editVm,
                        onBack = { backStack.removeLastOrNull(); refreshToggle = !refreshToggle },
                        onDeleted = {
                            backStack.apply {
                                removeLastOrNull()
                                removeLastOrNull()
                                add(Screen.MealTime(s.returnMealType, s.dateIso))
                            }
                            refreshToggle = !refreshToggle
                        }
                    )
                }
                entry<Screen.FoodManage> { s ->
                    FoodManageScreen(
                        viewModel = foodEditViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onOpenEdit = { id -> backStack.add(Screen.FoodEdit(id, s.dateIso)) }
                    )
                }
                entry<Screen.FoodEdit> { s ->
                    FoodEditScreen(
                        viewModel = foodEditViewModel,
                        foodId = s.foodId,
                        onBack = { foodEditViewModel.refresh(); backStack.removeLastOrNull(); refreshToggle = !refreshToggle },
                        onDeleted = { foodEditViewModel.refresh(); backStack.removeLastOrNull(); refreshToggle = !refreshToggle }
                    )
                }
                entry<Screen.Settings> {
                    SettingsScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onOpenMealsList = { backStack.add(Screen.MealsManage(currentDateIso())) },
                        onOpenWeightList = { backStack.add(Screen.WeightList(returnTo = Screen.ReturnTo.Settings)) },
                        onOpenFoodManage = { backStack.add(Screen.FoodManage(currentDateIso())) },
                        onOpenSourcePreferences = { backStack.add(Screen.SourcePreference) },
                        onRequestHealthPermissions = { HealthManager.requestPermissions() },
                        settingsRepo = settingsRepo
                    )
                }
                entry<Screen.SourcePreference> {
                    val viewModel = remember(settingsRepo) { SourcePreferenceViewModel(settingsRepo) }
                    SourcePreferenceScreen(
                        onBack = { backStack.removeLastOrNull() },
                        viewModel = viewModel
                    )
                }
                entry<Screen.MealsManage> { s ->
                    MealsListScreen(
                        viewModel = mealsListViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onOpenMeal = { id -> backStack.add(Screen.EditMealFromList(id, s.dateIso)) }
                    )
                }
                entry<Screen.EditMealFromList> { s ->
                    val editVm = remember(intakeRepo, s.mealId) { EditMealViewModel(mealRepo, foodRepo, s.mealId) }
                    EditMealScreen(
                        viewModel = editVm,
                        onBack = { mealsListViewModel.refresh(); backStack.removeLastOrNull(); refreshToggle = !refreshToggle },
                        onDeleted = { mealsListViewModel.refresh(); backStack.removeLastOrNull(); refreshToggle = !refreshToggle }
                    )
                }
                entry<Screen.ExerciseList>(
                    metadata = NavDisplay.transitionSpec {
                        (scaleIn(
                            initialScale = 0.8f,
                            animationSpec = tween(200)
                        ) + fadeIn(
                            animationSpec = tween(200)
                        )) togetherWith fadeOut(animationSpec = tween(200))
                    } + NavDisplay.popTransitionSpec {
                        fadeIn(animationSpec = tween(200)) togetherWith
                            (scaleOut(
                                targetScale = 0.8f,
                                animationSpec = tween(200)
                            ) + fadeOut(animationSpec = tween(200)))
                    }
                ) { s ->
                    val vm = remember(exerciseRepo, s.dateIso, refreshToggle) {
                        ExerciseListViewModel(exerciseRepo, s.dateIso)
                    }
                    ExerciseListScreen(
                        viewModel = vm,
                        onBack = { backStack.removeLastOrNull() },
                        onEnableHealth = { HealthManager.requestPermissions() }
                    )
                }
                entry<Screen.WeightList> { s ->
                    val vm = remember(weightRepo, s.openAdd, refreshToggle) {
                        WeightListViewModel(weightRepo)
                    }
                    val onBackAction: () -> Unit = when (s.returnTo) {
                        is Screen.ReturnTo.Settings -> {
                            { backStack.removeLastOrNull() }
                        }
                        is Screen.ReturnTo.Main -> {
                            { backStack.removeLastOrNull(); refreshToggle = !refreshToggle }
                        }
                    }
                    WeightListScreen(
                        viewModel = vm,
                        onBack = onBackAction,
                        openAdd = s.openAdd
                    )
                }
                entry<Screen.Statistics> {
                    val vm = remember(intakeRepo, exerciseRepo, settingsRepo) { StatisticsViewModel(intakeRepo, exerciseRepo, settingsRepo) }
                    StatisticsScreen(
                        viewModel = vm,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
            }
        )
    }
}

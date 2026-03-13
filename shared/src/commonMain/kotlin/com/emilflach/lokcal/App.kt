package com.emilflach.lokcal

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.data.FoodRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.MealRepository
import com.emilflach.lokcal.data.SettingsRepository
import com.emilflach.lokcal.data.SqlDriverFactory
import com.emilflach.lokcal.data.WeightRepository
import com.emilflach.lokcal.data.createDatabase
import com.emilflach.lokcal.theme.AppTheme
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.screens.EditMealScreen
import com.emilflach.lokcal.ui.screens.ExerciseListScreen
import com.emilflach.lokcal.ui.screens.FoodEditScreen
import com.emilflach.lokcal.ui.screens.FoodManageScreen
import com.emilflach.lokcal.ui.screens.IntakeScreen
import com.emilflach.lokcal.ui.screens.MainScreen
import com.emilflach.lokcal.ui.screens.MealTimeScreen
import com.emilflach.lokcal.ui.screens.MealsListScreen
import com.emilflach.lokcal.ui.screens.SettingsScreen
import com.emilflach.lokcal.ui.screens.StatisticsScreen
import com.emilflach.lokcal.ui.screens.WeightListScreen
import com.emilflach.lokcal.util.currentDateIso
import com.emilflach.lokcal.viewmodel.EditMealViewModel
import com.emilflach.lokcal.viewmodel.ExerciseListViewModel
import com.emilflach.lokcal.viewmodel.FoodEditViewModel
import com.emilflach.lokcal.viewmodel.IntakeViewModel
import com.emilflach.lokcal.viewmodel.MainViewModel
import com.emilflach.lokcal.viewmodel.MealTimeViewModel
import com.emilflach.lokcal.viewmodel.MealsListViewModel
import com.emilflach.lokcal.viewmodel.StatisticsViewModel
import com.emilflach.lokcal.viewmodel.WeightListViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
private sealed interface Screen : NavKey {
    @Serializable
    data class Main(val dateIso: String) : Screen
    @Serializable
    data class MealTime(val mealType: String, val dateIso: String, val highlightLatest: Boolean = false) : Screen
    @Serializable
    data class Intake(val mealType: String, val dateIso: String) : Screen
    @Serializable
    data class EditMeal(val mealId: Long, val returnMealType: String, val dateIso: String) : Screen
    // Settings flow
    @Serializable
    data object Settings : Screen
    @Serializable
    data class MealsList(val dateIso: String) : Screen
    @Serializable
    data class FoodManage(val dateIso: String) : Screen
    @Serializable
    data class FoodEdit(val foodId: Long?, val dateIso: String) : Screen
    @Serializable
    data class EditMealFromList(val mealId: Long, val dateIso: String) : Screen
    // Exercise flow
    @Serializable
    data class ExerciseList(val dateIso: String) : Screen
    // Weight flow
    @Serializable
    sealed interface ReturnTo {
        @Serializable
        data object Settings : ReturnTo
        @Serializable
        data class Main(val dateIso: String) : ReturnTo
    }
    @Serializable
    data class WeightList(val openAdd: Boolean = false, val returnTo: ReturnTo) : Screen
    // Stats flow
    @Serializable
    data object Statistics : Screen
}

private val navigationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Screen.Main::class, Screen.Main.serializer())
            subclass(Screen.MealTime::class, Screen.MealTime.serializer())
            subclass(Screen.Intake::class, Screen.Intake.serializer())
            subclass(Screen.EditMeal::class, Screen.EditMeal.serializer())
            subclass(Screen.Settings::class, Screen.Settings.serializer())
            subclass(Screen.MealsList::class, Screen.MealsList.serializer())
            subclass(Screen.FoodManage::class, Screen.FoodManage.serializer())
            subclass(Screen.FoodEdit::class, Screen.FoodEdit.serializer())
            subclass(Screen.EditMealFromList::class, Screen.EditMealFromList.serializer())
            subclass(Screen.ExerciseList::class, Screen.ExerciseList.serializer())
            subclass(Screen.WeightList::class, Screen.WeightList.serializer())
            subclass(Screen.Statistics::class, Screen.Statistics.serializer())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun App(sqlDriverFactory: SqlDriverFactory) = AppTheme {
    // Database and repositories

    var seedingProgress by remember { mutableStateOf<Float?>(null) }
    val database by produceState<Database?>(null) {
        value = createDatabase(sqlDriverFactory, onProgress = { seedingProgress = it })
    }

    val colors = LocalRecipesColors.current

    if (database == null) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.backgroundPage
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val progress = seedingProgress
                if (progress != null) {
                    CircularProgressIndicator(
                        progress = { progress },
                        color = colors.backgroundBrand,
                        trackColor = colors.backgroundBrand.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Seeding data... ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.foregroundDefault
                    )
                } else {
                    CircularProgressIndicator(color = colors.backgroundBrand)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Initializing...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.foregroundDefault
                    )
                }
            }
        }
        return@AppTheme
    }

    val foodRepo = remember(database) { FoodRepository(database!!) }
    val intakeRepo = remember(database) { IntakeRepository(database!!) }
    val mealRepo = remember(database) { MealRepository(database!!) }
    val exerciseRepo = remember(database) { ExerciseRepository(database!!) }
    val weightRepo = remember(database) { WeightRepository(database!!) }
    val settingsRepo = remember(database) { SettingsRepository(database!!) }

    var refreshToggle by remember { mutableStateOf(false) }

    val mainViewModel = remember(intakeRepo, exerciseRepo, weightRepo, settingsRepo) {
        MainViewModel(intakeRepo, exerciseRepo, weightRepo, settingsRepo, currentDateIso())
    }

    val backStack = rememberNavBackStack(navigationConfig, Screen.Main(currentDateIso()))

    // Store ViewModels that need to be preserved across navigation
    val mealsListViewModel = remember(intakeRepo) { MealsListViewModel(intakeRepo) }
    val foodEditViewModel = remember(foodRepo, intakeRepo) { FoodEditViewModel(foodRepo, intakeRepo) }

    Surface(
        color = colors.backgroundPage,
        contentColor = colors.foregroundDefault
    ) {

        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            // Default fade transition for most screens
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
            },
            popTransitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
            },
            entryProvider = entryProvider {
                entry<Screen.Main> { s ->
                    // Update VM date when entering Main screen
                    LaunchedEffect(s.dateIso, refreshToggle) {
                        mainViewModel.loadFor(kotlinx.datetime.LocalDate.parse(s.dateIso))
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
                        // Scale + fade in when opening
                        (scaleIn(
                            initialScale = 0.8f,
                            animationSpec = tween(200)
                        ) + fadeIn(
                            animationSpec = tween(200)
                        )) togetherWith fadeOut(animationSpec = tween(200))
                    } + NavDisplay.popTransitionSpec {
                        // Scale + fade out when going back
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
                        onAdd = { meal ->
                            backStack.add(Screen.Intake(meal, s.dateIso))
                        },
                        shouldHighlightLatest = s.highlightLatest
                    )
                }
                entry<Screen.Intake>(
                    metadata = NavDisplay.transitionSpec {
                        // No transition - keep old screen visible
                        EnterTransition.None togetherWith ExitTransition.KeepUntilTransitionsFinished
                    } + NavDisplay.popTransitionSpec {
                        // No transition when going back
                        EnterTransition.None togetherWith ExitTransition.None
                    }
                ) { s ->
                    val intakeVm = remember(foodRepo, intakeRepo, s.mealType, s.dateIso) { IntakeViewModel(foodRepo, intakeRepo, s.mealType, s.dateIso) }
                    IntakeScreen(
                        viewModel = intakeVm,
                        onDone = { itemAdded ->
                            // Navigate back to meal time and refresh
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
                    val editVm = remember(intakeRepo, s.mealId) { EditMealViewModel(mealRepo, foodRepo, intakeRepo, s.mealId) }
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
                        onBack = { backStack.removeLastOrNull() },
                        onSaved = { foodEditViewModel.refresh(); backStack.removeLastOrNull(); refreshToggle = !refreshToggle },
                        onDeleted = { foodEditViewModel.refresh(); backStack.removeLastOrNull(); refreshToggle = !refreshToggle }
                    )
                }
                entry<Screen.Settings> {
                    SettingsScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onOpenMealsList = { backStack.add(Screen.MealsList(currentDateIso())) },
                        onOpenWeightList = { backStack.add(Screen.WeightList(returnTo = Screen.ReturnTo.Settings)) },
                        onOpenFoodManage = { backStack.add(Screen.FoodManage(currentDateIso())) },
                        settingsRepo = settingsRepo
                    )
                }
                entry<Screen.MealsList> { s ->
                    MealsListScreen(
                        viewModel = mealsListViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onOpenMeal = { id -> backStack.add(Screen.EditMealFromList(id, s.dateIso)) }
                    )
                }
                entry<Screen.EditMealFromList> { s ->
                    val editVm = remember(intakeRepo, s.mealId) { EditMealViewModel(mealRepo, foodRepo, intakeRepo, s.mealId) }
                    EditMealScreen(
                        viewModel = editVm,
                        onBack = { mealsListViewModel.refresh(); backStack.removeLastOrNull(); refreshToggle = !refreshToggle },
                        onDeleted = { mealsListViewModel.refresh(); backStack.removeLastOrNull(); refreshToggle = !refreshToggle }
                    )
                }
                entry<Screen.ExerciseList>(
                    metadata = NavDisplay.transitionSpec {
                        // Scale + fade in when opening
                        (scaleIn(
                            initialScale = 0.8f,
                            animationSpec = tween(200)
                        ) + fadeIn(
                            animationSpec = tween(200)
                        )) togetherWith fadeOut(animationSpec = tween(200))
                    } + NavDisplay.popTransitionSpec {
                        // Scale + fade out when going back
                        fadeIn(animationSpec = tween(200)) togetherWith
                            (scaleOut(
                                targetScale = 0.8f,
                                animationSpec = tween(200)
                            ) + fadeOut(animationSpec = tween(200)))
                    }
                ) { s ->
                    val vm = remember(exerciseRepo, s.dateIso, refreshToggle) {
                        ExerciseListViewModel(
                            exerciseRepo,
                            s.dateIso
                        )
                    }
                    ExerciseListScreen(
                        viewModel = vm,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<Screen.WeightList> { s ->
                    val vm = remember(weightRepo, s.openAdd, refreshToggle) {
                        WeightListViewModel(
                            weightRepo
                        )
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

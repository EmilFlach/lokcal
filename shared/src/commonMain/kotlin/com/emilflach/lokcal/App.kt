package com.emilflach.lokcal

import androidx.compose.animation.AnimatedContent
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

private sealed class Screen {
    data class Main(val dateIso: String) : Screen()
    data class MealTime(val mealType: String, val dateIso: String, val highlightLatest: Boolean = false) : Screen()
    data class Intake(val mealType: String, val dateIso: String) : Screen()
    data class EditMeal(val mealId: Long, val returnMealType: String, val dateIso: String) : Screen()
    // Settings flow
    data object Settings : Screen()
    data class MealsList(val viewModel: MealsListViewModel) : Screen()
    data class FoodManage(val viewModel: FoodEditViewModel) : Screen()
    data class FoodEdit(val foodId: Long?, val viewModel: FoodEditViewModel) : Screen()
    data class EditMealFromList(val mealId: Long, val viewModel: MealsListViewModel) : Screen()
    // Exercise flow
    data class ExerciseList(val dateIso: String) : Screen()
    // Weight flow
    sealed class ReturnTo {
        data object Settings : ReturnTo()
        data class Main(val dateIso: String) : ReturnTo()
    }
    data class WeightList(val openAdd: Boolean = false, val returnTo: ReturnTo) : Screen()
    // Stats flow
    data object Statistics : Screen()
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

    var screen by remember { mutableStateOf<Screen>(Screen.Main(currentDateIso())) }
    var refreshToggle by remember { mutableStateOf(false) }

    val mainViewModel = remember(intakeRepo, exerciseRepo, weightRepo, settingsRepo) {
        MainViewModel(intakeRepo, exerciseRepo, weightRepo, settingsRepo, currentDateIso())
    }

    Surface(
        color = colors.backgroundPage,
        contentColor = colors.foregroundDefault
    ) {

        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                when {
                    initialState is Screen.MealTime && targetState is Screen.Intake ||
                            initialState is Screen.Intake && targetState is Screen.MealTime ->
                        EnterTransition.None togetherWith ExitTransition.None

                    targetState is Screen.MealTime || targetState is Screen.ExerciseList ->
                        (scaleIn(
                            initialScale = 0.8f,
                            animationSpec = tween(200)
                        ) + fadeIn(
                            animationSpec = tween(200)
                        )).togetherWith(
                            fadeOut(animationSpec = tween(200))
                        )

                    initialState is Screen.MealTime && targetState is Screen.Main ||
                            initialState is Screen.ExerciseList && targetState is Screen.Main ->
                        fadeIn(animationSpec = tween(200))
                            .togetherWith(
                                scaleOut(
                                    targetScale = 0.8f,
                                    animationSpec = tween(200)
                                ) + fadeOut(animationSpec = tween(200))
                            )

                    else ->
                        fadeIn(animationSpec = tween(200))
                            .togetherWith(fadeOut(animationSpec = tween(200)))
                }
            }
        ) { s ->
            when (s) {
                is Screen.Main -> {
                    // Update VM date when entering Main screen
                    LaunchedEffect(s.dateIso, refreshToggle) {
                        mainViewModel.loadFor(kotlinx.datetime.LocalDate.parse(s.dateIso))
                    }
                    MainScreen(
                        viewModel = mainViewModel,
                        onOpenMeal = { meal, dateIso -> screen = Screen.MealTime(meal, dateIso) },
                        onOpenExercise = { dateIso -> screen = Screen.ExerciseList(dateIso) },
                        onOpenSettings = { screen = Screen.Settings },
                        onOpenWeightToday = { screen = Screen.WeightList(openAdd = true, returnTo = Screen.ReturnTo.Main(s.dateIso)) },
                        onOpenWeightList = { screen = Screen.WeightList(openAdd = false, returnTo = Screen.ReturnTo.Main(s.dateIso)) },
                        onOpenStatistics = { screen = Screen.Statistics }
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
                    val editVm = remember(intakeRepo, s.mealId) { EditMealViewModel(mealRepo, foodRepo, intakeRepo, s.mealId) }
                    EditMealScreen(
                        viewModel = editVm,
                        onBack = { screen = Screen.MealTime(s.returnMealType, s.dateIso); refreshToggle = !refreshToggle },
                        onDeleted = { screen = Screen.MealTime(s.returnMealType, s.dateIso); refreshToggle = !refreshToggle }
                    )
                }
                is Screen.FoodManage -> {
                    FoodManageScreen(
                        viewModel = s.viewModel,
                        onBack = { screen = Screen.Settings },
                        onOpenEdit = { id -> screen = Screen.FoodEdit(id, s.viewModel) }
                    )
                }
                is Screen.FoodEdit -> {
                    FoodEditScreen(
                        viewModel = s.viewModel,
                        foodId = s.foodId,
                        onBack = { screen = Screen.FoodManage(s.viewModel) },
                        onSaved = { s.viewModel.refresh(); screen = Screen.FoodManage(s.viewModel); refreshToggle = !refreshToggle },
                        onDeleted = { s.viewModel.refresh(); screen = Screen.FoodManage(s.viewModel); refreshToggle = !refreshToggle }
                    )
                }
                Screen.Settings -> {
                    SettingsScreen(
                        onBack = { screen = Screen.Main(currentDateIso()) },
                        onOpenMealsList = { screen = Screen.MealsList(MealsListViewModel(intakeRepo)) },
                        onOpenWeightList = { screen = Screen.WeightList(returnTo = Screen.ReturnTo.Settings) },
                        onOpenFoodManage = { screen = Screen.FoodManage(FoodEditViewModel(foodRepo, intakeRepo)) },
                        settingsRepo = settingsRepo
                    )
                }
                is Screen.MealsList -> {
                    MealsListScreen(
                        viewModel = s.viewModel,
                        onBack = { screen = Screen.Settings },
                        onOpenMeal = { id -> screen = Screen.EditMealFromList(id, s.viewModel) }
                    )
                }
                is Screen.EditMealFromList -> {
                    val editVm = remember(intakeRepo, s.mealId) { EditMealViewModel(mealRepo, foodRepo, intakeRepo, s.mealId) }
                    EditMealScreen(
                        viewModel = editVm,
                        onBack = { s.viewModel.refresh(); screen = Screen.MealsList(s.viewModel); refreshToggle = !refreshToggle },
                        onDeleted = { s.viewModel.refresh(); screen = Screen.MealsList(s.viewModel); refreshToggle = !refreshToggle }
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
                        onBack = { screen = Screen.Main(s.dateIso) }
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
                Screen.Statistics -> {
                    val vm = remember(intakeRepo, exerciseRepo, settingsRepo) { StatisticsViewModel(intakeRepo, exerciseRepo, settingsRepo) }
                    StatisticsScreen(
                        viewModel = vm,
                        onBack = { screen = Screen.Main(currentDateIso()) }
                    )
                }
            }
        }
    }
}

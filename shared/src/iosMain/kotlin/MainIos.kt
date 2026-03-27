import androidx.compose.runtime.*
import androidx.compose.ui.window.ComposeUIViewController
import com.emilflach.lokcal.App
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.data.*
import com.emilflach.lokcal.screens.initializeRepositories
import com.emilflach.lokcal.theme.AppTheme
import com.emilflach.lokcal.ui.screens.AppLoadingScreen
import com.emilflach.lokcal.util.currentDateIso
import com.emilflach.lokcal.viewmodel.FoodEditViewModel
import com.emilflach.lokcal.viewmodel.MainViewModel
import com.emilflach.lokcal.viewmodel.MealsListViewModel
import platform.UIKit.UIViewController

// Legacy fullscreen app (kept for backward compatibility)
fun MainViewController(): UIViewController = ComposeUIViewController { App(sqlDriverFactory = SqlDriverFactory()) }

// Used by native navigation
fun initApp(onReady: () -> Unit) = ComposeUIViewController {
    AppTheme {
        var seedingProgress by remember { mutableStateOf<Float?>(null) }
        val database by produceState<Database?>(null) {
            value = createDatabase(SqlDriverFactory(), onProgress = { seedingProgress = it })
        }

        if (database == null) {
            AppLoadingScreen(seedingProgress)
            return@AppTheme
        }

        LaunchedEffect(database) {
            database?.let { db ->
                val foodRepo = FoodRepository(db)
                val intakeRepo = IntakeRepository(db)
                val mealRepo = MealRepository(db)
                val exerciseRepo = ExerciseRepository(db)
                val weightRepo = WeightRepository(db)
                val settingsRepo = SettingsRepository(db)

                val mainViewModel = MainViewModel(intakeRepo, exerciseRepo, weightRepo, settingsRepo, currentDateIso())
                val mealsListViewModel = MealsListViewModel(intakeRepo, mealRepo)
                val foodEditViewModel = FoodEditViewModel(foodRepo, intakeRepo, mealRepo)

                initializeRepositories(
                    foodRepo = foodRepo,
                    intakeRepo = intakeRepo,
                    mealRepo = mealRepo,
                    exerciseRepo = exerciseRepo,
                    weightRepo = weightRepo,
                    settingsRepo = settingsRepo,
                    mainViewModel = mainViewModel,
                    mealsListViewModel = mealsListViewModel,
                    foodEditViewModel = foodEditViewModel
                )

                onReady()
            }
        }
    }
}

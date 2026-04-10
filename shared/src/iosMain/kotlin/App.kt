
import androidx.compose.runtime.*
import androidx.compose.ui.window.ComposeUIViewController
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.data.*
import com.emilflach.lokcal.screens.initializeRepositories
import com.emilflach.lokcal.theme.AppTheme
import com.emilflach.lokcal.ui.screens.AppLoadingScreen
import com.emilflach.lokcal.util.currentDateIso
import com.emilflach.lokcal.viewmodel.FoodEditViewModel
import com.emilflach.lokcal.viewmodel.MainViewModel
import com.emilflach.lokcal.viewmodel.MealsListViewModel

// Called from Swift: AppKt.initializeApp(onReady:) `iosApp/iosApp/MainView.swift`
@Suppress("unused")
fun initializeApp(onReady: () -> Unit) = ComposeUIViewController {
    AppTheme {
        var seedingProgress by remember { mutableStateOf<Float?>(null) }
        val database by produceState<Database?>(null) {
            value = createDatabase(SqlDriverFactory(), onProgress = { seedingProgress = it })
        }

        if (database == null) {
            AppLoadingScreen(seedingProgress)
            return@AppTheme
        }

        val db = database!!
        val foodRepo = remember(db) { FoodRepository(db) }
        val intakeRepo = remember(db) { IntakeRepository(db) }
        val mealRepo = remember(db) { MealRepository(db) }
        val exerciseRepo = remember(db) { ExerciseRepository(db) }
        val weightRepo = remember(db) { WeightRepository(db) }
        val settingsRepo = remember(db) { SettingsRepository(db) }
        val imageCacheRepo = remember(db) { ImageCacheRepository(db) }

        val mainViewModel = remember(intakeRepo, exerciseRepo, weightRepo, settingsRepo) {
            MainViewModel(intakeRepo, exerciseRepo, weightRepo, settingsRepo, currentDateIso())
        }
        val mealsListViewModel = remember(intakeRepo, mealRepo, imageCacheRepo) {
            MealsListViewModel(intakeRepo, mealRepo, imageCacheRepo)
        }
        val foodEditViewModel = remember(foodRepo, intakeRepo, mealRepo, imageCacheRepo) {
            FoodEditViewModel(foodRepo, intakeRepo, mealRepo, imageCacheRepo)
        }

        LaunchedEffect(db) {
            initializeRepositories(
                foodRepo = foodRepo,
                intakeRepo = intakeRepo,
                mealRepo = mealRepo,
                exerciseRepo = exerciseRepo,
                weightRepo = weightRepo,
                settingsRepo = settingsRepo,
                imageCacheRepo = imageCacheRepo,
                mainViewModel = mainViewModel,
                mealsListViewModel = mealsListViewModel,
                foodEditViewModel = foodEditViewModel
            )
            onReady()
        }
    }
}

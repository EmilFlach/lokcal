import SwiftUI
import Shared

// MARK: - Navigation Path Items

enum NavigationDestination: Hashable {
    case mealTime(mealType: String, dateIso: String, highlightLatest: Bool, refreshId: Int = 0)
    case intake(mealType: String, dateIso: String)
    case editMeal(mealId: Int64, returnMealType: String, dateIso: String)
    case settings
    case sourcePreference
    case mealsManage(dateIso: String, refreshId: Int = 0)
    case foodManage(dateIso: String, refreshId: Int = 0)
    case foodEdit(foodId: Int64?, dateIso: String)
    case editMealFromList(mealId: Int64, dateIso: String)
    case exerciseList(dateIso: String, refreshId: Int = 0)
    case weightList(openAdd: Bool, returnToSettings: Bool, dateIso: String?, refreshId: Int = 0)
    case statistics
    case statisticsDemo
}

// MARK: - Native Navigation Container

struct NativeNavigationView: View {
    @State private var isAppReady = false
    @State private var showOnboarding = false
    @State private var navigationPath = NavigationPath()
    @State private var refreshKey = 0

    var body: some View {
        Group {
            if isAppReady {
                if showOnboarding {
                    OnboardingView(onGetStarted: {
                        showOnboarding = false
                    })
                } else {
                    NavigationStack(path: $navigationPath) {
                        MainScreen(navigationPath: $navigationPath, refreshKey: $refreshKey)
                            .navigationDestination(for: NavigationDestination.self) { destination in
                                destinationView(for: destination)
                            }
                    }
                }
            } else {
                LoadingView(onReady: { needsOnboarding in
                    showOnboarding = needsOnboarding
                    isAppReady = true
                })
            }
        }
    }

    @ViewBuilder
    private func destinationView(for destination: NavigationDestination) -> some View {
        switch destination {
        case .mealTime(let mealType, let dateIso, let highlightLatest, let refreshId):
            MealTimeScreen(mealType: mealType, dateIso: dateIso, highlightLatest: highlightLatest, refreshId: refreshId, navigationPath: $navigationPath, refreshKey: $refreshKey)
        case .intake(let mealType, let dateIso):
            IntakeScreen(mealType: mealType, dateIso: dateIso, navigationPath: $navigationPath, refreshKey: $refreshKey)
        case .editMeal(let mealId, let returnMealType, let dateIso):
            EditMealScreen(mealId: mealId, returnMealType: returnMealType, dateIso: dateIso, navigationPath: $navigationPath, refreshKey: $refreshKey)
        case .settings:
            SettingsScreen(navigationPath: $navigationPath)
        case .sourcePreference:
            SourcePreferenceScreen(navigationPath: $navigationPath)
        case .mealsManage(let dateIso, _):
            MealsListScreen(dateIso: dateIso, navigationPath: $navigationPath)
        case .editMealFromList(let mealId, _):
            EditMealFromListScreen(mealId: mealId, navigationPath: $navigationPath, refreshKey: $refreshKey)
        case .foodManage(let dateIso, _):
            FoodManageScreen(dateIso: dateIso, navigationPath: $navigationPath)
        case .foodEdit(let foodId, _):
            FoodEditScreen(foodId: foodId, navigationPath: $navigationPath, refreshKey: $refreshKey)
        case .exerciseList(let dateIso, _):
            ExerciseListScreen(dateIso: dateIso, navigationPath: $navigationPath, refreshKey: refreshKey)
        case .weightList(let openAdd, let returnToSettings, _, _):
            WeightListScreen(openAdd: openAdd, returnToSettings: returnToSettings, navigationPath: $navigationPath, refreshKey: $refreshKey)
        case .statistics:
            StatisticsScreen(navigationPath: $navigationPath)
        case .statisticsDemo:
            StatisticsDemoScreen(navigationPath: $navigationPath)
        }
    }
}

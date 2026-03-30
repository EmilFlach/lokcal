import SwiftUI
import Shared

@main
struct LokcalApp: App {
    var body: some Scene {
        WindowGroup {
            NativeNavigationView()
                .ignoresSafeArea(.all)
        }
    }
}

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
} 

// MARK: - Native Navigation Container

struct NativeNavigationView: View {
    @State private var isAppReady = false
    @State private var navigationPath = NavigationPath()
    @State private var refreshKey = 0
    @State private var mainScreenTitle = ""

    var body: some View {
        Group {
            if isAppReady {
                NavigationStack(path: $navigationPath) {
                    MainView(
                        navigationPath: $navigationPath,
                        refreshKey: refreshKey
                    )
                    .ignoresSafeArea(.all)
                    .toolbarBackground(.hidden, for: .navigationBar)
                    .navigationTitle(mainScreenTitle)
                    .navigationBarTitleDisplayMode(.inline)
                    .onAppear {
                        ScreenFactoriesKt.getGlobalMainViewModel().refresh()
                        mainScreenTitle = getFormattedDate()
                    }
                    .onReceive(Timer.publish(every: 0.1, on: .main, in: .common).autoconnect()) { _ in
                        let newTitle = getFormattedDate()
                        if newTitle != mainScreenTitle {
                            mainScreenTitle = newTitle
                        }
                    }
                    .toolbar {
                        ToolbarItem(placement: .navigationBarLeading) {
                            Button(action: {
                                ScreenFactoriesKt.getGlobalMainViewModel().setToCurrentDate()
                            }) {
                                Image(systemName: "calendar")
                            }
                        }
                        ToolbarItemGroup(placement: .primaryAction) {
                            Button(action: {
                                navigationPath.append(NavigationDestination.statistics)
                            }) {
                                Image(systemName: "chart.bar")
                            }
                            Button(action: {
                                navigationPath.append(NavigationDestination.weightList(openAdd: false, returnToSettings: false, dateIso: nil, refreshId: refreshKey))
                            }) {
                                Image(systemName: "scalemass")
                            }
                            Button(action: {
                                navigationPath.append(NavigationDestination.settings)
                            }) {
                                Image(systemName: "gearshape")
                            }
                        }
                    }
                    .navigationDestination(for: NavigationDestination.self) { destination in
                        destinationView(for: destination)
                    }
                }
            } else {
                LoadingView(onReady: {
                    isAppReady = true
                })
            }
        }
    }

    @ViewBuilder
    private func destinationView(for destination: NavigationDestination) -> some View {
        Group {
            switch destination {
        case .mealTime(let mealType, let dateIso, let highlightLatest, let refreshId):
            MealTimeView(
                mealType: mealType,
                dateIso: dateIso,
                shouldHighlightLatest: highlightLatest,
                navigationPath: $navigationPath,
                refreshKey: refreshKey
            )
            .ignoresSafeArea(edges: .all)
            .navigationTitle(mealType.capitalized)
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.hidden, for: .navigationBar)
            .toolbarBackground(.hidden, for: .tabBar)
            .id("\(mealType)-\(dateIso)-\(refreshId)")
            .onAppear {
                let vm = ScreenFactoriesKt.getMealTimeViewModel(mealType: mealType, dateIso: dateIso, refreshKey: Int32(refreshKey))
                vm.loadForSelectedDate(shouldHighlightLatest: false)
            }
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        let vm = ScreenFactoriesKt.getMealTimeViewModel(mealType: mealType, dateIso: dateIso, refreshKey: Int32(refreshKey))
                        vm.toggleLeftovers()
                    } label: {
                        Image(systemName: "bookmark")
                    }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        // For save meal, we need to show a dialog - this is handled in Compose
                        // We'll leave this as a visual placeholder for now since the dialog is in Compose
                    } label: {
                        Image(systemName: "square.and.arrow.down")
                    }
                }
            }
            .overlay(alignment: .bottom) {
                Button {
                    navigationPath.append(NavigationDestination.intake(mealType: mealType, dateIso: dateIso))
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 22))
                        Text("Add food")
                            .fontWeight(.semibold)
                    }
                    .foregroundStyle(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 14)
                    .background {
                        Capsule()
                            .fill(Color(red: 0xD9/255, green: 0x91/255, blue: 0x0D/255))
                            .overlay(
                                Capsule()
                                    .fill(
                                        LinearGradient(
                                            colors: [
                                                .white.opacity(0.2),
                                                .white.opacity(0.08),
                                                .clear
                                            ],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                    )
                            )
                    }
                    .overlay(
                        Capsule()
                            .stroke(.white.opacity(0.3), lineWidth: 0.5)
                    )
                }
                .shadow(color: Color(red: 0xD9/255, green: 0x91/255, blue: 0x0D/255).opacity(0.25), radius: 10, y: 5)
                .shadow(color: .black.opacity(0.18), radius: 7, y: 3)
                .padding(.bottom, 16)
            }

        case .intake(let mealType, let dateIso):
            IntakeSearchableView(
                mealType: mealType,
                dateIso: dateIso,
                navigationPath: $navigationPath,
                onDone: { itemAdded in
                    // Remove intake and mealTime screens, add new mealTime with highlight
                    if navigationPath.count >= 2 {
                        navigationPath.removeLast(2)
                    }
                    refreshKey += 1
                    navigationPath.append(NavigationDestination.mealTime(
                        mealType: mealType,
                        dateIso: dateIso,
                        highlightLatest: itemAdded,
                        refreshId: refreshKey
                    ))
                }
            )
            .ignoresSafeArea(edges: .all)
            .navigationTitle(mealType.capitalized)
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.hidden, for: .navigationBar)
            .toolbarBackground(.hidden, for: .tabBar)

        case .editMeal(let mealId, let returnMealType, let dateIso):
            EditMealView(
                mealId: mealId,
                navigationPath: $navigationPath,
                onDeleted: {
                    // Remove edit and mealTime, add new mealTime
                    if navigationPath.count >= 2 {
                        navigationPath.removeLast(2)
                    }
                    refreshKey += 1
                    navigationPath.append(NavigationDestination.mealTime(
                        mealType: returnMealType,
                        dateIso: dateIso,
                        highlightLatest: false,
                        refreshId: refreshKey
                    ))
                }
            )
            .ignoresSafeArea(.all)
            .navigationTitle("Edit Meal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button(role: .destructive) {
                        // TODO: Trigger delete from ViewModel
                        // For now, this will be handled by the Compose screen
                    } label: {
                        Image(systemName: "trash")
                    }
                }
            }

        case .settings:
            SettingsView(
                navigationPath: $navigationPath
            )
            .ignoresSafeArea(.all)
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)

        case .sourcePreference:
            SourcePreferenceView(
                navigationPath: $navigationPath
            )
            .ignoresSafeArea(.all)
            .navigationTitle("Search Sources")
            .navigationBarTitleDisplayMode(.inline)

        case .mealsManage(let dateIso, _):
            MealsListView(
                navigationPath: $navigationPath,
                dateIso: dateIso
            )
            .ignoresSafeArea(.all)
            .navigationTitle("Meals")
            .navigationBarTitleDisplayMode(.inline)

        case .editMealFromList(let mealId, _):
            EditMealFromListView(
                mealId: mealId,
                navigationPath: $navigationPath,
                onBack: {
                    navigationPath.removeLast()
                    refreshKey += 1
                },
                onDeleted: {
                    navigationPath.removeLast()
                    refreshKey += 1
                }
            )
            .ignoresSafeArea(.all)
            .navigationTitle("Edit Meal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button(role: .destructive) {
                        // TODO: Trigger delete from ViewModel
                        // For now, this will be handled by the Compose screen
                    } label: {
                        Image(systemName: "trash")
                    }
                }
            }

        case .foodManage(let dateIso, _):
            FoodManageView(
                navigationPath: $navigationPath,
                dateIso: dateIso
            )
            .ignoresSafeArea(.all)
            .navigationTitle("Foods")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        navigationPath.append(NavigationDestination.foodEdit(foodId: nil, dateIso: dateIso))
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }

        case .foodEdit(let foodId, _):
            let onSaved = {
                navigationPath.removeLast()
                refreshKey += 1
            }
            let onDeleted = {
                navigationPath.removeLast()
                refreshKey += 1
            }
            FoodEditView(
                foodId: foodId,
                navigationPath: $navigationPath,
                onSaved: onSaved,
                onDeleted: onDeleted
            )
            .ignoresSafeArea(.all)
            .navigationTitle(foodId == nil ? "Add Food" : "Edit Food")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if foodId != nil {
                    ToolbarItem(placement: .primaryAction) {
                        Button(role: .destructive) {
                            ScreenFactoriesKt.getGlobalFoodEditViewModel().delete {
                                onDeleted()
                            }
                        } label: {
                            Image(systemName: "trash")
                        }
                    }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        ScreenFactoriesKt.getGlobalFoodEditViewModel().save {
                            onSaved()
                        }
                    } label: {
                        Image(systemName: "checkmark")
                    }
                }
            }

        case .exerciseList(let dateIso, _):
            ExerciseListView(
                dateIso: dateIso,
                navigationPath: $navigationPath,
                refreshKey: refreshKey
            )
            .ignoresSafeArea(.all)
            .navigationTitle("Exercise")
            .navigationBarTitleDisplayMode(.inline)

        case .weightList(let openAdd, let returnToSettings, _, _):
            WeightListView(
                openAdd: openAdd,
                navigationPath: $navigationPath,
                onBack: {
                    navigationPath.removeLast()
                    if !returnToSettings {
                        refreshKey += 1
                    }
                },
                refreshKey: refreshKey
            )
            .ignoresSafeArea(.all)
            .navigationTitle("Weight")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        let vm = ScreenFactoriesKt.getWeightListViewModel()
                        vm.openAddDialog(open: true)
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }

        case .statistics:
            StatisticsView(
                navigationPath: $navigationPath
            )
            .ignoresSafeArea(.all)
            .navigationTitle("Statistics")
            .navigationBarTitleDisplayMode(.inline)
            }
        }
    }
}

// MARK: - Loading View

private struct LoadingView: UIViewControllerRepresentable {
    let onReady: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        MainIosKt.doInitApp(onReady: onReady)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - Main View

private struct MainView: UIViewControllerRepresentable {
    @Binding var navigationPath: NavigationPath
    let refreshKey: Int

    func makeUIViewController(context: Context) -> UIViewController {
        let currentDate = getCurrentDateIso()
        return ScreenFactoriesKt.MainViewController(
            dateIso: currentDate,
            onOpenMeal: { mealType, dateIso in
                navigationPath.append(NavigationDestination.mealTime(
                    mealType: mealType,
                    dateIso: dateIso,
                    highlightLatest: false,
                    refreshId: refreshKey
                ))
            },
            onOpenExercise: { dateIso in
                navigationPath.append(NavigationDestination.exerciseList(dateIso: dateIso))
            },
            onOpenSettings: {
                navigationPath.append(NavigationDestination.settings)
            },
            onOpenWeightToday: {
                let currentDate = getCurrentDateIso()
                navigationPath.append(NavigationDestination.weightList(
                    openAdd: true,
                    returnToSettings: false,
                    dateIso: currentDate
                ))
            },
            onOpenWeightList: {
                navigationPath.append(NavigationDestination.weightList(
                    openAdd: false,
                    returnToSettings: false,
                    dateIso: nil
                ))
            },
            onOpenStatistics: {
                navigationPath.append(NavigationDestination.statistics)
            },
            refreshKey: Int32(refreshKey)
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - MealTime View

private struct MealTimeView: UIViewControllerRepresentable {
    let mealType: String
    let dateIso: String
    let shouldHighlightLatest: Bool
    @Binding var navigationPath: NavigationPath
    let refreshKey: Int

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenFactoriesKt.MealTimeViewController(
            mealType: mealType,
            dateIso: dateIso,
            shouldHighlightLatest: shouldHighlightLatest,
            onBack: {
                navigationPath.removeLast()
            },
            onAdd: { mealType in
                navigationPath.append(NavigationDestination.intake(
                    mealType: mealType,
                    dateIso: dateIso
                ))
            },
            refreshKey: Int32(refreshKey)
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - Intake View

private struct IntakeSearchableView: View {
    let mealType: String
    let dateIso: String
    @Binding var navigationPath: NavigationPath
    let onDone: (Bool) -> Void
    @State private var searchText = ""
    @State private var isSearchPresented = false
    @State private var keyboardHeight: CGFloat = 0

    var body: some View {
        IntakeView(
            mealType: mealType,
            dateIso: dateIso,
            navigationPath: $navigationPath,
            onDone: onDone,
            searchQuery: searchText
        )
        .searchable(text: $searchText, isPresented: $isSearchPresented, placement: .toolbar, prompt: "Search foods and meals")
        .autocorrectionDisabled()
        .textInputAutocapitalization(.never)
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now()) {
                isSearchPresented = true
            }
        }
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                Button {
                    let vm = ScreenFactoriesKt.getIntakeViewModel(mealType: mealType, dateIso: dateIso)
                    vm.setShowScanner(show: true)
                } label: {
                    Label("Scan Barcode", systemImage: "barcode.viewfinder")
                }
            }
        }
    }
}

private struct IntakeView: UIViewControllerRepresentable {
    let mealType: String
    let dateIso: String
    @Binding var navigationPath: NavigationPath
    let onDone: (Bool) -> Void
    let searchQuery: String

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenFactoriesKt.IntakeViewController(
            mealType: mealType,
            dateIso: dateIso,
            onDone: { itemAdded in
                onDone(itemAdded.boolValue)
            },
            autoFocusSearch: false, // Don't auto-focus since we're using native search
            searchQuery: searchQuery
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Update ViewModel when search query changes
        ScreenFactoriesKt.getIntakeViewModel(mealType: mealType, dateIso: dateIso).setQuery(value: searchQuery)
    }
}

// MARK: - EditMeal View

private struct EditMealView: UIViewControllerRepresentable {
    let mealId: Int64
    @Binding var navigationPath: NavigationPath
    let onDeleted: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenFactoriesKt.EditMealViewController(
            mealId: mealId,
            onBack: {
                navigationPath.removeLast()
            },
            onDeleted: onDeleted
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - Settings View

private struct SettingsView: UIViewControllerRepresentable {
    @Binding var navigationPath: NavigationPath

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenFactoriesKt.SettingsViewController(
            onBack: {
                navigationPath.removeLast()
            },
            onOpenMealsList: {
                let currentDate = getCurrentDateIso()
                navigationPath.append(NavigationDestination.mealsManage(dateIso: currentDate))
            },
            onOpenWeightList: {
                navigationPath.append(NavigationDestination.weightList(
                    openAdd: false,
                    returnToSettings: true,
                    dateIso: nil
                ))
            },
            onOpenFoodManage: {
                let currentDate = getCurrentDateIso()
                navigationPath.append(NavigationDestination.foodManage(dateIso: currentDate))
            },
            onOpenSourcePreferences: {
                navigationPath.append(NavigationDestination.sourcePreference)
            }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - SourcePreference View

private struct SourcePreferenceView: UIViewControllerRepresentable {
    @Binding var navigationPath: NavigationPath

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenFactoriesKt.SourcePreferenceViewController(
            onBack: {
                navigationPath.removeLast()
            }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - MealsList View

private struct MealsListView: UIViewControllerRepresentable {
    @Binding var navigationPath: NavigationPath
    let dateIso: String

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenFactoriesKt.MealsListViewController(
            onBack: {
                navigationPath.removeLast()
            },
            onOpenMeal: { mealId in
                navigationPath.append(NavigationDestination.editMealFromList(
                    mealId: mealId.int64Value,
                    dateIso: dateIso
                ))
            }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - EditMealFromList View

private struct EditMealFromListView: UIViewControllerRepresentable {
    let mealId: Int64
    @Binding var navigationPath: NavigationPath
    let onBack: () -> Void
    let onDeleted: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenFactoriesKt.EditMealFromListViewController(
            mealId: mealId,
            onBack: onBack,
            onDeleted: onDeleted
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - FoodManage View

private struct FoodManageView: UIViewControllerRepresentable {
    @Binding var navigationPath: NavigationPath
    let dateIso: String

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenFactoriesKt.FoodManageViewController(
            onBack: {
                navigationPath.removeLast()
            },
            onOpenEdit: { foodId in
                navigationPath.append(NavigationDestination.foodEdit(
                    foodId: foodId?.int64Value,
                    dateIso: dateIso
                ))
            }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - FoodEdit View

private struct FoodEditView: UIViewControllerRepresentable {
    let foodId: Int64?
    @Binding var navigationPath: NavigationPath
    let onSaved: () -> Void
    let onDeleted: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        let kotlinFoodId: KotlinLong? = foodId.map { KotlinLong(longLong: $0) }
        return ScreenFactoriesKt.FoodEditViewController(
            foodId: kotlinFoodId,
            onBack: {
                navigationPath.removeLast()
            },
            onSaved: onSaved,
            onDeleted: onDeleted
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - ExerciseList View

private struct ExerciseListView: UIViewControllerRepresentable {
    let dateIso: String
    @Binding var navigationPath: NavigationPath
    let refreshKey: Int

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenFactoriesKt.ExerciseListViewController(
            dateIso: dateIso,
            onBack: {
                navigationPath.removeLast()
            },
            refreshKey: Int32(refreshKey)
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - WeightList View

private struct WeightListView: UIViewControllerRepresentable {
    let openAdd: Bool
    @Binding var navigationPath: NavigationPath
    let onBack: () -> Void
    let refreshKey: Int

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenFactoriesKt.WeightListViewController(
            openAdd: openAdd,
            onBack: onBack,
            refreshKey: Int32(refreshKey)
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - Statistics View

private struct StatisticsView: UIViewControllerRepresentable {
    @Binding var navigationPath: NavigationPath

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenFactoriesKt.StatisticsViewController(
            onBack: {
                navigationPath.removeLast()
            }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - Helper Functions

private func getCurrentDateIso() -> String {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withFullDate]
    return formatter.string(from: Date())
}

private func getFormattedDate() -> String {
    let vm = ScreenFactoriesKt.getGlobalMainViewModel()
    return vm.formattedDate()
}

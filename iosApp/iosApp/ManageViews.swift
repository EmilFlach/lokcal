import SwiftUI
import Shared

// MARK: - MealsList Screen (navigation destination)

struct MealsListScreen: View {
    let dateIso: String
    @Binding var navigationPath: NavigationPath

    var body: some View {
        MealsListView(navigationPath: $navigationPath, dateIso: dateIso)
            .ignoresSafeArea(.all)
            .navigationTitle("Meals")
            .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - FoodManage Screen (navigation destination)

struct FoodManageScreen: View {
    let dateIso: String
    @Binding var navigationPath: NavigationPath

    var body: some View {
        FoodManageView(navigationPath: $navigationPath, dateIso: dateIso)
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
    }
}

// MARK: - FoodEdit Screen (navigation destination)

struct FoodEditScreen: View {
    let foodId: Int64?
    @Binding var navigationPath: NavigationPath
    @Binding var refreshKey: Int

    var body: some View {
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
    }
}

// MARK: - MealsList ViewController Wrapper

struct MealsListView: UIViewControllerRepresentable {
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

// MARK: - FoodManage ViewController Wrapper

struct FoodManageView: UIViewControllerRepresentable {
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

// MARK: - FoodEdit ViewController Wrapper

struct FoodEditView: UIViewControllerRepresentable {
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

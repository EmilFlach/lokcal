import SwiftUI
import Shared

// MARK: - EditMeal Screen (navigation destination)

struct EditMealScreen: View {
    let mealId: Int64
    let returnMealType: String
    let dateIso: String
    @Binding var navigationPath: NavigationPath
    @Binding var refreshKey: Int

    var body: some View {
        EditMealView(
            mealId: mealId,
            navigationPath: $navigationPath,
            onDeleted: {
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
                } label: {
                    Image(systemName: "trash")
                }
            }
        }
    }
}

// MARK: - EditMealFromList Screen (navigation destination)

struct EditMealFromListScreen: View {
    let mealId: Int64
    @Binding var navigationPath: NavigationPath
    @Binding var refreshKey: Int

    var body: some View {
        EditMealFromListView(
            mealId: mealId,
            navigationPath: $navigationPath,
            onBack: {
                ScreenFactoriesKt.getGlobalMealsListViewModel().refresh()
                navigationPath.removeLast()
                refreshKey += 1
            },
            onDeleted: {
                ScreenFactoriesKt.getGlobalMealsListViewModel().refresh()
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
                } label: {
                    Image(systemName: "trash")
                }
            }
        }
    }
}

// MARK: - EditMeal ViewController Wrapper

struct EditMealView: UIViewControllerRepresentable {
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

// MARK: - EditMealFromList ViewController Wrapper

struct EditMealFromListView: UIViewControllerRepresentable {
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

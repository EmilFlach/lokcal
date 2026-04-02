import SwiftUI
import Shared

// MARK: - MealsList Screen (navigation destination)

struct MealsListScreen: View {
    let dateIso: String
    @Binding var navigationPath: NavigationPath

    var body: some View {
        MealsListSearchableView(navigationPath: $navigationPath, dateIso: dateIso)
            .ignoresSafeArea(.all)
            .navigationTitle("Meals")
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                ScreenFactoriesKt.getGlobalMealsListViewModel().refresh()
            }
    }
}

// MARK: - MealsList Searchable View

struct MealsListSearchableView: View {
    @Binding var navigationPath: NavigationPath
    let dateIso: String
    @State private var searchText = ""
    @State private var isSearchPresented = false
    @State private var showMissingImages = false

    var body: some View {
        MealsListView(
            navigationPath: $navigationPath,
            dateIso: dateIso,
            searchQuery: searchText,
            showMissingImages: showMissingImages
        )
        .searchable(text: $searchText, isPresented: $isSearchPresented, placement: .toolbar, prompt: "Search meals")
        .autocorrectionDisabled()
        .textInputAutocapitalization(.never)
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                Menu {
                    Picker("Filter", selection: $showMissingImages) {
                        Text("All").tag(false)
                        Text("Missing Images").tag(true)
                    }
                } label: {
                    Image(systemName: showMissingImages ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
                }
            }
        }
    }
}

// MARK: - FoodManage Screen (navigation destination)

struct FoodManageScreen: View {
    let dateIso: String
    @Binding var navigationPath: NavigationPath

    var body: some View {
        FoodManageSearchableView(navigationPath: $navigationPath, dateIso: dateIso)
            .ignoresSafeArea(.all)
            .navigationTitle("Foods")
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                ScreenFactoriesKt.getGlobalFoodEditViewModel().refresh()
            }
    }
}

// MARK: - FoodManage Searchable View

struct FoodManageSearchableView: View {
    @Binding var navigationPath: NavigationPath
    let dateIso: String
    @State private var searchText = ""
    @State private var isSearchPresented = false
    @State private var showMissingImages = false

    var body: some View {
        FoodManageView(
            navigationPath: $navigationPath,
            dateIso: dateIso,
            searchQuery: searchText,
            showMissingImages: showMissingImages
        )
        .searchable(text: $searchText, isPresented: $isSearchPresented, placement: .toolbar, prompt: "Search foods")
        .autocorrectionDisabled()
        .textInputAutocapitalization(.never)
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                Button {
                    navigationPath.append(NavigationDestination.foodEdit(foodId: nil, dateIso: dateIso))
                } label: {
                    Image(systemName: "plus")
                }
                Menu {
                    Picker("Filter", selection: $showMissingImages) {
                        Text("All").tag(false)
                        Text("Missing Images").tag(true)
                    }
                } label: {
                    Image(systemName: showMissingImages ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
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
            ScreenFactoriesKt.getGlobalFoodEditViewModel().refresh()
            navigationPath.removeLast()
            refreshKey += 1
        }
        let onDeleted = {
            ScreenFactoriesKt.getGlobalFoodEditViewModel().refresh()
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
        }
    }
}

// MARK: - MealsList ViewController Wrapper

struct MealsListView: UIViewControllerRepresentable {
    @Binding var navigationPath: NavigationPath
    let dateIso: String
    let searchQuery: String
    let showMissingImages: Bool

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
            },
            searchQuery: searchQuery,
            showMissingImages: showMissingImages
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        let vm = ScreenFactoriesKt.getGlobalMealsListViewModel()
        vm.setSearch(value: searchQuery)
        if let current = vm.filterMissingImages.value as? Bool, current != showMissingImages {
            vm.toggleMissingImagesFilter()
        }
    }
}

// MARK: - FoodManage ViewController Wrapper

struct FoodManageView: UIViewControllerRepresentable {
    @Binding var navigationPath: NavigationPath
    let dateIso: String
    let searchQuery: String
    let showMissingImages: Bool

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
            },
            searchQuery: searchQuery,
            showMissingImages: showMissingImages
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        let vm = ScreenFactoriesKt.getGlobalFoodEditViewModel()
        vm.setSearch(value: searchQuery)
        if let current = vm.filterMissingImages.value as? Bool, current != showMissingImages {
            vm.toggleMissingImagesFilter()
        }
    }
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
                ScreenFactoriesKt.getGlobalFoodEditViewModel().refresh()
                navigationPath.removeLast()
            },
            onSaved: onSaved,
            onDeleted: onDeleted
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

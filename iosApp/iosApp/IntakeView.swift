import SwiftUI
import Shared

// MARK: - Intake Screen (navigation destination)

struct IntakeScreen: View {
    let mealType: String
    let dateIso: String
    @Binding var navigationPath: NavigationPath
    @Binding var refreshKey: Int

    var body: some View {
        IntakeSearchableView(
            mealType: mealType,
            dateIso: dateIso,
            navigationPath: $navigationPath,
            onDone: { itemAdded in
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
    }
}

// MARK: - Intake Searchable View

struct IntakeSearchableView: View {
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
        .onChange(of: isSearchPresented) { _, newValue in
            if newValue {
                KeyboardToolbarManager.shared.setIntakeFieldFocused(false)
            }
        }
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

// MARK: - Intake ViewController Wrapper

struct IntakeView: UIViewControllerRepresentable {
    let mealType: String
    let dateIso: String
    @Binding var navigationPath: NavigationPath
    let onDone: (Bool) -> Void
    let searchQuery: String

    func makeUIViewController(context: Context) -> UIViewController {
        let viewController = ScreenFactoriesKt.IntakeViewController(
            mealType: mealType,
            dateIso: dateIso,
            onDone: { itemAdded in
                onDone(itemAdded.boolValue)
            },
            autoFocusSearch: false,
            searchQuery: searchQuery
        )

        KeyboardToolbarManager.shared.setup()
        return viewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        ScreenFactoriesKt.getIntakeViewModel(mealType: mealType, dateIso: dateIso).setQuery(value: searchQuery)
    }
}

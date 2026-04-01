import SwiftUI
import Shared

// MARK: - WeightList Screen (navigation destination)

struct WeightListScreen: View {
    let openAdd: Bool
    let returnToSettings: Bool
    @Binding var navigationPath: NavigationPath
    @Binding var refreshKey: Int

    var body: some View {
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
    }
}

// MARK: - WeightList ViewController Wrapper

struct WeightListView: UIViewControllerRepresentable {
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

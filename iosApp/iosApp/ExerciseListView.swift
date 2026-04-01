import SwiftUI
import Shared

// MARK: - ExerciseList Screen (navigation destination)

struct ExerciseListScreen: View {
    let dateIso: String
    @Binding var navigationPath: NavigationPath
    let refreshKey: Int

    var body: some View {
        ExerciseListView(dateIso: dateIso, navigationPath: $navigationPath, refreshKey: refreshKey)
            .ignoresSafeArea(.all)
            .navigationTitle("Exercise")
            .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - ExerciseList ViewController Wrapper

struct ExerciseListView: UIViewControllerRepresentable {
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

import SwiftUI
import Shared

// MARK: - Statistics Screen (navigation destination)

struct StatisticsScreen: View {
    @Binding var navigationPath: NavigationPath

    var body: some View {
        StatisticsView(navigationPath: $navigationPath)
            .ignoresSafeArea(.all)
            .navigationTitle("Statistics")
            .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Statistics ViewController Wrapper

struct StatisticsView: UIViewControllerRepresentable {
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

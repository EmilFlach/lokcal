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
            onBack: { navigationPath.removeLast() },
            onOpenDemo: { navigationPath.append(NavigationDestination.statisticsDemo) }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - Statistics Demo Screen (navigation destination)

struct StatisticsDemoScreen: View {
    @Binding var navigationPath: NavigationPath

    var body: some View {
        StatisticsDemoView(navigationPath: $navigationPath)
            .ignoresSafeArea(.all)
            .navigationTitle("Demo")
            .navigationBarTitleDisplayMode(.inline)
    }
}

struct StatisticsDemoView: UIViewControllerRepresentable {
    @Binding var navigationPath: NavigationPath

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenFactoriesKt.StatisticsDemoViewController(
            onBack: { navigationPath.removeLast() }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

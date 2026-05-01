import SwiftUI
import Shared

// MARK: - Settings Screen (navigation destination)

struct SettingsScreen: View {
    @Binding var navigationPath: NavigationPath

    var body: some View {
        SettingsView(navigationPath: $navigationPath)
            .ignoresSafeArea(.all)
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - SourcePreference Screen (navigation destination)

struct SourcePreferenceScreen: View {
    @Binding var navigationPath: NavigationPath

    var body: some View {
        SourcePreferenceView(navigationPath: $navigationPath)
            .ignoresSafeArea(.all)
            .navigationTitle("Search Sources")
            .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Settings ViewController Wrapper

struct SettingsView: UIViewControllerRepresentable {
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
            onOpenExerciseManage: {
                let currentDate = getCurrentDateIso()
                navigationPath.append(NavigationDestination.exerciseManage(dateIso: currentDate))
            },
            onOpenSourcePreferences: {
                navigationPath.append(NavigationDestination.sourcePreference)
            },
            onOpenLicenses: {
                navigationPath.append(NavigationDestination.licenses)
            }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - Licenses Screen (navigation destination)

struct LicensesScreen: View {
    @Binding var navigationPath: NavigationPath

    var body: some View {
        LicensesView(navigationPath: $navigationPath)
            .ignoresSafeArea(.all)
            .navigationTitle("Open source licenses")
            .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Licenses ViewController Wrapper

struct LicensesView: UIViewControllerRepresentable {
    @Binding var navigationPath: NavigationPath

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenFactoriesKt.LicensesViewController(
            onBack: {
                navigationPath.removeLast()
            }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - SourcePreference ViewController Wrapper

struct SourcePreferenceView: UIViewControllerRepresentable {
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

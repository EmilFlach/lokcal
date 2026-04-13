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

// MARK: - Helper Functions

func getCurrentDateIso() -> String {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withFullDate]
    return formatter.string(from: Date())
}

func getFormattedDate() -> String {
    let vm = ScreenFactoriesKt.getGlobalMainViewModel()
    return vm.formattedDate()
}

func getShowWeightPrompt() -> Bool {
    (ScreenFactoriesKt.getGlobalMainViewModel().uiState.value as? MainUiState)?.dayState.showWeightPrompt == true
}

func getSelectedDateIso() -> String {
    ScreenFactoriesKt.getGlobalMainViewModel().getSelectedDateIso()
}

func dateFromIso(_ iso: String) -> Date {
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withFullDate]
    return formatter.date(from: iso) ?? Date()
}


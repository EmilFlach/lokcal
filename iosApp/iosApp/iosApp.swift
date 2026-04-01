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

import SwiftUI
import Shared

// MARK: - Main Screen (navigation root)

struct MainScreen: View {
    @Binding var navigationPath: NavigationPath
    @Binding var refreshKey: Int
    @State private var mainScreenTitle = ""
    @State private var showWeightBadge = false
    @State private var badgePulse = false

    var body: some View {
        MainView(navigationPath: $navigationPath, refreshKey: refreshKey)
            .ignoresSafeArea(.all)
            .toolbarBackground(.hidden, for: .navigationBar)
            .navigationTitle(mainScreenTitle)
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                ScreenFactoriesKt.getGlobalMainViewModel().refresh()
                mainScreenTitle = getFormattedDate()
                showWeightBadge = getShowWeightPrompt()
            }
            .onReceive(Timer.publish(every: 0.1, on: .main, in: .common).autoconnect()) { _ in
                let newTitle = getFormattedDate()
                if newTitle != mainScreenTitle {
                    mainScreenTitle = newTitle
                }
                let newBadge = getShowWeightPrompt()
                if newBadge != showWeightBadge {
                    showWeightBadge = newBadge
                }
            }
            .onReceive(Timer.publish(every: 0.5, on: .main, in: .common).autoconnect()) { _ in
                if showWeightBadge {
                    withAnimation(.easeInOut(duration: 0.5)) {
                        badgePulse.toggle()
                    }
                }
            }
            .toolbar {
                ToolbarItemGroup(placement: .primaryAction) {
                    Button(action: {
                        navigationPath.append(NavigationDestination.statistics)
                    }) {
                        Image(systemName: "chart.bar")
                    }
                    Button(action: {
                        let currentDate = getCurrentDateIso()
                        navigationPath.append(NavigationDestination.weightList(openAdd: showWeightBadge, returnToSettings: false, dateIso: showWeightBadge ? currentDate : nil, refreshId: refreshKey))
                    }) {
                        Image(systemName: "scalemass")
                            .overlay(alignment: .topTrailing) {
                                if showWeightBadge {
                                    Circle()
                                        .fill(Color(red: 0xD9/255, green: 0x91/255, blue: 0x0D/255))
                                        .frame(width: 7, height: 7)
                                        .opacity(badgePulse ? 0.3 : 1.0)
                                        .offset(x: 4, y: -4)
                                }
                            }
                    }
                    Button(action: {
                        navigationPath.append(NavigationDestination.settings)
                    }) {
                        Image(systemName: "gearshape")
                    }
                }
            }
    }
}

// MARK: - Loading View

struct LoadingView: UIViewControllerRepresentable {
    let onReady: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        AppKt.initializeApp(onReady: onReady)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - Main ViewController Wrapper

struct MainView: UIViewControllerRepresentable {
    @Binding var navigationPath: NavigationPath
    let refreshKey: Int

    func makeUIViewController(context: Context) -> UIViewController {
        let currentDate = getCurrentDateIso()
        return ScreenFactoriesKt.MainViewController(
            dateIso: currentDate,
            onOpenMeal: { mealType, dateIso in
                navigationPath.append(NavigationDestination.mealTime(
                    mealType: mealType,
                    dateIso: dateIso,
                    highlightLatest: false,
                    refreshId: refreshKey
                ))
            },
            onOpenExercise: { dateIso in
                navigationPath.append(NavigationDestination.exerciseList(dateIso: dateIso))
            },
            onOpenSettings: {
                navigationPath.append(NavigationDestination.settings)
            },
            onOpenWeightToday: {
                let currentDate = getCurrentDateIso()
                navigationPath.append(NavigationDestination.weightList(
                    openAdd: true,
                    returnToSettings: false,
                    dateIso: currentDate
                ))
            },
            onOpenWeightList: {
                navigationPath.append(NavigationDestination.weightList(
                    openAdd: false,
                    returnToSettings: false,
                    dateIso: nil
                ))
            },
            onOpenStatistics: {
                navigationPath.append(NavigationDestination.statistics)
            },
            refreshKey: Int32(refreshKey)
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

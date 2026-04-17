import SwiftUI
import Shared

// MARK: - Main Screen (navigation root)

struct MainScreen: View {
    @Binding var navigationPath: NavigationPath
    @Binding var refreshKey: Int
    @State private var mainScreenTitle = ""
    @State private var showWeightBadge = false
    @State private var badgePulse = false
    @State private var showDatePicker = false
    @State private var pickedDate = Date()

    var body: some View {
        MainView(navigationPath: $navigationPath, refreshKey: refreshKey)
            .ignoresSafeArea(.all)
            .toolbarBackground(.hidden, for: .navigationBar)
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                ScreenFactoriesKt.getGlobalMainViewModel().refresh()
                mainScreenTitle = getFormattedDate()
                showWeightBadge = getShowWeightPrompt()
            }
            .onReceive(Timer.publish(every: 0.1, on: .main, in: .common).autoconnect()) { _ in
                guard navigationPath.isEmpty else { return }
                let newTitle = getFormattedDate()
                if newTitle != mainScreenTitle { mainScreenTitle = newTitle }
                let newBadge = getShowWeightPrompt()
                if newBadge != showWeightBadge { showWeightBadge = newBadge }
            }
            .onReceive(Timer.publish(every: 0.5, on: .main, in: .common).autoconnect()) { _ in
                if showWeightBadge {
                    withAnimation(.easeInOut(duration: 0.5)) {
                        badgePulse.toggle()
                    }
                }
            }
            .sheet(isPresented: $showDatePicker) {
                NavigationView {
                    DatePicker(
                        "",
                        selection: $pickedDate,
                        displayedComponents: .date
                    )
                    .datePickerStyle(.graphical)
                    .padding(.horizontal)
                    .navigationTitle("Select Date")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { showDatePicker = false }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Done") {
                                let formatter = ISO8601DateFormatter()
                                formatter.formatOptions = [.withFullDate]
                                let dateIso = formatter.string(from: pickedDate)
                                ScreenFactoriesKt.getGlobalMainViewModel().navigateToDate(dateIso: dateIso)
                                showDatePicker = false
                            }
                        }
                    }
                }
                .presentationDetents([.medium])
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {
                        pickedDate = dateFromIso(getSelectedDateIso())
                        showDatePicker = true
                    }) {
                        Text(mainScreenTitle)
                            .font(.headline)
                    }
                }
                ToolbarItemGroup(placement: .primaryAction) {
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
                                        .offset(x: 0, y: -2)
                                }
                            }
                    }
                    Button(action: {
                        navigationPath.append(NavigationDestination.statistics)
                    }) {
                        Image(systemName: "chart.bar")
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
    let onReady: (Bool) -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        AppKt.initializeApp(onReady: { needsOnboarding in
            onReady(needsOnboarding.boolValue)
        })
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - Onboarding View

struct OnboardingView: UIViewControllerRepresentable {
    let onGetStarted: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenFactoriesKt.OnboardingViewController(onGetStarted: onGetStarted)
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

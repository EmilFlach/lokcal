import SwiftUI
import Shared

// MARK: - MealTime Screen (navigation destination)

struct MealTimeScreen: View {
    let mealType: String
    let dateIso: String
    let highlightLatest: Bool
    let refreshId: Int
    @Binding var navigationPath: NavigationPath
    @Binding var refreshKey: Int

    var body: some View {
        MealTimeView(
            mealType: mealType,
            dateIso: dateIso,
            shouldHighlightLatest: highlightLatest,
            navigationPath: $navigationPath,
            refreshKey: refreshKey
        )
        .ignoresSafeArea(edges: .all)
        .navigationTitle(mealType.capitalized)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.hidden, for: .navigationBar)
        .toolbarBackground(.hidden, for: .tabBar)
        .id("\(mealType)-\(dateIso)-\(refreshId)")
        .onAppear {
            let vm = ScreenFactoriesKt.getMealTimeViewModel(mealType: mealType, dateIso: dateIso, refreshKey: Int32(refreshKey))
            vm.loadForSelectedDate(shouldHighlightLatest: false)
        }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    let vm = ScreenFactoriesKt.getMealTimeViewModel(mealType: mealType, dateIso: dateIso, refreshKey: Int32(refreshKey))
                    vm.toggleLeftovers()
                } label: {
                    Image(systemName: "bookmark")
                }
            }
            ToolbarItem(placement: .primaryAction) {
                Button {
                    // TODO: Trigger save meal dialog - handled in Compose
                } label: {
                    Image(systemName: "square.and.arrow.down")
                }
            }
        }
    }
}

// MARK: - MealTime View

struct MealTimeView: View {
    let mealType: String
    let dateIso: String
    let shouldHighlightLatest: Bool
    @Binding var navigationPath: NavigationPath
    let refreshKey: Int
    @State private var isKeyboardVisible = false

    var body: some View {
        MealTimeViewControllerWrapper(
            mealType: mealType,
            dateIso: dateIso,
            shouldHighlightLatest: shouldHighlightLatest,
            navigationPath: $navigationPath,
            refreshKey: refreshKey
        )
        .overlay(alignment: .bottom) {
            if !isKeyboardVisible {
                Button {
                    navigationPath.append(NavigationDestination.intake(mealType: mealType, dateIso: dateIso))
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 22))
                        Text("Add food")
                            .fontWeight(.semibold)
                    }
                    .foregroundStyle(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 14)
                    .background {
                        Capsule()
                            .fill(Color(red: 0xD9/255, green: 0x91/255, blue: 0x0D/255))
                            .overlay(
                                Capsule()
                                    .fill(
                                        LinearGradient(
                                            colors: [
                                                .white.opacity(0.2),
                                                .white.opacity(0.08),
                                                .clear
                                            ],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                    )
                            )
                    }
                    .overlay(
                        Capsule()
                            .stroke(.white.opacity(0.3), lineWidth: 0.5)
                    )
                }
                .shadow(color: Color(red: 0xD9/255, green: 0x91/255, blue: 0x0D/255).opacity(0.25), radius: 10, y: 5)
                .shadow(color: .black.opacity(0.18), radius: 7, y: 3)
                .padding(.bottom, 48)
                .transition(.opacity.combined(with: .move(edge: .bottom)))
            }
        }
        .onAppear {
            NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillShowNotification, object: nil, queue: .main) { _ in
                withAnimation(.easeOut(duration: 0.25)) {
                    isKeyboardVisible = true
                }
            }
            NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillHideNotification, object: nil, queue: .main) { _ in
                withAnimation(.easeOut(duration: 0.25)) {
                    isKeyboardVisible = false
                }
            }
        }
    }
}

// MARK: - MealTime ViewController Wrapper

struct MealTimeViewControllerWrapper: UIViewControllerRepresentable {
    let mealType: String
    let dateIso: String
    let shouldHighlightLatest: Bool
    @Binding var navigationPath: NavigationPath
    let refreshKey: Int

    func makeUIViewController(context: Context) -> UIViewController {
        let viewController = ScreenFactoriesKt.MealTimeViewController(
            mealType: mealType,
            dateIso: dateIso,
            shouldHighlightLatest: shouldHighlightLatest,
            onBack: {
                navigationPath.removeLast()
            },
            onAdd: { mealType in
                navigationPath.append(NavigationDestination.intake(
                    mealType: mealType,
                    dateIso: dateIso
                ))
            },
            refreshKey: Int32(refreshKey)
        )

        KeyboardToolbarManager.shared.setup()
        return viewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

import SwiftUI
import UIKit
import Shared

// MARK: - Search Bar Barcode Button

/// Installs a barcode bookmark button inside the UISearchBar created by .searchable.
/// Uses a delegate proxy to forward all other delegate calls to SwiftUI's internal handler.
private struct SearchBarBarcodeInstaller: UIViewRepresentable {
    let onTap: () -> Void

    func makeCoordinator() -> Coordinator { Coordinator(onTap: onTap) }

    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)
        view.isHidden = true
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        context.coordinator.onTap = onTap
        DispatchQueue.main.async {
            guard let window = uiView.window,
                  let searchBar = window.findFirstSubview(ofType: UISearchBar.self),
                  !context.coordinator.installed else { return }
            context.coordinator.install(on: searchBar)
        }
    }

    final class Coordinator: NSObject, UISearchBarDelegate {
        var onTap: () -> Void
        var installed = false
        private weak var proxiedDelegate: UISearchBarDelegate?

        init(onTap: @escaping () -> Void) { self.onTap = onTap }

        func install(on searchBar: UISearchBar) {
            installed = true
            proxiedDelegate = searchBar.delegate
            searchBar.delegate = self
            searchBar.showsBookmarkButton = true
            searchBar.setImage(
                UIImage(systemName: "barcode.viewfinder"),
                for: .bookmark, state: .normal
            )
        }

        func searchBarBookmarkButtonClicked(_ searchBar: UISearchBar) {
            onTap()
        }

        // Forward everything else to SwiftUI's internal delegate
        override func responds(to aSelector: Selector!) -> Bool {
            super.responds(to: aSelector) || (proxiedDelegate?.responds(to: aSelector) ?? false)
        }
        override func forwardingTarget(for aSelector: Selector!) -> Any? {
            proxiedDelegate
        }
    }
}

private extension UIView {
    func findFirstSubview<T: UIView>(ofType type: T.Type) -> T? {
        if let match = self as? T { return match }
        for sub in subviews {
            if let found = sub.findFirstSubview(ofType: type) { return found }
        }
        return nil
    }
}

// MARK: - Intake Screen (navigation destination)

struct IntakeScreen: View {
    let mealType: String
    let dateIso: String
    @Binding var navigationPath: NavigationPath
    @Binding var refreshKey: Int

    var body: some View {
        IntakeSearchableView(
            mealType: mealType,
            dateIso: dateIso,
            navigationPath: $navigationPath,
            onDone: { itemAdded in
                if navigationPath.count >= 2 {
                    navigationPath.removeLast(2)
                }
                refreshKey += 1
                navigationPath.append(NavigationDestination.mealTime(
                    mealType: mealType,
                    dateIso: dateIso,
                    highlightLatest: itemAdded,
                    refreshId: refreshKey
                ))
            },
            onNavigateToSettings: {
                navigationPath.append(NavigationDestination.sourcePreference)
            }
        )
        .ignoresSafeArea(edges: .all)
        .navigationTitle(mealType.capitalized)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.hidden, for: .navigationBar)
        .toolbarBackground(.hidden, for: .tabBar)
        .onAppear {
            ScreenFactoriesKt.getIntakeViewModel(mealType: mealType, dateIso: dateIso).refreshSourcesConfigured()
        }
    }
}

// MARK: - Intake Searchable View

struct IntakeSearchableView: View {
    let mealType: String
    let dateIso: String
    @Binding var navigationPath: NavigationPath
    let onDone: (Bool) -> Void
    let onNavigateToSettings: () -> Void
    @State private var searchText = ""
    @State private var isSearchPresented = false
    @State private var showScanner = false

    var body: some View {
        IntakeView(
            mealType: mealType,
            dateIso: dateIso,
            navigationPath: $navigationPath,
            onDone: onDone,
            onNavigateToSettings: onNavigateToSettings,
            searchQuery: searchText
        )
        .searchable(text: $searchText, isPresented: $isSearchPresented, placement: .toolbar, prompt: "Search foods and meals")
        .autocorrectionDisabled()
        .textInputAutocapitalization(.never)
        .onChange(of: isSearchPresented) { _, newValue in
            if newValue {
                KeyboardToolbarManager.shared.setIntakeFieldFocused(false)
            }
        }
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now()) {
                isSearchPresented = true
            }
        }
        .background(
            SearchBarBarcodeInstaller {
                showScanner = true
            }
        )
        .fullScreenCover(isPresented: $showScanner) {
            NativeScannerView(mealType: mealType, dateIso: dateIso) {
                showScanner = false
            }
        }
    }
}

// MARK: - Native Scanner Wrapper

struct NativeScannerView: View {
    let mealType: String
    let dateIso: String
    let onDismiss: () -> Void

    private let scannerController = IosScannerController()
    @State private var torchOn = false

    var body: some View {
        NavigationStack {
            ScannerComposeView(
                mealType: mealType,
                dateIso: dateIso,
                scannerController: scannerController,
                onDismiss: onDismiss
            )
            .ignoresSafeArea()
            .navigationTitle("Scan barcode")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel", action: onDismiss)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        torchOn.toggle()
                        scannerController.toggleTorch()
                    } label: {
                        Image(systemName: torchOn ? "flashlight.on.fill" : "flashlight.off.fill")
                    }
                }
            }
        }
    }
}

struct ScannerComposeView: UIViewControllerRepresentable {
    let mealType: String
    let dateIso: String
    let scannerController: IosScannerController
    let onDismiss: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        ScreenFactoriesKt.ScannerViewController(
            controller: scannerController,
            onScan: { barcode in
                ScreenFactoriesKt.getIntakeViewModel(mealType: mealType, dateIso: dateIso).setQuery(value: barcode)
                onDismiss()
            },
            onClose: onDismiss
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

// MARK: - Intake ViewController Wrapper

struct IntakeView: UIViewControllerRepresentable {
    let mealType: String
    let dateIso: String
    @Binding var navigationPath: NavigationPath
    let onDone: (Bool) -> Void
    let onNavigateToSettings: () -> Void
    let searchQuery: String

    func makeUIViewController(context: Context) -> UIViewController {
        // Reset any stale scanner state from a previous visit
        ScreenFactoriesKt.getIntakeViewModel(mealType: mealType, dateIso: dateIso).setShowScanner(show: false)

        let viewController = ScreenFactoriesKt.IntakeViewController(
            mealType: mealType,
            dateIso: dateIso,
            onDone: { itemAdded in
                onDone(itemAdded.boolValue)
            },
            onNavigateToSettings: onNavigateToSettings,
            autoFocusSearch: false,
            searchQuery: searchQuery
        )

        KeyboardToolbarManager.shared.setup()
        return viewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        ScreenFactoriesKt.getIntakeViewModel(mealType: mealType, dateIso: dateIso).setQuery(value: searchQuery)
    }
}

import UIKit

// MARK: - Keyboard Toolbar Manager

class KeyboardToolbarManager {
    static let shared = KeyboardToolbarManager()

    private var toolbar: UIToolbar?
    private var isIntakeFieldFocused = false

    func setIntakeFieldFocused(_ focused: Bool) {
        isIntakeFieldFocused = focused
    }

    func setup() {
        guard toolbar == nil else { return }

        let toolbar = UIToolbar()
        toolbar.sizeToFit()

        let closeButton = UIBarButtonItem(barButtonSystemItem: .close, target: self, action: #selector(cancelKeyboard))
        let flexSpace = UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil)

        toolbar.items = [closeButton, flexSpace]
        toolbar.alpha = 0
        self.toolbar = toolbar

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(keyboardWillShow(_:)),
            name: UIResponder.keyboardWillShowNotification,
            object: nil
        )

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(keyboardWillHide(_:)),
            name: UIResponder.keyboardWillHideNotification,
            object: nil
        )

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(intakeFieldFocusChanged(_:)),
            name: NSNotification.Name("IntakeFieldFocusChanged"),
            object: nil
        )
    }

    @objc private func intakeFieldFocusChanged(_ notification: Notification) {
        if let userInfo = notification.userInfo,
           let focused = userInfo["focused"] as? Bool {
            isIntakeFieldFocused = focused
        }
    }

    @objc private func keyboardWillShow(_ notification: Notification) {
        guard let toolbar = toolbar,
              let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect,
              let window = getKeyWindow() else {
            return
        }

        // Only show toolbar for intake fields (not search or other fields)
        if !isIntakeFieldFocused {
            toolbar.alpha = 0
            return
        }

        let toolbarHeight: CGFloat = 44
        let gap: CGFloat = 12
        let finalY = keyboardFrame.origin.y - toolbarHeight - gap

        // Disable implicit animations to prevent toolbar animating from top on first show
        CATransaction.begin()
        CATransaction.setDisableActions(true)

        if toolbar.superview == nil {
            window.addSubview(toolbar)
        }

        let toolbarWidth: CGFloat = 80
        toolbar.frame = CGRect(
            x: 0,
            y: finalY,
            width: toolbarWidth,
            height: toolbarHeight
        )

        CATransaction.commit()

        // Fade in toolbar synchronized with keyboard animation
        if let duration = notification.userInfo?[UIResponder.keyboardAnimationDurationUserInfoKey] as? Double {
            UIView.animate(withDuration: duration) {
                toolbar.alpha = 1
            }
        } else {
            toolbar.alpha = 1
        }
    }

    private func getKeyWindow() -> UIWindow? {
        return UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first(where: { $0.isKeyWindow })
    }

    @objc private func keyboardWillHide(_ notification: Notification) {
        guard let toolbar = toolbar else { return }

        if let duration = notification.userInfo?[UIResponder.keyboardAnimationDurationUserInfoKey] as? Double {
            UIView.animate(withDuration: duration, animations: {
                toolbar.alpha = 0
            }) { _ in
                toolbar.removeFromSuperview()
            }
        } else {
            toolbar.removeFromSuperview()
        }
    }

    @objc private func cancelKeyboard() {
        getKeyWindow()?.endEditing(true)
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
        toolbar?.removeFromSuperview()
    }
}

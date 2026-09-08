---
name: android-device-run
description: Connect a physical Android phone and install or launch Lokcal on it. Use for USB or wireless ADB connections, QR pairing, and physical-device deployment; prefer QR pairing and release builds. Not for emulator-only runs or ordinary build/test work.
---

# Android device run

**Prefer wireless debugging with QR pairing. Automatically open the generated QR image on the Mac so the user can scan it; do not only provide an image link or ask the user to open it.** Use an existing authorized connection when available. USB and manual pairing codes are fallbacks.

1. Use the SDK's `platform-tools/adb` (on this Mac: `/Users/emil/Library/Android/sdk/platform-tools/adb`). Run `adb devices -l` and `adb mdns services`. The phone and Mac must be on the same network, with **Developer options → Wireless debugging** enabled on the phone.
2. If the phone is already paired but disconnected, use the currently advertised `_adb-tls-connect._tcp` address with `adb connect <ip>:<connect-port>`. Discover it each time; addresses and ports can change.
3. If pairing is needed, generate a fresh service name such as `studio-Lokcal-<random hex>` and a random password (for example, Python `secrets.token_hex(16)`). Encode this exact QR payload, substituting those values: `WIFI:T:ADB;S:<service-name>;P:<password>;;`. Keep temporary pairing files under `/tmp`, outside the repository; never reuse or commit pairing credentials.
4. Generate a high-contrast PNG with a white quiet zone. macOS Core Image's `CIQRCodeGenerator` works via a temporary Swift script; render with integer scaling and no smoothing. If the sandbox prevents Core Image rendering, request tool escalation for that rendering command.
5. Start a background listener polling `adb mdns services` about every two seconds, with a bounded timeout (for example, ten minutes). Match the **generated service name** on `_adb-tls-pairing._tcp`; when it appears, run `adb pair <advertised-ip>:<pairing-port> <generated-password>`. The pairing port is different from the connection port.
6. **Immediately open the PNG automatically**, for example `open -a Preview /tmp/lokcal-adb-pairing.png`, and also display it in the conversation when possible. Tell the user to open **Wireless debugging → Pair device with QR code** and scan it. Keep the listener running while they scan; no manual code exchange is needed.
7. After pairing succeeds, check `adb devices -l`. If necessary, discover the connection service again and run `adb connect` with its current address. Use the exact device ID reported by ADB, which may be an mDNS service name rather than an IP address.
8. When the user requested deployment, build, install, and launch with `ANDROID_HOME=… ./kotlin run -m androidApp -v release -d <device-id>`. Preserve existing app data; do not uninstall to work around an installation failure. Confirm launch succeeds and the installed package is not marked `DEBUGGABLE`. Stop the pairing listener when finished.

## Deployment defaults

Always use `-v release` on physical devices. Check the current device list before deploying; do not reuse a device ID or IP from a previous session. If several phones are connected and the target is unclear, ask which one to use.

A connection-only request ends after pairing/connecting; do not install or launch the app unless requested. If discovery or pairing times out, stop the listener and report the result rather than retrying indefinitely.

# ─── ML Kit barcode scanning (pulled in by KScan) ────────────────────────────
# ML Kit resolves its scanner through a Firebase-style component registry: the
# registrars are named only as <meta-data> strings in the manifest, and the
# components themselves are looked up reflectively. R8 full mode (the AGP default)
# prunes the members that lookup depends on, so BarcodeScanning.getClient()
# resolves a null component and NPEs in BarcodeAnalyzer.<init>.
# Release-only failure — debug builds are unminified, which is why it never showed
# up in `./kotlin build -m androidApp`.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**

# The component registry that ML Kit is discovered through.
-keep class com.google.firebase.components.** { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }

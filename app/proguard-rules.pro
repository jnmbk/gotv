# Add project specific ProGuard rules here.

# Compose UI + Lifecycle CompositionLocal keep rules
-keep class androidx.compose.ui.platform.AndroidCompositionLocals_androidKt { *; }
-keep class androidx.lifecycle.ViewTreeLifecycleOwner { *; }
-keep class androidx.savedstate.ViewTreeSavedStateRegistryOwner { *; }
-keep class androidx.lifecycle.ViewTreeViewModelStoreOwner { *; }

# Media3 ExoPlayer keep rules for reflection-based decoders/renderers
-keep class androidx.media3.** { *; }

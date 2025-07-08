# This file contains the definitive ProGuard rules to protect the native library.

# This rule keeps the entire class and all its public members.
-keep public class ir.zarbang.FFTsounds.bridge.UnityAudioBridge { *; }

# REDUNDANT BUT SAFE: This more specific rule explicitly keeps the getInstance method,
# ensuring it is never stripped by any aggressive optimization configuration.
-keepclassmembers class ir.zarbang.FFTsounds.bridge.UnityAudioBridge {
    public static ir.zarbang.FFTsounds.bridge.UnityAudioBridge getInstance(android.content.Context);
}

# Keep the data models that are accessed by the bridge.
-keep public class ir.zarbang.FFTsounds.fft.FrequencyBands { *; }

# Keep the service class, as it is instantiated by the Android system.
-keep public class ir.zarbang.FFTsounds.service.PlaybackService

# Prevent obfuscation of any class within the bridge package to be safe.
-keepnames class ir.zarbang.FFTsounds.bridge.** { *; }

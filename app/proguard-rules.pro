# This file contains ProGuard rules that will be packaged with the AAR.
# Any application that consumes this library will automatically apply these rules,
# preventing critical classes from being stripped by R8/ProGuard.

# --- Unity Bridge ---
# This is the most critical rule. It keeps our main API facade and all its
# public methods and fields, making them available for Unity's JNI calls.
-keep public class ir.zarbang.FFTsounds.bridge.UnityAudioBridge { *; }

# --- Standard Unity Keep Rules ---
# These are standard best practices for any native library intended for Unity,
# ensuring that classes extending Unity's core activities are not stripped.
-keep public class * extends com.unity3d.player.UnityPlayerActivity
-keep public class * extends com.unity3d.player.UnityPlayerGameActivity

# --- Resource Keep Rules ---
# This rule prevents the R class and its inner classes (like R.string, R.drawable)
# from being obfuscated, which can cause issues if resources are accessed by name.
-keep public class ir.zarbang.FFTsounds.R$* {
    public static <fields>;
}

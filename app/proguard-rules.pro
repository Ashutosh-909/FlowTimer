# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ── Jetpack Glance (App Widget) ─────────────────────────────────────────
# Glance resolves ActionCallback subclasses by fully-qualified class name
# at runtime. R8 must not rename or remove them.
-keep class * extends androidx.glance.appwidget.action.ActionCallback { *; }

# Keep the GlanceAppWidget and GlanceAppWidgetReceiver so the system can
# instantiate them via the manifest-declared class name.
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
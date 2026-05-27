# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified in the
# Android SDK's tools/proguard/proguard-android-optimize.txt

# Keep Room entities
-keep class com.lagoda.mnemo.data.db.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

# Keep ONNX Runtime
-keep class ai.onnxruntime.** { *; }

# Keep LiteRT/TFLite
-keep class com.google.ai.edge.** { *; }

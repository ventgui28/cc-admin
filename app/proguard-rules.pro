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

# Preserva atributos necessários para serialização
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Evita que o R8 remova ou altere membros de classes marcadas com @Serializable
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class * implements kotlinx.serialization.KSerializer

# Preservar data models
-keep class com.ventgui.app.data.model.** { *; }

# Preservar classes e annotations do Supabase SDK
-keep class io.github.jan.supabase.** { *; }
-keep interface io.github.jan.supabase.** { *; }

# Preservar classes e annotations do Ktor
-keep class io.ktor.** { *; }
-keep interface io.ktor.** { *; }
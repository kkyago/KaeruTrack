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

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes SerializedName
-keepattributes InnerClasses
-keepnames class kotlinx.coroutines.internal.MainDispatcherLoader {}
-keepnames class kotlinx.coroutines.android.HandlerContext {}
-keep class com.kaeru.app.** { *; }
-keep class androidx.room.** { *; }
-keep class kotlin.Metadata { *; }
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keep class kotlin.coroutines.Continuation { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.model.** { *; }
-keep interface com.google.api.client.** { *; }
-dontwarn com.google.gson.**
-dontwarn sun.misc.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**
-keepclassmembers  interface * {
    @retrofit2.http.* <methods>;
}
-keep class androidx.work.** { *; }
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
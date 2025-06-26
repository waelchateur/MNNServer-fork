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

# mls.api
-keep class com.alibaba.mls.api.** {*;}

# gson
-keep class com.google.gson.reflect.TypeToken {*;}
-keep class * extends com.google.gson.reflect.TypeToken
-keep class io.kindbrave.mnn.server.utils.TTSModelType {*;}
-keep class io.kindbrave.mnn.server.utils.TTSModelConfig {*;}
-keepattributes AnnotationDefault, RuntimeVisibleAnnotations


# retrofit2
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
# OkHttp3
-dontwarn okhttp3.logging.**
-keep class okhttp3.internal.**{*;}
-dontwarn okio.**
# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

-keep class kotlin.Pair { *; }

-keep class io.kindbrave.mnn.server.MNN {*;}

-keep class io.kindbrave.mnn.server.engine.** {*;}

-dontwarn java.lang.invoke.StringConcatFactory
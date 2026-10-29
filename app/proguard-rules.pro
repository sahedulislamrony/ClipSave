# youtubedl-android maps yt-dlp JSON with Jackson reflection.
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**
-keepattributes *Annotation*, Signature, InnerClasses

# kotlinx.serialization (download history)
-keepclassmembers class com.sahedul.clipsave.** { *** Companion; }
-keepclasseswithmembers class com.sahedul.clipsave.** { kotlinx.serialization.KSerializer serializer(...); }

# yt-dlp/ffmpeg unpacking uses commons-compress, which registers zip extra fields reflectively.
-keep class org.apache.commons.compress.** { *; }
-keep class org.apache.commons.io.** { *; }
-dontwarn org.apache.commons.compress.**
-dontwarn org.apache.commons.io.**

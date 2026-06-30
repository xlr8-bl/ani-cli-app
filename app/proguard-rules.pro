# XLR8 ProGuard / R8 rules

# Keep kotlinx.serialization metadata for our @Serializable models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep serializers for classes annotated with @Serializable in our package.
-keepclassmembers class com.xlr8.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.xlr8.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.xlr8.app.**$$serializer { *; }

# Ktor / OkHttp
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Media3 keeps its own consumer rules; nothing extra needed here.

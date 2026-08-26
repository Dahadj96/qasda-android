# kotlinx.serialization keeps its serializers on the classes it generates them for.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class pro.qasdatrip.** { *** Companion; }
-keepclasseswithmembers class pro.qasdatrip.** { kotlinx.serialization.KSerializer serializer(...); }

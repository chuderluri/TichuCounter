# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class ch.tichu.counter.**$$serializer { *; }
-keepclassmembers class ch.tichu.counter.** { *** Companion; }
-keepclasseswithmembers class ch.tichu.counter.** { kotlinx.serialization.KSerializer serializer(...); }

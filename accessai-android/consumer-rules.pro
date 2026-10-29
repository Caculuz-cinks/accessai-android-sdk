# AccessAI wire models are (de)serialised with kotlinx.serialization.
-keep,includedescriptorclasses class com.accessai.core.protocol.**$$serializer { *; }
-keepclassmembers class com.accessai.core.protocol.** { *** Companion; }
-keepclasseswithmembers class com.accessai.core.protocol.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.accessai.core.screen.**$$serializer { *; }
-keepclassmembers class com.accessai.core.screen.** { *** Companion; }
-keepclasseswithmembers class com.accessai.core.screen.** { kotlinx.serialization.KSerializer serializer(...); }

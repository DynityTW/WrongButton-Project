# kotlinx.serialization keeps generated serializers via its own consumer rules.

# Navigation 3 的 NavKeySerializer 用類別名稱（Class.forName）＋反射找 serializer 來存、還原返回堆疊，
# 而且 Navigation 3 沒有附 R8 規則。保留畫面鍵的名稱與成員，存檔後換版本也能還原。
-keep class * implements androidx.navigation3.runtime.NavKey { *; }

# 我們自己的 @Serializable 類別（存檔、設定、付費狀態、關卡狀態）：保留名稱、成員與產生的 serializer。
-keep @kotlinx.serialization.Serializable class tw.dynity.wrongbutton.** { *; }
-keep class tw.dynity.wrongbutton.**$$serializer { *; }
-keepclassmembers class tw.dynity.wrongbutton.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault, InnerClasses, Signature, EnclosingMethod

# 閃退時留下看得懂的行號（對照 mapping.txt 還原）。
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

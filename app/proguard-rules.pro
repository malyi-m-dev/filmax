# ProGuard/R8 правила для release-сборки Filmax.
#
# Базовый набор: kotlinx.serialization требует явных keep-правил (генерируемые
# сериализаторы находятся по рефлексии). Остальной стек — Koin (конструкторный),
# Ktor/OkHttp, Media3, Coil3 — везёт consumer-rules сам, поэтому здесь их нет.
# Если после реальной проверки release-сборки что-то отвалится (R8 вырезал
# используемое через рефлексию) — точечно доправить keep-правилами ниже.

# --- Общие атрибуты, нужные рефлексии/сериализации ---
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-keepattributes InnerClasses, Signature, EnclosingMethod, Exceptions

# --- kotlinx.serialization ------------------------------------------------
# Официальный набор правил (см. README kotlinx.serialization).

# Оставляем поле Companion у @Serializable-классов.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Оставляем serializer() на companion-объектах @Serializable-классов.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Оставляем INSTANCE.serializer() у @Serializable-объектов.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Сохраняем сгенерированные $$serializer-классы целиком.
-keepclassmembers class **$$serializer {
    *** descriptor;
}
-keep,includedescriptorclasses class **$$serializer { *; }

# Значения enum-ов, участвующих в сериализации, не должны переименовываться.
-keepclassmembers @kotlinx.serialization.Serializable class * {
    <fields>;
}

# --- Прочее ---------------------------------------------------------------
# Метаданные Kotlin (нужны рефлексии и корректной работе сериализации).
-keep class kotlin.Metadata { *; }

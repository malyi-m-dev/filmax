plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library)     apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.jvm)          apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler)    apply false
    // Firebase: применяются в :app и только при наличии google-services.json (см. app/build.gradle.kts).
    alias(libs.plugins.google.services)     apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

plugins {
    id("filmax.android.compose")
}

android {
    namespace = "com.filmax.core.presentation"
}

dependencies {
    val bom = platform(libs.compose.bom)
    api(bom)
    // Compose runtime + androidx.lifecycle.ViewModel/viewModelScope живут здесь — и ТОЛЬКО здесь.
    api(libs.bundles.compose)
    api(libs.kotlinx.coroutines.core)
}

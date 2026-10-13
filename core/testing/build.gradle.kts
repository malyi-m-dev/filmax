// Общее окружение UI-тестов на Robolectric. Подключается к тестам UI-модулей конвенцией
// filmax.android.ui-test, в рабочий код не попадает.
plugins {
    id("filmax.android.library")
}

android { namespace = "com.filmax.core.testing" }

dependencies {
    api(libs.coil.compose)
    api(libs.coil.test)
    implementation(libs.junit4)
}

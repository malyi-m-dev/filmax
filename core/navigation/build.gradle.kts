// Контракт навигации: куда можно перейти ([Destination]) и чем это просят ([Navigator]).
// Чистый JVM-модуль без Android и Compose: фича не должна тянуть androidx.navigation ради того,
// чтобы сказать «открой карточку тайтла». Исполняет команды :app — он один знает граф.
plugins {
    id("filmax.kotlin.library")
}

dependencies {
    // Адреса экранов несут доменные типы: что играть (VideoRef) и что передать на ТВ.
    api(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.core)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
}

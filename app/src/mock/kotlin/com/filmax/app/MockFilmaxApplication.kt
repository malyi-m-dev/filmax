package com.filmax.app

import com.filmax.data.fake.di.fakeDataModule
import org.koin.core.module.Module

/**
 * Application сборки `mock`: тот же граф, что у debug, но репозитории kino.pub и TMDB подменены
 * фейками из :data:fake. Фейки ложатся ПОВЕРХ настоящих data-модулей — Koin 4 по умолчанию
 * разрешает override, и определение из модуля, загруженного позже, побеждает. В итоге приложение
 * стартует авторизованным и ни разу не ходит в kino.pub.
 */
class MockFilmaxApplication : FilmaxApplication() {
    override fun dataModules(): List<Module> = super.dataModules() + fakeDataModule
}

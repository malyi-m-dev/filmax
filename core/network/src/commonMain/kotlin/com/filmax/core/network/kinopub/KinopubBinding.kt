package com.filmax.core.network.kinopub

import com.filmax.core.domain.source.ContentSource
import org.koin.core.definition.Definition
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named

/** Квалификатор репозиториев kino.pub: по нему их находит переключатель источника (data:source). */
val KinopubSource: Qualifier = named(ContentSource.KINOPUB)

/**
 * Репозиторий kino.pub — под квалификатором [KinopubSource] и без него. Под квалификатором его
 * берёт переключатель источника и подменяет мок-сборка. Без квалификатора — те, у кого источник
 * один: iOS-слой и тесты. В приложении этот ключ потом занимает переключатель, а псевдоним
 * остаётся ссылкой на ту же реализацию, а не вторым экземпляром.
 */
inline fun <reified T : Any> Module.kinopubSingle(noinline definition: Definition<T>) {
    single<T>(KinopubSource, definition = definition)
    single<T> { get(KinopubSource) }
}

package com.filmax.data.fake

import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.NavCommand
import com.filmax.core.navigation.Navigator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Навигатор для тестов ScreenModel: команды копятся в [sent], а не уходят графу. Так тест
 * проверяет, куда модель повела пользователя, не поднимая NavHost.
 */
class FakeNavigator : Navigator {

    val sent = mutableListOf<NavCommand>()

    /** Адреса открытых экранов по порядку — самая частая проверка в тестах. */
    val opened: List<Destination>
        get() = sent.filterIsInstance<NavCommand.Open>().map { it.destination }

    override val commands: Flow<NavCommand> = emptyFlow()

    override fun open(destination: Destination) {
        sent += NavCommand.Open(destination)
    }

    override fun replace(destination: Destination) {
        sent += NavCommand.Replace(destination)
    }

    override fun root(destination: Destination) {
        sent += NavCommand.Root(destination)
    }

    override fun back() {
        sent += NavCommand.Back
    }
}

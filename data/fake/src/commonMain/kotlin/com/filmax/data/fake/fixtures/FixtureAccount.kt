package com.filmax.data.fake.fixtures

import com.filmax.core.domain.favorites.WATCHLIST_FOLDER_TITLE
import com.filmax.core.domain.user.model.DeviceSettings
import com.filmax.core.domain.user.model.SERVER_LOCATION_AUTO
import com.filmax.core.domain.user.model.Subscription
import com.filmax.core.domain.user.model.UserProfile
import com.filmax.core.domain.watching.model.Notification
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.store.FolderState

private const val MILLIS_IN_HOUR = 3_600_000L
private const val MILLIS_IN_DAY = 24 * MILLIS_IN_HOUR

/** Остаток подписки: заметно больше недели, чтобы плашка «нужна подписка» не мешала проверкам. */
private const val SUBSCRIPTION_DAYS_LEFT = 47

/**
 * Дата регистрации — постоянная, а не от текущего момента: из неё выводится ключ связи телефона с ТВ,
 * и две мок-сборки на разных устройствах должны узнать друг друга как один аккаунт.
 */
private const val DEMO_REGISTERED_AT = 1_577_836_800_000L

/** Папка «Буду смотреть» — то же название, по которому её ищет FavoritesRepositoryImpl. */

/**
 * Профиль «как от kino.pub»: id пользователя сервер не отдаёт (настоящий репозиторий тоже кладёт 0),
 * подписка активна, срок — от текущего момента.
 */
internal fun demoProfile(now: Long): UserProfile = UserProfile(
    id = 0,
    username = "Демо Зритель",
    email = "demo@example.com",
    avatarUrl = avatarUrl(),
    subscription = Subscription(
        active = true,
        endsAt = now + SUBSCRIPTION_DAYS_LEFT * MILLIS_IN_DAY,
        daysLeft = SUBSCRIPTION_DAYS_LEFT,
    ),
    registeredAt = DEMO_REGISTERED_AT,
)

/** Настройки устройства: тип потока 2 — HLS4, сервер раздачи — «Автоматически». */
internal val DEMO_DEVICE_SETTINGS = DeviceSettings(
    id = 4242,
    title = "Filmax Mock",
    supportSsl = true,
    supportHevc = true,
    supportHdr = false,
    support4k = true,
    mixedPlaylist = false,
    streamingType = 2,
    serverLocation = SERVER_LOCATION_AUTO,
)

private data class FolderSpec(val id: Int, val title: String, val itemIds: List<Int>, val daysAgo: Int)

/** «Отложено на потом» — 25 тайтлов: в «Моём» папка открывается двумя страницами. */
private val LATER_ITEM_IDS = listOf(
    1001,
    1002,
    1003,
    1005,
    1007,
    1009,
    1011,
    1013,
    1014,
    1017,
    1019,
    1021,
    1022,
    1024,
    1025,
    1026,
    2004,
    2005,
    2006,
    2008,
    2009,
    2010,
    2013,
    3002,
    3004,
)

/** «Буду смотреть» плюс три папки: обычная, большая (две страницы) и пустая. */
private val FOLDERS: List<FolderSpec> = listOf(
    FolderSpec(
        id = FakeIds.WATCHLIST_FOLDER,
        title = WATCHLIST_FOLDER_TITLE,
        itemIds = listOf(1012, 2007, 3003, 1018, 4004, FakeIds.HERO_MOVIE),
        daysAgo = 1,
    ),
    FolderSpec(
        id = 2,
        title = "Пересмотреть с семьёй",
        itemIds = listOf(FakeIds.HERO_MOVIE, 1020, 3005, 1015),
        daysAgo = 12,
    ),
    FolderSpec(id = FakeIds.LARGE_FOLDER, title = "Отложено на потом", itemIds = LATER_ITEM_IDS, daysAgo = 30),
    FolderSpec(id = FakeIds.EMPTY_FOLDER, title = "Новогоднее", itemIds = emptyList(), daysAgo = 90),
)

internal fun initialFolders(now: Long): List<FolderState> = FOLDERS.map { spec ->
    FolderState(
        id = spec.id,
        title = spec.title,
        itemIds = spec.itemIds,
        updatedAt = now - spec.daysAgo * MILLIS_IN_DAY,
    )
}

private data class NotificationSpec(
    val id: Int,
    val title: String,
    val text: String,
    val hoursAgo: Int,
    val read: Boolean,
    val itemId: Int?,
)

private val NOTIFICATIONS: List<NotificationSpec> = listOf(
    NotificationSpec(
        id = 1,
        title = "Новая серия «Хроники Пограничья»",
        text = "Вышла 16-я серия 7-го сезона.",
        hoursAgo = 2,
        read = false,
        itemId = FakeIds.LONG_SERIES,
    ),
    NotificationSpec(
        id = 2,
        title = "Вышел второй сезон «Станции „Восток“»",
        text = "Все восемь серий уже доступны.",
        hoursAgo = 26,
        read = false,
        itemId = 2007,
    ),
    NotificationSpec(
        id = 3,
        title = "Новое в подборке «Аниме для новичков»",
        text = "Добавили «Школу тихих голосов».",
        hoursAgo = 72,
        read = true,
        itemId = 3002,
    ),
    NotificationSpec(
        id = 4,
        title = "Подписка продлена",
        text = "Filmax Premium активен ещё на $SUBSCRIPTION_DAYS_LEFT дней.",
        hoursAgo = 240,
        read = true,
        itemId = null,
    ),
)

/** Уведомления: два непрочитанных — счётчик есть с первого запуска. */
internal fun demoNotifications(now: Long): List<Notification> = NOTIFICATIONS.map { spec ->
    Notification(
        id = spec.id,
        title = spec.title,
        text = spec.text,
        createdAt = now - spec.hoursAgo * MILLIS_IN_HOUR,
        read = spec.read,
        itemId = spec.itemId,
    )
}

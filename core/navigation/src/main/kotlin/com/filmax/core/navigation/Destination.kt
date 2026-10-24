package com.filmax.core.navigation

/**
 * Куда приложение умеет переходить. Абстрактный адрес, а не маршрут навигации: `@Serializable`
 * route-объекты остаются в модулях фич (их читает ScreenModel через `toRoute<…>()`), а сопоставление
 * «адрес → маршрут» живёт в `:app` — единственном месте, которое знает форму графа.
 *
 * Почему так: фича, которой нужно открыть карточку тайтла, не должна ни зависеть от модуля деталей,
 * ни получать колбэк из графа сверху. Она называет адрес — граф решает, как туда попасть (и на
 * телефоне, и на ТВ, где часть корней своя).
 */
sealed interface Destination {

    /**
     * Раздел нижней навигации. Переход к разделу — не обычный push: экземпляр единственный,
     * состояние вкладки сохраняется и восстанавливается (см. исполнение команд в `:app`).
     */
    sealed interface Tab : Destination

    data object Home : Tab
    data object Catalog : Tab

    /** Поиск — отдельная вкладка телефона; на ТВ поиск живёт в Каталоге, туда и ведёт. */
    data object Search : Tab
    data object Mine : Tab
    data object Profile : Tab

    /**
     * Раздел каталога с готовой выборкой: «Фильмы», «4K и HDR», «Новинки» — из чипов и шторки
     * разделов Главной. [section] — имя `CatalogSection` из domain строкой (core:navigation не знает
     * типов domain). Открывается поверх раздела, со своей кнопкой «назад».
     */
    data class Browse(val section: String) : Destination

    /** Вход в приложение — корень для неавторизованного пользователя. */
    data object Onboarding : Destination

    data class Details(val itemId: Int) : Destination

    /**
     * Плеер. [videoId] — НОМЕР видео, а не id трека: тем же числом kino.pub принимает и отдаёт
     * прогресс. [season] обязателен вместе с ним — номер уникален только внутри сезона.
     * Дефолтные `-1` означают «фильм или неизвестная серия»: плеер возьмёт первый трек.
     * [startSeconds] — позиция старта, когда просмотр передали с телефона; `-1` — с сохранённого прогресса.
     */
    data class Player(
        val itemId: Int,
        val videoId: Int = -1,
        val season: Int = -1,
        val startSeconds: Int = -1,
    ) : Destination

    data class Trailer(val url: String, val title: String) : Destination

    data class CollectionDetail(val collectionId: Int, val title: String) : Destination

    /**
     * Открытая папка-закладка «Моё». На ТВ — отдельный экран без меню (шапка цвета папки, сетка
     * тайтлов); на телефоне папка открывается внутри Библиотеки, своего экрана у неё нет.
     */
    data class BookmarkFolder(val folderId: Int) : Destination

    /** Фильмография человека: тап по актёру или режиссёру в деталях. */
    data class Filmography(val name: String, val isDirector: Boolean) : Destination

    /** Пульт телевизора — только на телефоне: им управляют подключённым ТВ. */
    data object Remote : Destination

    /**
     * Выбор телевизора в сети — только на телефоне. Аргументы — что запустить на нём, как у [Player];
     * [itemId] = -1 — ничего: телевизор выбирают ради пульта.
     */
    data class TvPicker(
        val itemId: Int = -1,
        val videoId: Int = -1,
        val season: Int = -1,
        val startSeconds: Int = -1,
    ) : Destination

    data object DeviceSettings : Destination

    /**
     * Окно оценки приложения. [trigger] — имя `FeedbackTrigger` из domain (Profile, AfterUpdate,
     * AfterWatch): core:navigation не знает типов domain, поэтому строкой.
     */
    data class Feedback(val trigger: String) : Destination

    /** Каталог компонентов дизайн-системы — только для debug-сборки. */
    data object DesignSystem : Destination
}

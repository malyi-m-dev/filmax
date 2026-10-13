package com.filmax.app.tv.navigation

import com.filmax.app.navigation.GraphRoutes
import com.filmax.core.navigation.Destination
import com.filmax.feature.collections.common.navigation.CollectionDetailRoute
import com.filmax.feature.details.common.navigation.DetailsRoute
import com.filmax.feature.home.tv.navigation.TvHomeRoute
import com.filmax.feature.library.common.navigation.FolderRoute
import com.filmax.feature.library.tv.navigation.TvLibraryRoute
import com.filmax.feature.onboarding.tv.navigation.TvOnboardingRoute
import com.filmax.feature.player.common.navigation.PlayerRoute
import com.filmax.feature.player.common.navigation.TrailerRoute
import com.filmax.feature.profile.tv.navigation.TvDeviceSettingsRoute
import com.filmax.feature.profile.tv.navigation.TvProfileRoute
import com.filmax.feature.search.common.navigation.FilmographyRoute
import com.filmax.feature.search.tv.navigation.TvSearchRoute

/** Маршруты ТВ-графа: разделы и онбординг свои, детали/плеер/подборки — общие с телефоном. */
internal val TvRoutes = GraphRoutes(tabRoot = TvHomeRoute, toRoute = Destination::toTvRoute)

/** Адрес → маршрут ТВ-графа. `TvSearchRoute` отдаёт Каталог, `TvLibraryRoute` — «Моё». */
private fun Destination.toTvRoute(): Any = when (this) {
    Destination.Home -> TvHomeRoute
    Destination.Catalog -> TvSearchRoute
    Destination.Mine -> TvLibraryRoute
    Destination.Profile -> TvProfileRoute
    Destination.Onboarding -> TvOnboardingRoute
    Destination.DeviceSettings -> TvDeviceSettingsRoute
    // Каталог компонентов — debug-экран телефона, на ТВ его нет и открывать нечем.
    Destination.DesignSystem -> error("DesignSystem не существует в ТВ-графе")
    is Destination.Details -> DetailsRoute(itemId)
    is Destination.Player -> PlayerRoute(itemId = itemId, videoId = videoId, season = season)
    is Destination.Trailer -> TrailerRoute(url = url, title = title)
    is Destination.CollectionDetail -> CollectionDetailRoute(collectionId = collectionId, title = title)
    is Destination.Filmography -> FilmographyRoute(name = name, isDirector = isDirector)
    is Destination.BookmarkFolder -> FolderRoute(folderId)
}

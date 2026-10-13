package com.filmax.feature.home.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvErrorState
import com.filmax.core.ui.components.appErrorText
import com.filmax.feature.home.common.HomeEvent
import com.filmax.feature.home.common.HomeScreenModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * ТВ-Главная: карусель «Выбор редакции» и ряды. Поверх общего [HomeScreenModel] — данные те же,
 * что и на телефоне; состав и порядок рядов задаёт модель, экран только рисует их.
 *
 * Плоской ленты «Все» здесь намеренно нет: это работа Каталога с его фильтрами и сортировкой.
 * Бесконечный ряд на пульте — сотни нажатий вправо и ни одного способа найти в нём конкретное.
 */
@Composable
fun TvHomeScreen(
    modifier: Modifier = Modifier,
    screenModel: HomeScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    val offline by screenModel.collectOfflineBannerAsState()
    val appError by screenModel.collectErrorAsState()
    val actions = TvHomeActions(
        hero = HeroActions(
            onPlay = { item ->
                navigator.open(
                    Destination.Player(itemId = item.id, videoId = NO_VIDEO_ID, season = NO_SEASON)
                )
            },
            onDetails = { item -> navigator.open(Destination.Details(item.id)) },
            onToggleWatchLater = { item -> screenModel.dispatch(HomeEvent.ToggleWatchLater(item)) },
            onNext = { screenModel.dispatch(HomeEvent.NextHero) },
        ),
        rails = RailActions(
            onOpenItem = { id -> navigator.open(Destination.Details(id)) },
            onPlay = { itemId, season, videoId ->
                navigator.open(
                    Destination.Player(itemId = itemId, videoId = videoId, season = season)
                )
            },
            onOpenCollection = { id, title ->
                navigator.open(
                    Destination.CollectionDetail(collectionId = id, title = title)
                )
            },
            onLoadMore = { id -> screenModel.dispatch(HomeEvent.LoadMoreRow(id)) },
        ),
        onReload = { screenModel.dispatch(HomeEvent.Load) },
        onOpenCatalog = { navigator.open(Destination.Catalog) },
    )

    Box(modifier.fillMaxSize().background(LocalFxColors.current.background)) {
        val error = appError
        when {
            state.loading -> CircularProgressIndicator(
                color = LocalFxColors.current.accent,
                modifier = Modifier.align(Alignment.Center),
            )

            // Показывать нечего и есть ошибка — объясняемся и даём «Повторить». Модалки, как на
            // телефоне, тут нет: на пульте перекрывать ей пустой экран незачем, а фокусу нужна
            // хоть одна цель. Пришли данные (пусть и из кэша) — ошибку снимает баннер «нет сети».
            error != null && state.isEmpty -> {
                val text = appErrorText(error)
                TvErrorState(title = text.title, message = text.message, onRetry = screenModel::retry)
            }

            else -> TvHomeFeed(state = state, offline = offline, actions = actions)
        }
    }
}

/** Действия Главной одним объектом: hero, ряды и служебные (повтор, переход в каталог). */
internal data class TvHomeActions(
    val hero: HeroActions,
    val rails: RailActions,
    val onReload: () -> Unit,
    val onOpenCatalog: () -> Unit,
)

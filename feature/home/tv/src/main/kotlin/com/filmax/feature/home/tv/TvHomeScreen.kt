package com.filmax.feature.home.tv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.filmax.core.domain.error.AppError
import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvErrorState
import com.filmax.core.tv.designsystem.preview.TvPreviews
import com.filmax.core.ui.components.appErrorText
import com.filmax.feature.home.common.HomeEvent
import com.filmax.feature.home.common.HomeSamples
import com.filmax.feature.home.common.HomeScreenModel
import com.filmax.feature.home.common.HomeSideEffect
import com.filmax.feature.home.common.HomeState
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
    screenModel.collectSideEffect { effect ->
        when (effect) {
            HomeSideEffect.AskFeedback -> navigator.open(Destination.Feedback(FeedbackTrigger.AfterUpdate.name))
        }
    }
    val offline by screenModel.collectOfflineBannerAsState()
    val appError by screenModel.collectErrorAsState()
    val actions = remember(navigator, screenModel) { tvHomeActions(navigator, screenModel) }
    TvHomeScreen(state = state, actions = actions, modifier = modifier, offline = offline, error = appError)
}

/**
 * ТВ-Главная из состояния — без Koin и навигатора: её рисуют превью и UI-тесты. [offline] и
 * [error] — общие флаги базовой модели, а не часть [HomeState].
 */
@Composable
internal fun TvHomeScreen(
    state: HomeState,
    actions: TvHomeActions,
    modifier: Modifier = Modifier,
    offline: Boolean = false,
    error: AppError? = null,
) {
    // Фон — у окна (FilmaxTvTheme): своя заливка экрана закрашивала бы каждый пиксель лишний раз.
    Box(modifier.fillMaxSize()) {
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
                TvErrorState(title = text.title, message = text.message, onRetry = actions.onRetry)
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
    val onRetry: () -> Unit,
    val onOpenCatalog: () -> Unit,
) {
    companion object {
        /** Ничего не делает — для превью. */
        val None = TvHomeActions(
            hero = HeroActions(onPlay = {}, onDetails = {}, onToggleWatchLater = {}, onNext = {}),
            rails = RailActions(onOpenItem = {}, onPlay = { _, _ -> }, onOpenCollection = { _, _ -> }, onLoadMore = {}),
            onReload = {},
            onRetry = {},
            onOpenCatalog = {},
        )
    }
}

/** Куда ведёт Главная. Кнопка hero и ряд «Продолжить» открывают плеер напрямую, минуя детали. */
private fun tvHomeActions(navigator: Navigator, screenModel: HomeScreenModel) = TvHomeActions(
    hero = HeroActions(
        onPlay = { hero -> navigator.open(Destination.Player(itemId = hero.id)) },
        onDetails = { hero -> navigator.open(Destination.Details(hero.id)) },
        onToggleWatchLater = { hero -> screenModel.dispatch(HomeEvent.ToggleWatchLater(hero.id)) },
        onNext = { screenModel.dispatch(HomeEvent.NextHero) },
    ),
    rails = RailActions(
        onOpenItem = { id -> navigator.open(Destination.Details(id)) },
        onPlay = { itemId, video -> navigator.open(Destination.Player(itemId = itemId, video = video)) },
        onOpenCollection = { id, title ->
            navigator.open(Destination.CollectionDetail(collectionId = id, title = title))
        },
        onLoadMore = { id -> screenModel.dispatch(HomeEvent.LoadMoreRow(id)) },
    ),
    onReload = { screenModel.dispatch(HomeEvent.Load) },
    onRetry = screenModel::retry,
    onOpenCatalog = { navigator.open(Destination.Catalog) },
)

private class HomeStatePreviews : PreviewParameterProvider<HomeState> {
    override val values = HomeSamples.all.asSequence()
}

@TvPreviews
@Composable
private fun TvHomeScreenPreview(@PreviewParameter(HomeStatePreviews::class) state: HomeState) {
    FilmaxTvTheme { TvHomeScreen(state = state, actions = TvHomeActions.None) }
}

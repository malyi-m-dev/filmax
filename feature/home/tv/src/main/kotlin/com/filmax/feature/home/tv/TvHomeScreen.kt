package com.filmax.feature.home.tv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvErrorState
import com.filmax.core.ui.components.appErrorText
import com.filmax.feature.home.common.HomeScreenModel
import com.filmax.feature.home.common.HomeSideEffect
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * ТВ-Главная: карусель «Выбор редакции» и ряды. Поверх общего [HomeScreenModel] — данные те же,
 * что и на телефоне; состав и порядок рядов задаёт модель, экран рисует их, шлёт события и
 * исполняет переходы, которые модель отвечает на них.
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

    screenModel.collectSideEffect { effect ->
        when (effect) {
            is HomeSideEffect.OpenDetails -> navigator.open(Destination.Details(effect.itemId))
            is HomeSideEffect.OpenPlayer ->
                navigator.open(Destination.Player(effect.itemId, effect.videoId, effect.season))
            is HomeSideEffect.OpenSection -> navigator.open(Destination.Browse(effect.section.name))
            is HomeSideEffect.OpenCollection ->
                navigator.open(Destination.CollectionDetail(effect.collectionId, effect.title))
            HomeSideEffect.OpenTvPicker -> navigator.open(Destination.TvPicker())
            HomeSideEffect.OpenCatalog -> navigator.open(Destination.Catalog)
            HomeSideEffect.AskFeedback -> navigator.open(Destination.Feedback(FeedbackTrigger.AfterUpdate.name))
        }
    }

    // Фон — у окна (FilmaxTvTheme): своя заливка экрана закрашивала бы каждый пиксель лишний раз.
    Box(modifier.fillMaxSize()) {
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

            else -> TvHomeFeed(state = state, offline = offline, onEvent = screenModel::dispatch)
        }
    }
}

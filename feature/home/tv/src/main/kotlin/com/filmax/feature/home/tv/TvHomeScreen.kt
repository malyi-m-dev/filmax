package com.filmax.feature.home.tv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvErrorState
import com.filmax.core.ui.components.appErrorText
import com.filmax.feature.home.common.HomeScreenModel
import org.koin.androidx.compose.koinViewModel

/**
 * ТВ-Главная: карусель «Выбор редакции» и ряды. Поверх общего [HomeScreenModel] — данные те же,
 * что и на телефоне; состав и порядок рядов задаёт модель, экран только рисует их и шлёт события.
 *
 * Плоской ленты «Все» здесь намеренно нет: это работа Каталога с его фильтрами и сортировкой.
 * Бесконечный ряд на пульте — сотни нажатий вправо и ни одного способа найти в нём конкретное.
 */
@Composable
fun TvHomeScreen(
    modifier: Modifier = Modifier,
    screenModel: HomeScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val offline by screenModel.collectOfflineBannerAsState()
    val appError by screenModel.collectErrorAsState()

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

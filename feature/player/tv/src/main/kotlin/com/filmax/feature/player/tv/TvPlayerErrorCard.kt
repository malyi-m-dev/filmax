package com.filmax.feature.player.tv

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.filmax.core.domain.error.AppError
import com.filmax.core.tv.designsystem.TvMessageCard
import com.filmax.core.ui.components.appErrorText

/**
 * Карточка ошибки поверх кадра TV-плеера. Кнопок нет намеренно: единственный выход из
 * плеера — «Назад». До неё сбой загрузки серии оставлял чёрный экран без индикации
 * (жалоба «следующая серия не запустилась»).
 */
@Composable
internal fun PlayerErrorCard(error: AppError, modifier: Modifier = Modifier) {
    val text = appErrorText(error)
    TvMessageCard(title = text.title, message = text.message, hint = "«Назад» — выйти из плеера", modifier = modifier)
}

/** Плашка «нужна подписка»: без неё kino.pub не отдаст поток, и экран остался бы просто чёрным. */
@Composable
internal fun SubscriptionCard(modifier: Modifier = Modifier) {
    TvMessageCard(
        title = "Нужна подписка",
        message = "Просмотр доступен только с активной подпиской — оформите её в аккаунте kino.pub",
        modifier = modifier,
    )
}

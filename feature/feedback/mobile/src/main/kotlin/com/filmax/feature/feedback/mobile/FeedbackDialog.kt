package com.filmax.feature.feedback.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapeSidePanel
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxButtonStyle
import com.filmax.core.ui.components.FilmaxRatingStars
import com.filmax.core.ui.components.FilmaxTextArea
import com.filmax.feature.feedback.common.FeedbackEvent
import com.filmax.feature.feedback.common.FeedbackScreenModel
import com.filmax.feature.feedback.common.FeedbackSideEffect
import com.filmax.feature.feedback.common.FeedbackState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private const val COMMENT_LINES = 4

/** Окно оценки поверх экрана: звёзды, комментарий системной клавиатурой и отправка. */
@Composable
fun FeedbackDialog(
    screenModel: FeedbackScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    screenModel.collectSideEffect { effect ->
        when (effect) {
            FeedbackSideEffect.Close -> navigator.back()
        }
    }
    Surface(
        shape = ShapeSidePanel,
        color = FilmaxTheme.palette.background,
        contentColor = FilmaxColors.Text,
        border = BorderStroke(1.dp, FilmaxColors.PanelBorder),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.sent) ThankYou(screenModel::dispatch) else Form(state, screenModel::dispatch)
        }
    }
}

@Composable
private fun Form(state: FeedbackState, dispatch: (FeedbackEvent) -> Unit) {
    Text(state.title, style = FilmaxType.sheetTitle, color = FilmaxColors.Text)
    FilmaxRatingStars(rating = state.rating, onRate = { dispatch(FeedbackEvent.Rate(it)) })
    FilmaxTextArea(
        value = state.comment,
        onValueChange = { dispatch(FeedbackEvent.Comment(it)) },
        placeholder = "Что нравится, чего не хватает — по желанию",
        minLines = COMMENT_LINES,
    )
    state.error?.let { Text(it, style = FilmaxType.meta, color = FilmaxColors.Error) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilmaxButton(
            text = "Не сейчас",
            onClick = { dispatch(FeedbackEvent.Dismiss) },
            style = FilmaxButtonStyle.Secondary,
            modifier = Modifier.weight(1f),
        )
        FilmaxButton(
            text = if (state.sending) "Отправляем…" else "Отправить",
            onClick = { dispatch(FeedbackEvent.Send) },
            enabled = state.rating > 0 && !state.sending,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ThankYou(dispatch: (FeedbackEvent) -> Unit) {
    Text("Спасибо!", style = FilmaxType.sheetTitle, color = FilmaxColors.Text)
    Text("Отзыв отправлен — мы его прочитаем.", style = FilmaxType.body, color = FilmaxColors.TextSecondary)
    FilmaxButton(text = "Готово", onClick = { dispatch(FeedbackEvent.Dismiss) }, modifier = Modifier.fillMaxWidth())
}

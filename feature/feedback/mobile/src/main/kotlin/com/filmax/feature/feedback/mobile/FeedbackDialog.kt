package com.filmax.feature.feedback.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxRatingStars
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
    Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.sent) ThankYou(screenModel::dispatch) else Form(state, screenModel::dispatch)
        }
    }
}

@Composable
private fun Form(state: FeedbackState, dispatch: (FeedbackEvent) -> Unit) {
    Text(state.title, style = MaterialTheme.typography.headlineSmall)
    FilmaxRatingStars(rating = state.rating, onRate = { dispatch(FeedbackEvent.Rate(it)) })
    OutlinedTextField(
        value = state.comment,
        onValueChange = { dispatch(FeedbackEvent.Comment(it)) },
        placeholder = { Text("Что нравится, чего не хватает — по желанию") },
        minLines = COMMENT_LINES,
        modifier = Modifier.fillMaxWidth(),
    )
    state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { dispatch(FeedbackEvent.Dismiss) }) { Text("Не сейчас") }
        Button(onClick = { dispatch(FeedbackEvent.Send) }, enabled = state.rating > 0 && !state.sending) {
            Text(if (state.sending) "Отправляем…" else "Отправить")
        }
    }
}

@Composable
private fun ThankYou(dispatch: (FeedbackEvent) -> Unit) {
    Text("Спасибо!", style = MaterialTheme.typography.headlineSmall)
    Text("Отзыв отправлен — мы его прочитаем.", style = MaterialTheme.typography.bodyMedium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Button(onClick = { dispatch(FeedbackEvent.Dismiss) }) { Text("Готово") }
    }
}

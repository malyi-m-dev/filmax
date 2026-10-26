// Окно оценки на ТВ: звёзды, комментарий с экранной клавиатуры, «Отправить» и «Не сейчас».
package com.filmax.feature.feedback.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.input.KeyboardLayout
import com.filmax.core.presentation.input.keyboardRows
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvButtonStyle
import com.filmax.core.tv.designsystem.TvKeyboard
import com.filmax.core.tv.designsystem.TvKeyboardActions
import com.filmax.core.tv.designsystem.TvKeyboardLayouts
import com.filmax.core.tv.designsystem.TvKeyboardStyle
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvRatingStars
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.feature.feedback.common.FeedbackEvent
import com.filmax.feature.feedback.common.FeedbackInput
import com.filmax.feature.feedback.common.FeedbackScreenModel
import com.filmax.feature.feedback.common.FeedbackSideEffect
import com.filmax.feature.feedback.common.FeedbackState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val FormWidth = 340.dp
private val ColumnGap = 40.dp
private val FieldMinHeight = 96.dp
private const val FIELD_FILL_ALPHA = 0.06f

/** Окно оценки поверх тёмного фона. «Назад» закрывает его, как и «Не сейчас». */
@Composable
fun TvFeedbackScreen(screenModel: FeedbackScreenModel = koinViewModel(), navigator: Navigator = koinInject()) {
    val state by screenModel.collectAsState()
    screenModel.collectSideEffect { effect ->
        when (effect) {
            FeedbackSideEffect.Close -> navigator.back()
        }
    }
    Box(
        Modifier.fillMaxSize().background(LocalFxColors.current.background),
        contentAlignment = Alignment.Center,
    ) {
        if (state.sent) ThankYou(screenModel::dispatch) else FeedbackForm(state, screenModel::dispatch)
    }
}

@Composable
private fun FeedbackForm(state: FeedbackState, onEvent: (FeedbackEvent) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = TvMetrics.SafeHorizontal),
        horizontalArrangement = Arrangement.spacedBy(ColumnGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RatingColumn(state, onEvent, Modifier.width(FormWidth))
        CommentKeyboard(state, onEvent, Modifier.weight(1f))
    }
}

/** Заголовок, звёзды, комментарий и кнопки. Фокус при открытии — на первой звезде. */
@Composable
private fun RatingColumn(state: FeedbackState, onEvent: (FeedbackEvent) -> Unit, modifier: Modifier) {
    val colors = LocalFxColors.current
    val firstStar = remember { FocusRequester() }
    val send = remember { FocusRequester() }
    var focusPlaced by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("ОТЗЫВ О ПРИЛОЖЕНИИ", style = FxType.overline, color = colors.accentText)
        Text(state.title, style = FxType.screenTitle, color = colors.text)
        TvRatingStars(
            rating = state.rating,
            onRate = { onEvent(FeedbackEvent.Rate(it)) },
            firstStar = firstStar,
            exitDown = send,
            // Звёзды разместились — фокус на первую, один раз.
            modifier = Modifier.onPlaced { if (!focusPlaced) focusPlaced = firstStar.requestFocus() },
        )
        CommentField(state.comment)
        state.error?.let { Text(it, style = FxType.meta, color = colors.error) }
        Row(Modifier.tvFocusGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvButton(
                text = if (state.sending) "Отправляем…" else "Отправить",
                onClick = { onEvent(FeedbackEvent.Send) },
                focusRequester = send,
            )
            TvButton("Не сейчас", onClick = { onEvent(FeedbackEvent.Dismiss) }, style = TvButtonStyle.Secondary)
        }
    }
}

/** Комментарий — не фокусируется: набирают его клавиатурой справа. */
@Composable
private fun CommentField(comment: String) {
    val colors = LocalFxColors.current
    Text(
        comment.ifEmpty { "Комментарий — по желанию: что нравится, чего не хватает" },
        style = FxType.body,
        color = if (comment.isEmpty()) colors.textDim else colors.text,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FieldMinHeight)
            .background(Color.White.copy(alpha = FIELD_FILL_ALPHA), TvMetrics.PanelShape)
            .border(1.dp, colors.outline, TvMetrics.PanelShape)
            .padding(14.dp),
    )
}

@Composable
private fun CommentKeyboard(state: FeedbackState, onEvent: (FeedbackEvent) -> Unit, modifier: Modifier) {
    val key = { input: FeedbackInput -> onEvent(FeedbackEvent.Key(input)) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TvKeyboard(
            rows = keyboardRows(state.layout),
            actions = TvKeyboardActions(
                onKey = { key(FeedbackInput.Letter(it)) },
                onSpace = { key(FeedbackInput.Space) },
                onDelete = { key(FeedbackInput.Erase) },
            ),
            // Панель тянется по ширине колонки: сеткой клавиатура не влезала рядом с формой.
            style = TvKeyboardStyle.Panel,
            modifier = Modifier.fillMaxWidth(),
        )
        TvKeyboardLayouts(
            labels = KeyboardLayout.entries.map { it.label },
            selected = state.layout.ordinal,
            onSelect = { key(FeedbackInput.Layout(KeyboardLayout.entries[it])) },
        )
    }
}

/** Отзыв ушёл: благодарность и «Готово». */
@Composable
private fun ThankYou(onEvent: (FeedbackEvent) -> Unit) {
    val colors = LocalFxColors.current
    val done = remember { FocusRequester() }
    var focusPlaced by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Спасибо!", style = FxType.screenTitle, color = colors.text)
        Text("Отзыв отправлен — мы его прочитаем.", style = FxType.body, color = colors.textSecondary)
        TvButton(
            "Готово",
            onClick = { onEvent(FeedbackEvent.Dismiss) },
            focusRequester = done,
            modifier = Modifier.onPlaced { if (!focusPlaced) focusPlaced = done.requestFocus() },
        )
    }
}

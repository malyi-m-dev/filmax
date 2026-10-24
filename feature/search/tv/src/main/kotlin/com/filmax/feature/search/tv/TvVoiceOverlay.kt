// Голосовой поиск (макет S25): оверлей поверх размытого каталога — микрофон с кольцами,
// эквалайзер громкости и текст по ходу речи; по паузе — поиск, «Назад» — отмена.
package com.filmax.feature.search.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvKeyHint
import com.filmax.core.tv.designsystem.TvKeyHints
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvMicButton
import com.filmax.core.tv.designsystem.TvMicPulse
import com.filmax.core.tv.designsystem.TvSearchField
import com.filmax.core.tv.designsystem.TvSearchFieldMode
import com.filmax.core.tv.designsystem.TvVoiceLevel
import com.filmax.core.tv.designsystem.TvVoiceTranscript
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.core.ui.components.VoiceSearchController
import com.filmax.core.ui.components.rememberInAppVoiceSearch
import com.filmax.core.ui.components.voiceErrorText
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

/** Каталог за оверлеем размыт слабее, чем под панелью: его ещё видно, но он не спорит с текстом. */
internal val VoiceBackdropBlur = 6.dp

/** Отступ блока слушания от верха — круг микрофона на трети экрана, как в макете. */
private val ListeningTop = 150.dp
private val HintsBottom = 22.dp
private const val WASH_CENTER_Y = 0.58f
private const val WASH_RADIUS = 0.6f

/**
 * Голосовой поиск экрана: распознаватель и то, чем кончилось слушание. Дала речь запрос — фокус
 * ведёт открывшийся поиск; закрылось без запроса — фокус возвращается на микрофон.
 */
@Stable
internal class CatalogVoice(val controller: VoiceSearchController, private val outcome: VoiceOutcome) {
    /** Слушать заново: прошлый ответ к этому слушанию отношения не имеет. */
    fun start() {
        outcome.answered = false
        controller.start()
    }

    internal val answered: Boolean get() = outcome.answered
}

/** Дал ли голос запрос — пишется из колбэка распознавателя, читается при закрытии оверлея. */
internal class VoiceOutcome {
    var answered = false
}

@Composable
internal fun rememberCatalogVoice(onQuery: (String) -> Unit): CatalogVoice {
    val currentOnQuery by rememberUpdatedState(onQuery)
    val outcome = remember { VoiceOutcome() }
    val controller = rememberInAppVoiceSearch { spoken ->
        outcome.answered = true
        currentOnQuery(spoken)
    }
    return remember(controller) { CatalogVoice(controller, outcome) }
}

/**
 * Оверлей слушания поверх экрана; сам экран под ним размыт ([VoiceBackdropBlur]). Пока оверлей
 * открыт, фокус заперт в нём: пульт не должен листать размытый каталог. «Назад» — отмена.
 */
@Composable
internal fun TvVoiceOverlay(voice: CatalogVoice, focus: CatalogFocus) {
    val controller = voice.controller
    ReturnFocusAfterVoice(voice, focus)
    BackHandler(enabled = controller.active) { controller.cancel() }
    AnimatedVisibility(
        visible = controller.active,
        enter = fadeIn(FxMotion.fade()),
        exit = fadeOut(FxMotion.panelOut()),
    ) {
        VoiceListening(controller)
    }
}

/** Оверлей закрылся без запроса (отмена, тишина, ошибка) — фокус обратно на микрофон. */
@Composable
private fun ReturnFocusAfterVoice(voice: CatalogVoice, focus: CatalogFocus) {
    LaunchedEffect(voice) {
        snapshotFlow { voice.controller.active }
            .drop(1)
            .filter { active -> !active }
            .collect { if (!voice.answered) focus.moveTo(CatalogTarget.MIC) }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun VoiceListening(controller: VoiceSearchController) {
    val trap = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        trap.requestFocus()
    }
    Box(
        Modifier
            .fillMaxSize()
            .voiceWash()
            .focusRequester(trap)
            .focusProperties { exit = { FocusRequester.Cancel } }
            .focusable(),
    ) {
        VoiceSearchBar(controller.partialText)
        ListeningColumn(controller, Modifier.align(Alignment.TopCenter).padding(top = ListeningTop))
        TvKeyHints(
            hints = voiceHints(controller.error != null),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = HintsBottom),
        )
    }
}

/** Строка поиска над размытием: набранное голосом видно там же, где его увидит поиск. */
@Composable
private fun VoiceSearchBar(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = TvMetrics.SectionStart, end = TvMetrics.SectionEnd, top = TvMetrics.ContentTop),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TvSearchField(
            query = text,
            placeholder = "",
            onClick = {},
            mode = TvSearchFieldMode.Dictation,
            modifier = Modifier.weight(1f),
        )
        // Копия кнопки — показать, откуда пришёл голос; нажимать её незачем, фокус ей не нужен.
        TvMicButton(onClick = {}, listening = true, modifier = Modifier.focusProperties { canFocus = false })
    }
}

/** Круг микрофона, «Слушаю» с эквалайзером и текст речи — или ошибка, если слушать не вышло. */
@Composable
private fun ListeningColumn(controller: VoiceSearchController, modifier: Modifier) {
    val colors = LocalFxColors.current
    val error = controller.error
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TvMicPulse(error = error != null)
        if (error != null) {
            Text(
                voiceErrorText(error),
                style = FxType.rowTitle,
                color = colors.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 28.dp),
            )
            return@Column
        }
        // Тексты поднимаются вслед за кругом, по очереди: «Слушаю», речь, подсказка.
        Text(
            "Слушаю".uppercase(),
            style = FxType.overline.copy(fontSize = ListeningFontSize),
            color = colors.accentText,
            modifier = Modifier.padding(top = 28.dp).fxEnter(LISTENING_ENTER),
        )
        TvVoiceLevel(level = { controller.level }, modifier = Modifier.padding(top = 12.dp))
        TvVoiceTranscript(
            confirmed = controller.transcript.confirmed,
            hypothesis = controller.transcript.hypothesis,
            modifier = Modifier.padding(top = 16.dp).fxEnter(TRANSCRIPT_ENTER),
        )
        // Поиск ищет по названию — и подсказка просит название, а не жанр или актёра.
        Text(
            "Скажите название фильма или сериала — например, «Хроники Пограничья»",
            style = HintStyle,
            color = colors.text.copy(alpha = HINT_ALPHA),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp).fxEnter(HINT_ENTER),
        )
    }
}

/** «Слушаю» — надзаголовок чуть крупнее обычного (12sp) и цветом акцента: это состояние, а не раздел. */
private val ListeningFontSize = 12.sp
private val HintStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
private const val HINT_ALPHA = 0.6f

/** Порядок подъёма текстов (шаг fxEnter — 60 мс): ≈ 100, 200 и 260 мс макета после круга. */
private const val LISTENING_ENTER = 2
private const val TRANSCRIPT_ENTER = 3
private const val HINT_ENTER = 4

/** Подсветка палитры по центру и затемнение — поверх размытого каталога. */
@Composable
private fun Modifier.voiceWash(): Modifier {
    val wash = LocalFxColors.current.wash
    return drawBehind {
        drawRect(VoiceDim)
        drawRect(
            Brush.radialGradient(
                colors = listOf(wash, Color.Transparent),
                center = Offset(size.width / 2, size.height * WASH_CENTER_Y),
                radius = size.width * WASH_RADIUS,
            ),
        )
    }
}

/** Затемнение оверлея поверх затемнения размытого экрана — макет: rgba(6,5,12,.55). */
private val VoiceDim = Color(0x8C06050C)

/** Подсказки клавиш: слушание — «отмена» и «пауза — искать», ошибка — только «закрыть». */
private fun voiceHints(failed: Boolean): List<TvKeyHint> {
    val back = TvKeyHint(listOf(Icons.AutoMirrored.Rounded.ArrowBack), if (failed) "закрыть" else "отмена")
    if (failed) return listOf(back)
    return listOf(back, TvKeyHint(listOf(Icons.Rounded.Timer), "пауза 1.5 с — искать"))
}

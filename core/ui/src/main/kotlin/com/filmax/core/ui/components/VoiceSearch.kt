package com.filmax.core.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat

/**
 * Голосовой ввод БЕЗ стороннего экрана: слушаем микрофон через [SpeechRecognizer] прямо в
 * приложении, состояние (идёт слушание + частичный текст) отдаём Compose-стейтом —
 * его рисует [VoiceListeningDialog]. Финальная фраза уходит в onResult.
 *
 * Порядок запуска в [start]: нет разрешения на микрофон → системный запрос (после согласия
 * слушание стартует само); сервис распознавания недоступен → фолбэк на внешний
 * RecognizerIntent — хуже, но лучше, чем молчащая кнопка.
 */
// Набор методов диктует интерфейс RecognitionListener (8 обязательных колбэков) — дробить
// контроллер из-за пустых заглушек незачем.
@Suppress("TooManyFunctions")
@Stable
class VoiceSearchController internal constructor(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val requestPermission: () -> Unit,
    private val fallback: () -> Unit,
) : RecognitionListener {

    /** Идёт ли слушание — на нём держится [VoiceListeningDialog]. */
    var listening by mutableStateOf(false)
        private set

    /** Частичный распознанный текст — «эхо» того, что уже услышано. */
    var partialText by mutableStateOf("")
        private set

    private var recognizer: SpeechRecognizer? = null

    /** Точка входа кнопки «Голос». */
    fun start() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        when {
            !granted -> requestPermission()
            !SpeechRecognizer.isRecognitionAvailable(context) -> fallback()
            else -> startListening()
        }
    }

    /** Останов без результата (закрытие диалога, «Назад»). */
    fun cancel() {
        listening = false
        recognizer?.cancel()
    }

    internal fun startListening() {
        val speech = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { created ->
            created.setRecognitionListener(this)
            recognizer = created
        }
        partialText = ""
        listening = true
        speech.startListening(recognizeIntent())
    }

    internal fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    override fun onPartialResults(partialResults: Bundle?) {
        bestMatch(partialResults)?.let { partialText = it }
    }

    override fun onResults(results: Bundle?) {
        listening = false
        bestMatch(results)?.takeIf { it.isNotBlank() }?.let(onResult)
    }

    override fun onError(error: Int) {
        // Любая ошибка (тишина, таймаут, сеть) просто закрывает слушание — без модалок:
        // пользователь видит, что плашка исчезла, и может нажать «Голос» ещё раз.
        listening = false
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun bestMatch(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
}

/** Создаёт контроллер голосового ввода; жизненный цикл распознавателя привязан к композиции. */
@Composable
fun rememberInAppVoiceSearch(onResult: (String) -> Unit): VoiceSearchController {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val fallback = rememberExternalVoiceSearch { spoken -> currentOnResult(spoken) }

    // Держатель нужен, чтобы колбэк разрешения мог дотянуться до контроллера, который
    // создаётся строкой ниже и сам ссылается на launcher.
    val holder = remember { mutableStateOf<VoiceSearchController?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) holder.value?.startListening()
    }

    val controller = remember {
        VoiceSearchController(
            context = context,
            onResult = { spoken -> currentOnResult(spoken) },
            requestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            fallback = fallback,
        ).also { holder.value = it }
    }
    DisposableEffect(controller) {
        onDispose { controller.destroy() }
    }
    return controller
}

/**
 * Плашка слушания по центру экрана: микрофон, «Говорите…» и частичный текст. Рисуется через
 * [Dialog] — всплывает над всем окном из любого места дерева, а «Назад»/тап мимо отменяют
 * слушание. Ставится рядом с местом использования контроллера.
 */
@Composable
fun VoiceListeningDialog(controller: VoiceSearchController) {
    if (!controller.listening) return
    Dialog(onDismissRequest = controller::cancel) {
        Column(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 420.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Text(
                "Говорите…",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                controller.partialText.ifBlank { "Назовите фильм или сериал" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/**
 * Фолбэк: системный распознаватель отдельным экраном (RecognizerIntent). Используется только
 * когда [SpeechRecognizer] на устройстве недоступен.
 */
@Composable
private fun rememberExternalVoiceSearch(onResult: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let(onResult)
        }
    }
    return {
        val intent = recognizeIntent().apply {
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Назовите фильм или сериал")
        }
        runCatching { launcher.launch(intent) }
    }
}

private fun recognizeIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
}

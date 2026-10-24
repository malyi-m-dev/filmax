package com.filmax.core.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Пауза после речи, после которой фраза считается сказанной. Своя, а не только подсказка
 * распознавателю: подсказку EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS многие сервисы
 * распознавания на ТВ игнорируют и ждут по 3–5 секунд.
 */
private const val SILENCE_MILLIS = 1_500L

/** Громкость из onRmsChanged, дБ: ниже — тишина комнаты, выше — уверенная речь у пульта. */
private const val RMS_QUIET_DB = -2f
private const val RMS_LOUD_DB = 10f

/** Ключ хвоста, который распознаватель Google помечает неустойчивым (нет в публичном API). */
private const val EXTRA_UNSTABLE_TEXT = "android.speech.extra.UNSTABLE_TEXT"

/** Почему голосовой ввод не состоялся — то, что стоит показать зрителю, а не просто закрыть. */
enum class VoiceSearchError {
    /** Нет сети или сервис распознавания не ответил. */
    NETWORK,

    /** Зритель не дал доступ к микрофону. */
    PERMISSION,
}

/**
 * Голосовой ввод БЕЗ стороннего экрана: слушаем микрофон через [SpeechRecognizer] прямо в
 * приложении и отдаём Compose-стейтом всё, что нужно нарисовать слушание: идёт ли оно, текст по
 * ходу речи ([transcript]), громкость ([level]) и ошибку ([error]). Телефон рисует его
 * [VoiceListeningOverlay], ТВ — своим оверлеем. Финальная фраза уходит в onResult.
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

    /** Идёт ли слушание — на нём держится [VoiceListeningOverlay]. */
    var listening by mutableStateOf(false)
        private set

    /** Текст по ходу речи: подтверждённая часть и гипотеза. */
    var transcript by mutableStateOf(VoiceTranscript())
        private set

    /** Частичный распознанный текст целиком — «эхо» того, что уже услышано. */
    val partialText: String get() = transcript.text

    /** Громкость речи 0..1 — для эквалайзера; вне слушания 0. */
    var level by mutableFloatStateOf(0f)
        private set

    /** Ошибка, которую стоит показать; тишина и «не расслышал» ошибкой не считаются. */
    var error by mutableStateOf<VoiceSearchError?>(null)
        private set

    /** Слушание на экране: идёт или кончилось ошибкой, которую зритель ещё не закрыл. */
    val active: Boolean get() = listening || error != null

    private var recognizer: SpeechRecognizer? = null
    private val silenceTimer = Handler(Looper.getMainLooper())
    private val stopOnSilence = Runnable { recognizer?.stopListening() }

    /** Точка входа кнопки «Голос». */
    fun start() {
        error = null
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        when {
            !granted -> requestPermission()
            !SpeechRecognizer.isRecognitionAvailable(context) -> fallback()
            else -> startListening()
        }
    }

    /** Останов без результата (закрытие диалога, «Назад»); заодно закрывает показанную ошибку. */
    fun cancel() {
        finish()
        error = null
        recognizer?.cancel()
    }

    internal fun startListening() {
        val speech = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { created ->
            created.setRecognitionListener(this)
            recognizer = created
        }
        transcript = VoiceTranscript()
        error = null
        listening = true
        speech.startListening(recognizeIntent())
    }

    /** Зритель отказал в микрофоне — показываем, почему голос не работает, а не молчим. */
    internal fun permissionDenied() {
        error = VoiceSearchError.PERMISSION
    }

    internal fun destroy() {
        silenceTimer.removeCallbacks(stopOnSilence)
        recognizer?.destroy()
        recognizer = null
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val heard = bestMatch(partialResults) ?: return
        val unstable = partialResults?.getStringArrayList(EXTRA_UNSTABLE_TEXT)?.firstOrNull()
        val next = nextTranscript(transcript.text, heard, unstable)
        val changed = next.text != transcript.text
        transcript = next
        // Текст изменился — речь идёт; не меняется SILENCE_MILLIS — фраза сказана.
        if (changed) {
            silenceTimer.removeCallbacks(stopOnSilence)
            silenceTimer.postDelayed(stopOnSilence, SILENCE_MILLIS)
        }
    }

    override fun onResults(results: Bundle?) {
        finish()
        bestMatch(results)?.takeIf { it.isNotBlank() }?.let(onResult)
    }

    override fun onError(error: Int) {
        finish()
        // Тишина и «не расслышал» просто закрывают слушание — зритель видит, что оверлей исчез, и
        // может нажать «Голос» ещё раз. Сеть и разрешение — объясняем: повтор тут не поможет.
        this.error = when (error) {
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER,
            -> VoiceSearchError.NETWORK
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceSearchError.PERMISSION
            else -> null
        }
    }

    override fun onRmsChanged(rmsdB: Float) {
        level = voiceLevel(rmsdB)
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun finish() {
        silenceTimer.removeCallbacks(stopOnSilence)
        listening = false
        level = 0f
    }

    private fun bestMatch(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
}

/** Громкость onRmsChanged (дБ) в долю 0..1 для эквалайзера. */
internal fun voiceLevel(rmsDb: Float): Float =
    ((rmsDb - RMS_QUIET_DB) / (RMS_LOUD_DB - RMS_QUIET_DB)).coerceIn(0f, 1f)

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
        if (granted) holder.value?.startListening() else holder.value?.permissionDenied()
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
    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_MILLIS)
}

/** Почему голос не сработал — словами, без кодов ошибок распознавателя. Одна формулировка на телефон и ТВ. */
fun voiceErrorText(error: VoiceSearchError): String = when (error) {
    VoiceSearchError.NETWORK -> "Нет связи с сервисом распознавания — проверьте интернет"
    VoiceSearchError.PERMISSION -> "Нет доступа к микрофону — разрешите его в настройках"
}

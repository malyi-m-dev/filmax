package com.filmax.core.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Голосовой поиск через системный распознаватель речи (Google). Возвращает лямбду-запуск:
 * по вызову открывается системный UI распознавания, а распознанный текст приходит в [onResult].
 * Разрешение на микрофон спрашивает сам системный UI — манифест-permission не нужен.
 *
 * Работает и на телефоне, и на Android TV (там голос идёт через пульт/ассистента).
 */
@Composable
fun rememberVoiceSearch(onResult: (String) -> Unit): () -> Unit {
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
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Назовите фильм или сериал")
        }
        runCatching { launcher.launch(intent) }
    }
}

package com.filmax.data.fake.fixtures

import com.filmax.core.domain.catalog.model.AudioTrack
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.Trailer
import com.filmax.core.domain.catalog.model.VideoFile
import com.filmax.data.fake.FakeIds

// ─────────────────────────────── Видео ───────────────────────────────
// Публичные тестовые потоки: доступны без авторизации и не режутся DPI, как CDN kino.pub.
// Media3 собирает HLS-аудио в группы по NAME, поэтому важны не дорожки манифеста, а их имена:
// у Apple «advanced» три дорожки с одним именем — для плеера это одна группа.

/** Tears of Steel: две аудиогруппы (English, Italian). 12:14. */
private const val TEARS_MULTI_LANG_HLS =
    "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel-multi-lang.ism/.m3u8"

/** Apple bipbop 16×9: дорожка в потоке плюс альтернативная — тоже две группы. */
private const val APPLE_BIPBOP_HLS =
    "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"

/** Mux Big Buck Bunny: одна аудиогруппа. 10:34. */
private const val MUX_BUNNY_HLS = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

/** Apple bipbop advanced: три дорожки, но с одним именем — одна аудиогруппа. */
private const val APPLE_ADVANCED_HLS =
    "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8"

/** Прогрессивный MP4 — второй вариант доставки: на него плеер откатывается сразу после hls4. */
private const val BUNNY_MP4 = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"

private val UHD_QUALITIES = listOf("2160p", "1080p", "720p", "480p")
private val HD_QUALITIES = listOf("1080p", "720p", "480p")

/**
 * Тестовый поток тайтла. Число аудиогрупп совпадает с числом озвучек в метаданных — и у основного
 * варианта, и у запасного: плеер подписывает группы по `audios[].index`, и группа без метаданных
 * выглядела бы багом приложения, а не фикстуры. Запасной вариант — другой поток: так при
 * недоступном CDN видно, что плеер откатился. [lengthSeconds] — длина основного варианта.
 */
internal enum class TestStream(val hls4: String, val hls: String, val lengthSeconds: Int) {
    /** Две озвучки — в плеере есть меню озвучек. */
    MULTI_AUDIO(hls4 = TEARS_MULTI_LANG_HLS, hls = APPLE_BIPBOP_HLS, lengthSeconds = 734),

    /** Одна дорожка — меню озвучек скрыто, как у русских тайтлов на живом API. */
    SINGLE_AUDIO(hls4 = MUX_BUNNY_HLS, hls = APPLE_ADVANCED_HLS, lengthSeconds = 634),
}

internal fun streamFor(voices: List<AudioTrack>): TestStream =
    if (voices.size > 1) TestStream.MULTI_AUDIO else TestStream.SINGLE_AUDIO

/** Файлы по качествам — от лучшего к худшему: «Авто» в плеере берёт первый. */
internal fun videoFiles(stream: TestStream, uhd: Boolean): List<VideoFile> =
    (if (uhd) UHD_QUALITIES else HD_QUALITIES).map { quality ->
        VideoFile(quality = quality, hls4 = stream.hls4, hls = stream.hls, http = BUNNY_MP4)
    }

internal fun trailerFor(itemId: Int): Trailer = Trailer(id = "trailer-$itemId", url = MUX_BUNNY_HLS)

// ─────────────────────────────── Озвучки ───────────────────────────────
// Студии выдуманы. Подписи в плеере собираются как у kino.pub: «1. Русский · Дубляж · Северный дубляж».

private const val RUSSIAN = "rus"
private const val ORIGINAL = "Оригинал"
private const val STEREO = 2

/** Озвучка: язык, тип и студия (у оригинала студии нет). */
private data class Voice(val lang: String, val type: String, val author: String?)

private fun russian(type: String, author: String): Voice = Voice(RUSSIAN, type, author)

private fun original(lang: String): Voice = Voice(lang, ORIGINAL, author = null)

/**
 * Озвучки тайтла. У русского и у концерта — одна оригинальная дорожка: концерты не переводят. У
 * иностранного — две: у фильмов, аниме и документалок русская плюс оригинал, у сериалов — две
 * русские разных студий. Последний случай и объясняет, зачем меню показывает студию и зачем плеер
 * помнит выбор на тайтл: по языку такие озвучки не отличить.
 */
internal fun voicesFor(spec: TitleSpec): List<AudioTrack> {
    val language = spec.country.language
    val isAnime = spec.genres.any { it.id == FakeIds.ANIME_GENRE }
    val isDocumentary = spec.type == ItemType.DOCUMENTARY || spec.type == ItemType.DOCUMENTARY_MOVIE
    val voices = when {
        language == RUSSIAN || spec.type == ItemType.CONCERT -> listOf(original(language))
        isAnime -> listOf(russian("Многоголосый", "Сакура Саунд"), original(language))
        isDocumentary -> listOf(russian("Закадровый", "Северный дубляж"), original(language))
        spec.type == ItemType.MOVIE -> listOf(russian("Дубляж", "Северный дубляж"), original(language))
        else -> listOf(russian("Многоголосый", "Тихий Омут"), russian("Многоголосый", "Ламповая озвучка"))
    }
    return voices.mapIndexed { position, voice -> voice.toAudio(index = position + 1) }
}

/** Дорожка в терминах kino.pub: [index] — порядок в HLS-манифесте, с единицы. */
private fun Voice.toAudio(index: Int): AudioTrack = AudioTrack(
    id = index,
    index = index,
    lang = lang,
    title = author ?: type,
    channels = STEREO,
    voiceType = type,
    voiceAuthor = author,
)

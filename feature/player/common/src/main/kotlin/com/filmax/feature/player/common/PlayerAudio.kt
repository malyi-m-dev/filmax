package com.filmax.feature.player.common

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import com.filmax.core.domain.catalog.model.AudioTrack
import com.filmax.core.domain.catalog.model.audioLanguageName

/**
 * Озвучки плеера: варианты для меню, выбор дорожки и память озвучки на тайтл. Дорожки известны
 * только после разбора манифеста ([onTracksChanged]), а подписи и ключ озвучки — из `audios[]`
 * ответа API. Список — ВСЕ группы, а не уникальные языки: у тайтла обычно несколько озвучек одного
 * языка (дубляж, многоголоски разных студий, оригинал), и клиент kino.pub показывает их все.
 */
internal class PlayerAudio(private val player: Player) {

    /** Аудиогруппы последнего разбора — по ним выбор делает точечный override. */
    private var groups: List<Tracks.Group> = emptyList()

    /**
     * Озвучка, выбранная для этого тайтла (язык|тип|студия). Читается при загрузке и применяется к
     * КАЖДОМУ разбору дорожек: так следующая серия сериала стартует с той же студией, а смена
     * качества не сбрасывает выбор. Обновляется при ручном выборе дорожки.
     */
    var savedVoiceKey: String? = null

    /**
     * Дорожки после разбора манифеста. Запомненная озвучка тайтла ставится override'ом на группу с
     * тем же ключом; не нашлась (у серии другой набор озвучек) — остаётся выбор плеера.
     */
    fun onTracksChanged(tracks: Tracks, apiAudios: List<AudioTrack>): AudioChoice {
        groups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        val options = groups.mapIndexed { index, group -> audioOption(index, group, apiAudios) }
        val savedIndex = savedVoiceKey?.let { key ->
            groups.indices.firstOrNull { voiceKey(it, groups[it], apiAudios) == key }
        }
        if (savedIndex != null && !groups[savedIndex].isSelected) override(groups[savedIndex])
        val selectedIndex = savedIndex ?: groups.indexOfFirst { it.isSelected }
        return AudioChoice(options, selectedIndex)
    }

    /**
     * Ручной выбор: точечный override на группу — предпочитаемый ЯЗЫК не различил бы несколько
     * русских озвучек разных студий. Возвращает ключ озвучки, чтобы запомнить её на весь тайтл;
     * null — такой группы уже нет.
     */
    fun select(option: AudioOption, apiAudios: List<AudioTrack>): String? {
        val group = groups.getOrNull(option.groupIndex) ?: return null
        override(group)
        return voiceKey(option.groupIndex, group, apiAudios).also { savedVoiceKey = it }
    }

    private fun override(group: Tracks.Group) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
            .build()
    }
}

/** Озвучки после разбора: [options] — все группы, [selectedIndex] — выбранная (-1 — никакая). */
internal data class AudioChoice(val options: List<AudioOption>, val selectedIndex: Int) {

    /** Меню показываем только при выборе из нескольких. */
    val menu: List<AudioOption> get() = if (options.size > 1) options else emptyList()

    val current: String
        get() = options.getOrNull(selectedIndex)?.label ?: options.firstOrNull()?.label.orEmpty()
}

/**
 * Подпись дорожки: «2. Русский · Многоголосый · BaibaKo» — как в оригинальном клиенте kino.pub.
 * Метаданные берём из `audios[]` ответа API, сопоставляя с группой Media3 по порядку
 * (`audios[].index` 1-based = порядок дорожек в HLS-манифесте): сам манифест kino.pub кладёт в NAME
 * только код языка, и по нему озвучки неотличимы. Номер в начале гарантирует уникальность подписи,
 * даже если у двух озвучек совпали студия и тип.
 */
private fun audioLabel(groupIndex: Int, group: Tracks.Group, apiAudios: List<AudioTrack>): String {
    val meta = apiAudios.firstOrNull { it.index == groupIndex + 1 }
    val language = meta?.lang ?: group.getTrackFormat(0).language
    val parts = buildList {
        add(audioLanguageName(language))
        meta?.voiceType?.let { add(it) }
        meta?.voiceAuthor?.let { add(it) }
    }.distinct()
    return "${groupIndex + 1}. ${parts.joinToString(" · ")}"
}

/**
 * Вариант дорожки для меню: уникальная подпись ([audioLabel]) плюс те же метаданные по частям — ТВ
 * раскладывает их по строке варианта (язык и тип, студия, каналы).
 */
private fun audioOption(groupIndex: Int, group: Tracks.Group, apiAudios: List<AudioTrack>): AudioOption {
    val meta = apiAudios.firstOrNull { it.index == groupIndex + 1 }
    return AudioOption(
        label = audioLabel(groupIndex, group, apiAudios),
        groupIndex = groupIndex,
        languageCode = meta?.lang ?: group.getTrackFormat(0).language,
        voiceType = meta?.voiceType,
        voiceAuthor = meta?.voiceAuthor,
        channels = meta?.channels ?: group.getTrackFormat(0).channelCount.coerceAtLeast(0),
    )
}

/**
 * Ключ озвучки для памяти на тайтл: `язык|тип|студия` из метаданных API. Позиционный индекс не
 * годится — у разных серий порядок дорожек может отличаться, а связка язык+тип+студия
 * идентифицирует именно озвучку.
 */
private fun voiceKey(groupIndex: Int, group: Tracks.Group, apiAudios: List<AudioTrack>): String {
    val meta = apiAudios.firstOrNull { it.index == groupIndex + 1 }
    val language = meta?.lang ?: group.getTrackFormat(0).language
    return listOf(language.orEmpty(), meta?.voiceType.orEmpty(), meta?.voiceAuthor.orEmpty()).joinToString("|")
}

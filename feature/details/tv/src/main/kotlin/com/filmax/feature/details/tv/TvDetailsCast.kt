// Состав в деталях фильма: актёры кругами с фото и режиссёр в том же ряду.
package com.filmax.feature.details.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvPersonCard
import com.filmax.core.tv.designsystem.TvRail
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.core.tv.designsystem.tvRailEntry
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.initials
import com.filmax.feature.details.common.resolveCast

/** Состав появляется следом за кнопками шапки (у них fxEnter 4). */
private const val CAST_ENTER_FROM = 5

/**
 * Сколько первых карточек ряда появляются по очереди — те, что видны при открытии. Дальние
 * компонуются, когда до них долистали, и задержка появления там читалась бы как подтормаживание.
 */
private const val CAST_ENTER_COUNT = 6

/**
 * «В ролях»: актёры с фото (TMDB, если доехали; иначе — имена из строки kino.pub) и режиссёр
 * чипом в конце ряда. По «ОК» — фильмография человека.
 */
internal fun LazyListScope.castRail(
    state: DetailsState,
    item: Item,
    actions: DetailsActions,
    focus: TvScreenFocus,
    below: FocusRequester,
) {
    val people = resolveCast(state.cast, item.cast)
    // По запятой — только первый режиссёр: kino.pub ищет фильмографию по одному имени.
    val director = item.director.substringBefore(',').trim()
    if (people.isEmpty() && director.isBlank()) return
    item(key = "cast") {
        TvRail(title = "В ролях", contentStart = TvMetrics.SafeHorizontal) {
            // Без key: имена в составе kino.pub повторяются, позиционного ключа достаточно.
            itemsIndexed(people) { index, member ->
                TvPersonCard(
                    name = member.name,
                    photoUrl = member.photoUrl,
                    initials = initials(member.name),
                    onClick = { actions.onOpenPerson(member.name, false) },
                    modifier = focus.item("cast:$index")
                        .tvRailEntry(index)
                        // Чип режиссёра в том же ряду ниже фото, и штатный поиск счёл бы его
                        // соседом снизу: «вниз» с актёра вставал на чип, а не уходил в ряд ниже.
                        .focusProperties { down = below }
                        .castEnter(index),
                )
            }
            if (director.isNotBlank()) {
                item(key = "director") {
                    DirectorChip(director, actions, focus.item("director").tvRailEntry(people.size), people.size)
                }
            }
        }
    }
}

/** Появление карточки состава по очереди — только у видимых при открытии. */
private fun Modifier.castEnter(index: Int): Modifier =
    if (index < CAST_ENTER_COUNT) fxEnter(CAST_ENTER_FROM + index) else this

/** Режиссёр после актёров; [chipModifier] — память фокуса экрана и ряда, [index] — место в ряду. */
@Composable
private fun DirectorChip(name: String, actions: DetailsActions, chipModifier: Modifier, index: Int) {
    Column(
        Modifier.castEnter(index).padding(start = 24.dp, top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TvOverline("Режиссёр", accent = false)
        TvChip(
            label = name,
            selected = false,
            onClick = { actions.onOpenPerson(name, true) },
            modifier = chipModifier,
        )
    }
}

package com.filmax.core.tv.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Цвет папки в трёх яркостях макета (oklch с одним оттенком): [card] — плашка карточки в «Моё»
 * и шапка открытой папки, [tile] — плитка-иконка в списке «В папку», [swatch] — кружок выбора
 * цвета. Тёмная плашка держит белый текст, светлый кружок читается на тёмной панели.
 */
@Immutable
data class TvFolderTint(val card: Color, val tile: Color, val swatch: Color)

/**
 * Шесть цветов папок: фиолетовый, синий, красный, зелёный, розовый, янтарный (oklch 0.45/0.13,
 * 0.55/0.14 и 0.6/0.16 из макета). Номер цвета хранит домен, здесь — только его вид.
 */
val TvFolderTints = listOf(
    TvFolderTint(card = Color(0xFF564597), tile = Color(0xFF7260BD), swatch = Color(0xFF816BD8)),
    TvFolderTint(card = Color(0xFF00579A), tile = Color(0xFF1F74BF), swatch = Color(0xFF1A83DB)),
    TvFolderTint(card = Color(0xFF8F3126), tile = Color(0xFFB54C3D), swatch = Color(0xFFCE5342)),
    TvFolderTint(card = Color(0xFF00682A), tile = Color(0xFF1C8742), swatch = Color(0xFF139948)),
    TvFolderTint(card = Color(0xFF87305F), tile = Color(0xFFAB4A7D), swatch = Color(0xFFC3518D)),
    TvFolderTint(card = Color(0xFF7A4A00), tile = Color(0xFF9B6500), swatch = Color(0xFFB17100)),
)

/** Цвет папки по номеру; номер вне диапазона заворачивается по кругу, а не роняет экран. */
fun tvFolderTint(index: Int): TvFolderTint = TvFolderTints[index.mod(TvFolderTints.size)]

package com.filmax.core.tv.designsystem

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.ui.components.PosterImage

private val PersonPhotoSize = 62.dp
private val PersonCardWidth = 76.dp

/**
 * Человек из состава: круглое фото 62dp (или [initials], если фото нет) и имя под ним в две
 * строки. Ряд «В ролях» в деталях, найденные актёры в поиске.
 */
@Composable
fun TvPersonCard(
    name: String,
    photoUrl: String?,
    initials: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFxColors.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(
        modifier = modifier.width(PersonCardWidth).tvNeighborDim(focused),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TvFocusSurface(
            onClick = onClick,
            modifier = Modifier.size(PersonPhotoSize),
            style = TvFocusStyle.Circle,
            colors = TvSurfaceColors(container = colors.surfaceHigh, content = colors.textSecondary),
            interactionSource = interaction,
        ) {
            if (photoUrl != null) {
                PosterImage(
                    url = photoUrl,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    accentColor = TvSurfaceContainerHigh,
                )
            } else {
                Text(initials, style = FxType.button, modifier = Modifier.align(Alignment.Center))
            }
        }
        Text(
            name,
            style = FxType.meta,
            color = colors.text,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

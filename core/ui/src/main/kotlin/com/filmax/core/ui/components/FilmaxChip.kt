package com.filmax.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FilmaxChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        tween(240), label = "chipBg",
    )
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(240), label = "chipFg",
    )
    val shape = RoundedCornerShape(percent = 50)

    Surface(
        color  = bgColor,
        shape  = shape,
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .then(
                if (!selected) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                else Modifier
            ),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text  = label,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

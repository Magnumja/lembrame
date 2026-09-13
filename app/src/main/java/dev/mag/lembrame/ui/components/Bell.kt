package dev.mag.lembrame.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.mag.lembrame.R

/**
 * Sino do "Notify" do bencho.dev: balança numa oscilação amortecida, cada
 * passada menor que a anterior — o que um sino batido de verdade faz.
 * Pendura pela coroa, então é lá que pivota. Toca a cada mudança de [ring].
 */
@Composable
fun Bell(ring: Int, modifier: Modifier = Modifier) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(ring) {
        rotation.snapTo(0f)
        rotation.animateTo(
            0f,
            keyframes {
                durationMillis = 820
                0f at 0
                -17f at 90 using EaseOut
                14f at 220 using EaseOut
                -9f at 360 using EaseOut
                6f at 500 using EaseOut
                -3f at 640 using EaseOut
                0f at 820
            },
        )
    }
    Icon(
        painter = painterResource(R.drawable.ic_bell),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .size(28.dp)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0.16f)
                rotationZ = rotation.value
            },
    )
}

package dev.mag.lembrame.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Slide to confirm" do bencho.dev. A alça segue o dedo exatamente (delta a
 * delta, então nunca teleporta) e o commit é um MORPH: a alça abre pra trás
 * de si mesma e preenche a trilha que cruzou, em vez de ser trocada.
 */
@Composable
fun SlideToConfirm(
    label: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pad = 5.dp
    val grip = 52.dp
    val height = 62.dp
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(pad),
    ) {
        val density = LocalDensity.current
        val travel = with(density) { (maxWidth - grip).toPx() }
        val x = remember { Animatable(0f) }
        val widthExtra = remember { Animatable(0f) } // quanto a alça cresceu pra trás
        var done by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val spring = spring<Float>(dampingRatio = 0.8f, stiffness = 500f)

        LaunchedEffect(done) {
            if (done) {
                delay(420)
                onConfirm()
                delay(250)
                widthExtra.snapTo(0f); x.snapTo(0f); done = false
            }
        }

        val progress = (x.value / travel).coerceIn(0f, 1f)
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { alpha = 1f - progress * 1.6f },
        )

        Box(
            Modifier
                .fillMaxHeight()
                .graphicsLayer { translationX = x.value - widthExtra.value }
                .width(grip + with(density) { widthExtra.value.toDp() })
                .clip(CircleShape)
                .background(if (done) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                .pointerInput(travel) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (x.value > travel * 0.88f) {
                                    done = true
                                    launch { x.animateTo(travel, spring) }
                                    launch { widthExtra.animateTo(travel, spring) }
                                } else {
                                    x.animateTo(0f, spring)
                                }
                            }
                        },
                        onDragCancel = { scope.launch { x.animateTo(0f, spring) } },
                    ) { change, delta ->
                        change.consume()
                        if (!done) scope.launch { x.snapTo((x.value + delta).coerceIn(0f, travel)) }
                    }
                },
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(Modifier.width(grip).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Icon(
                    if (done) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

package dev.mag.lembrame.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Rótulo curto do dia: Hoje, Amanhã, depois o nome do dia da semana. */
fun dayLabel(day: LocalDate, today: LocalDate = LocalDate.now()): String = when (day) {
    today -> "Hoje"
    today.plusDays(1) -> "Amanhã"
    else -> day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR"))
        .removeSuffix(".").replaceFirstChar { it.uppercase() }
}

/**
 * Seletor dos 7 dias com o indicador "em duas fases" do Icon bar do bencho.dev:
 * a borda da frente salta e a pílula se estica sobre os dois slots; a borda de
 * trás alcança depois e contrai no destino. Dois Animatables, duas molas.
 */
@Composable
fun DayPicker(
    days: List<LocalDate>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val index = days.indexOf(selected).coerceAtLeast(0)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
    ) {
        val slot = maxWidth / days.size
        val slotPx = with(LocalDensity.current) { slot.toPx() }
        val left = remember { Animatable(index * slotPx) }
        val right = remember { Animatable((index + 1) * slotPx) }
        val scope = androidx.compose.runtime.rememberCoroutineScope()

        LaunchedEffect(index, slotPx) {
            val targetL = index * slotPx
            val targetR = (index + 1) * slotPx
            val movingRight = targetR > right.value
            val fast = spring<Float>(dampingRatio = 0.72f, stiffness = 1100f)
            val slow = spring<Float>(dampingRatio = 0.68f, stiffness = 380f)
            launch { left.animateTo(targetL, if (movingRight) slow else fast) }
            launch { right.animateTo(targetR, if (movingRight) fast else slow) }
        }

        Box(
            Modifier
                .fillMaxHeight()
                .graphicsLayer { translationX = left.value }
                .width(with(LocalDensity.current) { (right.value - left.value).toDp() })
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )

        Row(Modifier.fillMaxWidth()) {
            days.forEachIndexed { i, day ->
                val isSel = i == index
                val color by animateColorAsState(
                    if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "dayColor",
                )
                Column(
                    Modifier
                        .width(slot)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { scope.launch { onSelect(day) } },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Text(
                        dayLabel(day),
                        color = color,
                        fontSize = if (i <= 1) 11.sp else 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Text(
                        day.dayOfMonth.toString(),
                        color = color,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

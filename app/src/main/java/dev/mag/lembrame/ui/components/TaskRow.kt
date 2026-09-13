package dev.mag.lembrame.ui.components

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mag.lembrame.data.Task

/**
 * Linha de tarefa inspirada no "Checklist" do bencho.dev:
 * UMA MOLA POR LINHA. A caixa enchendo, o tick se desenhando, o risco cruzando
 * as palavras e o texto perdendo a tinta são quatro leituras do mesmo número —
 * não quatro animações tentando chegar juntas.
 */
@Composable
fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t by animateFloatAsState(
        targetValue = if (task.done) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "check",
    )
    // Onde o overshoot é permitido e onde não é: o preenchimento usa o valor
    // cru e passa do cheio; o tick e o risco usam o valor CLAMPADO — um tick
    // que passa do fim e volta é um glitch, não um bounce.
    val clamped = t.coerceIn(0f, 1f)
    val cut = ((t - 0.12f) / 0.72f).coerceIn(0f, 1f)

    val ink = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val outline = MaterialTheme.colorScheme.outline

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(26.dp)) {
            val stroke = 2.dp.toPx()
            val radius = CornerRadius(7.dp.toPx())
            drawRoundRect(
                color = outline,
                cornerRadius = radius,
                style = Stroke(stroke),
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
            )
            // preenchimento: valor cru, com overshoot
            if (t > 0.01f) scale(t) {
                drawRoundRect(color = accent, cornerRadius = radius)
            }
            // tick: DESENHADO, não fadeado. PathMeasure corta um trecho proporcional.
            if (clamped > 0.01f) {
                val s = size.width / 24f
                val tick = Path().apply {
                    moveTo(6f * s, 12.4f * s); lineTo(10.3f * s, 16.7f * s); lineTo(18f * s, 7.6f * s)
                }
                val measure = PathMeasure().apply { setPath(tick, false) }
                val seg = Path()
                measure.getSegment(0f, measure.length * clamped, seg, true)
                drawPath(seg, onAccent, style = Stroke(2.4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = task.title,
            color = ink.copy(alpha = 1f - 0.55f * cut),
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 22.sp,
            modifier = Modifier.drawBehind {
                // O risco segue o tick, não corre com ele: é uma janela no mesmo
                // número, escalada a partir da borda esquerda, do tamanho das palavras.
                if (cut > 0f) drawLine(
                    color = ink.copy(alpha = 0.8f),
                    start = Offset(0f, size.height / 2 + 1f),
                    end = Offset(size.width * cut, size.height / 2 + 1f),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            },
        )
    }
}

/**
 * Queda da linha quando o dia inteiro foi concluído. Vai no invólucro da
 * linha (com o fundo do swipe junto), não na linha em si.
 */
@Composable
fun Modifier.fallWhenDone(index: Int, fell: Boolean): Modifier {
    // Terminar a lista tira o chão das linhas: descida com forma de gravidade
    // (easeIn), quique no fim; a volta é uma MOLA, porque a lista se reafirmando
    // não é algo sendo derrubado — e uma mola interrompe limpo.
    val drop = 10f + index * 6f
    val fallY by animateFloatAsState(
        targetValue = if (fell) drop else 0f,
        animationSpec = if (fell) keyframes {
            durationMillis = 620
            delayMillis = 140 + index * 70
            0f at 0 using EaseIn
            drop at 410 using EaseOut
            (drop - 9f) at 520 using EaseIn
            drop at 620
        } else spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "fall",
    )
    val tilt by animateFloatAsState(
        targetValue = if (fell) (if (index % 2 == 0) -2.6f else 2.2f) else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
        label = "tilt",
    )

    return graphicsLayer {
        translationY = fallY * density
        rotationZ = tilt
    }
}

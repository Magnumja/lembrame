package dev.mag.lembrame.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider
import dev.mag.lembrame.MainActivity
import dev.mag.lembrame.R
import dev.mag.lembrame.data.Task
import dev.mag.lembrame.data.TaskRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class LembrameWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LembrameWidget()
}

private val TaskIdKey = ActionParameters.Key<String>("taskId")

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TaskIdKey] ?: return
        TaskRepository.toggle(context, id)
        LembrameWidget().update(context, glanceId)
    }
}

/**
 * Widget "Amanhã": as tarefas do próximo dia, com checkbox que funciona direto
 * na tela inicial. Embaixo, um resumo do que ainda falta hoje.
 */
class LembrameWidget : GlanceAppWidget() {

    companion object {
        /** Pede pro launcher fixar o widget (Android 8+); o launcher mostra o diálogo de confirmação. */
        fun requestPin(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, LembrameWidgetReceiver::class.java)
            if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(provider, null, null)
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        TaskRepository.purge(context) // toda atualização periódica limpa o que passou de uma semana

        // O Flow é coletado DENTRO do conteúdo: o Glance mantém a sessão viva e só
        // recompõe, então dados carregados antes do provideContent ficariam velhos.
        provideContent {
            val all by TaskRepository.tasks(context).collectAsState(emptyList())
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            GlanceTheme {
                Content(
                    tomorrow = tomorrow,
                    tomorrowTasks = all.filter { it.date == tomorrow.toString() },
                    todayTasks = all.filter { it.date == today.toString() },
                )
            }
        }
    }

    @Composable
    private fun Content(tomorrow: LocalDate, tomorrowTasks: List<Task>, todayTasks: List<Task>) {
        val bg = ColorProvider(day = Color(0xFFFF6B4A), night = Color(0xFF2A1F2E))
        val ink = ColorProvider(Color.White)
        val soft = ColorProvider(Color(0xCCFFFFFF))
        val date = tomorrow.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale("pt", "BR")))
            .replace(".", "")

        Column(
            GlanceModifier
                .fillMaxSize()
                .background(bg)
                .cornerRadius(24.dp)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    Text("Amanhã", style = TextStyle(color = ink, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                    Text(date, style = TextStyle(color = soft, fontSize = 12.sp))
                }
                Box(
                    GlanceModifier
                        .size(36.dp)
                        .background(ColorProvider(Color(0x33FFFFFF)))
                        .cornerRadius(18.dp)
                        .clickable(
                            actionStartActivity<MainActivity>(
                                actionParametersOf(
                                    ActionParameters.Key<String>(MainActivity.EXTRA_ADD_FOR) to tomorrow.toString(),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(ImageProvider(R.drawable.ic_add), contentDescription = "Adicionar", modifier = GlanceModifier.size(20.dp))
                }
            }
            Spacer(GlanceModifier.height(10.dp))

            if (tomorrowTasks.isEmpty()) {
                Text(
                    "Nada pra amanhã ainda 🌙\nToque em + pra lembrar.",
                    style = TextStyle(color = soft, fontSize = 14.sp),
                )
            } else {
                tomorrowTasks.take(6).forEach { task ->
                    Row(
                        GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(actionRunCallback<ToggleTaskAction>(actionParametersOf(TaskIdKey to task.id))),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            ImageProvider(if (task.done) R.drawable.ic_box_on else R.drawable.ic_box_off),
                            contentDescription = null,
                            modifier = GlanceModifier.size(20.dp),
                        )
                        Spacer(GlanceModifier.width(10.dp))
                        Text(
                            task.title,
                            maxLines = 1,
                            style = TextStyle(
                                color = if (task.done) soft else ink,
                                fontSize = 15.sp,
                                fontWeight = if (task.done) FontWeight.Normal else FontWeight.Medium,
                                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                            ),
                        )
                    }
                }
                if (tomorrowTasks.size > 6) {
                    Text("+${tomorrowTasks.size - 6} mais", style = TextStyle(color = soft, fontSize = 12.sp))
                }
            }

            if (todayTasks.isNotEmpty()) {
                Spacer(GlanceModifier.defaultWeight())
                val left = todayTasks.count { !it.done }
                Text(
                    if (left == 0) "Hoje: tudo feito ✨" else "Hoje: $left de ${todayTasks.size} ainda por fazer",
                    style = TextStyle(color = soft, fontSize = 12.sp),
                )
            }
        }
    }
}

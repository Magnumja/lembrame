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
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.datastore.preferences.core.stringPreferencesKey
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

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

class TomorrowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TomorrowWidget()
}

/** Widget de hoje: esferas começam no coral (quente). */
class TodayWidget : LembrameWidget(dayOffset = 0, label = "Hoje", receiver = TodayWidgetReceiver::class.java, orbStart = 1)

/** Widget de amanhã: esferas começam no azul (frio). */
class TomorrowWidget : LembrameWidget(dayOffset = 1, label = "Amanhã", receiver = TomorrowWidgetReceiver::class.java, orbStart = 0)

/** A data de referência vive no estado do Glance: mudar o valor força recomposição (o Compose ignora `now()` puro). */
private val DateKey = stringPreferencesKey("date")

/** Atualiza os dois widgets — chamado sempre que uma tarefa muda ou vira o dia. */
suspend fun refreshWidgets(context: Context) {
    val today = LocalDate.now().toString()
    val manager = GlanceAppWidgetManager(context)
    for (widget in listOf(TodayWidget(), TomorrowWidget())) {
        for (id in manager.getGlanceIds(widget.javaClass)) {
            updateAppWidgetState(context, id) { it[DateKey] = today }
            widget.update(context, id)
        }
    }
}

private val TaskIdKey = ActionParameters.Key<String>("taskId")

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[TaskIdKey] ?: return
        TaskRepository.toggle(context, id)
        refreshWidgets(context)
    }
}

/**
 * Um dia por widget (hoje ou amanhã), com checkbox que funciona direto na
 * tela inicial e uma pílula pra adicionar já naquele dia.
 */
abstract class LembrameWidget(
    private val dayOffset: Long,
    private val label: String,
    private val receiver: Class<out GlanceAppWidgetReceiver>,
    private val orbStart: Int,
) : GlanceAppWidget() {

    /** Recompõe com o tamanho real: o número de linhas depende da altura que o launcher deu. */
    override val sizeMode: SizeMode = SizeMode.Exact

    /** Pede pro launcher fixar este widget (Android 8+); o launcher mostra o diálogo de confirmação. */
    fun requestPin(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(context, receiver), null, null)
    }

    private companion object {
        val ORBS = listOf(R.drawable.orb_blue, R.drawable.orb_coral, R.drawable.orb_pink, R.drawable.orb_green)
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        TaskRepository.purge(context) // toda atualização periódica limpa o que passou de uma semana
        MidnightRefresh.schedule(context)

        // O Flow é coletado DENTRO do conteúdo: o Glance mantém a sessão viva e só
        // recompõe, então dados carregados antes do provideContent ficariam velhos.
        provideContent {
            val all by TaskRepository.tasks(context).collectAsState(emptyList())
            val base = currentState(DateKey)?.let { LocalDate.parse(it) } ?: LocalDate.now()
            val day = base.plusDays(dayOffset)
            GlanceTheme { Content(day, all.filter { it.date == day.toString() }) }
        }
    }

    /**
     * Visual do bloco "Roster" do bencho.dev: cartão escuro, uma esfera em
     * gradiente por linha, nome + legenda, anel de seleção à direita e uma
     * pílula clara embaixo que é o botão de adicionar.
     */
    @Composable
    private fun Content(day: LocalDate, tasks: List<Task>) {
        val card = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1F1F22))
        val ink = ColorProvider(day = Color(0xFF1B1720), night = Color(0xFFF2F0EA))
        val mute = ColorProvider(day = Color(0xFF8A8078), night = Color(0xFF8E8E93))
        val pill = ColorProvider(day = Color(0xFF1B1720), night = Color(0xFFEDEBE4))
        val onPill = ColorProvider(day = Color(0xFFF7F1E8), night = Color(0xFF1F1F22))

        val date = day.format(DateTimeFormatter.ofPattern("EEE, d 'de' MMM", Locale("pt", "BR")))
            .replace(".", "")
        // cabeçalho (~26dp) + pílula (46dp + 8dp) + margens (28dp) = 108dp; cada linha ocupa 48dp
        val capacity = ((LocalSize.current.height.value - 108f) / 48f).toInt().coerceIn(1, 8)
        val rows = tasks.take(capacity)
        val left = tasks.count { !it.done }

        Column(
            GlanceModifier
                .fillMaxSize()
                .background(card)
                .cornerRadius(24.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Row(GlanceModifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$label · $date",
                    style = TextStyle(color = mute, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    when {
                        tasks.isEmpty() -> "livre"
                        left == 0 -> "tudo feito"
                        else -> "$left por fazer"
                    },
                    style = TextStyle(color = mute, fontSize = 12.sp),
                )
            }

            if (rows.isEmpty()) {
                Row(GlanceModifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(ImageProvider(R.drawable.orb_blue), null, modifier = GlanceModifier.size(36.dp))
                    Spacer(GlanceModifier.width(12.dp))
                    Column {
                        Text("Nada marcado", style = TextStyle(color = ink, fontSize = 15.sp, fontWeight = FontWeight.Medium))
                        Text("Toque abaixo pra lembrar", style = TextStyle(color = mute, fontSize = 12.sp))
                    }
                }
            }

            rows.forEachIndexed { i, task ->
                Row(
                    GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable(actionRunCallback<ToggleTaskAction>(actionParametersOf(TaskIdKey to task.id))),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(ImageProvider(ORBS[(i + orbStart) % ORBS.size]), null, modifier = GlanceModifier.size(36.dp))
                    Spacer(GlanceModifier.width(12.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Text(
                            task.title,
                            maxLines = 1,
                            style = TextStyle(
                                color = if (task.done) mute else ink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                            ),
                        )
                        Text(if (task.done) "feito" else "pendente", style = TextStyle(color = mute, fontSize = 12.sp))
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Image(
                        ImageProvider(if (task.done) R.drawable.ic_ring_on else R.drawable.ic_ring_off),
                        contentDescription = if (task.done) "Feito" else "Pendente",
                        modifier = GlanceModifier.size(28.dp),
                    )
                }
            }
            if (tasks.size > rows.size) {
                Text("+${tasks.size - rows.size} mais no app", style = TextStyle(color = mute, fontSize = 12.sp))
            }

            Spacer(GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.height(8.dp))
            Row(
                GlanceModifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(pill)
                    .cornerRadius(23.dp)
                    .clickable(
                        actionStartActivity<MainActivity>(
                            actionParametersOf(ActionParameters.Key<String>(MainActivity.EXTRA_ADD_FOR) to day.toString()),
                        ),
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(ImageProvider(R.drawable.ic_pill_plus), null, modifier = GlanceModifier.size(18.dp))
                Spacer(GlanceModifier.width(6.dp))
                Text("Novo lembrete", style = TextStyle(color = onPill, fontSize = 15.sp, fontWeight = FontWeight.Bold))
            }
        }
    }

}

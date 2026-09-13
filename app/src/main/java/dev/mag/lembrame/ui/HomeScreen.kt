package dev.mag.lembrame.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mag.lembrame.AddRequest
import dev.mag.lembrame.data.Task
import dev.mag.lembrame.data.TaskRepository
import dev.mag.lembrame.ui.components.AddTaskSheet
import dev.mag.lembrame.ui.components.Bell
import dev.mag.lembrame.ui.components.DayPicker
import dev.mag.lembrame.ui.components.SlideToConfirm
import dev.mag.lembrame.ui.components.TaskRow
import dev.mag.lembrame.ui.components.dayLabel
import dev.mag.lembrame.ui.components.fallWhenDone
import dev.mag.lembrame.widget.TodayWidget
import dev.mag.lembrame.widget.TomorrowWidget
import dev.mag.lembrame.widget.refreshWidgets
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(addRequest: AddRequest?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val days = remember(today) { (0 until TaskRepository.HORIZON_DAYS).map { today.plusDays(it.toLong()) } }

    // O app é sobre o próximo dia, então ele abre em "Amanhã".
    var selected by remember { mutableStateOf(today.plusDays(1)) }
    var showAdd by remember { mutableStateOf(false) }
    LaunchedEffect(addRequest) {
        if (addRequest != null) { selected = addRequest.day; showAdd = true }
    }
    var ring by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }

    val all by TaskRepository.tasks(context).collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks = remember(all, selected) { all.filter { it.date == selected.toString() } }
    val fell = tasks.isNotEmpty() && tasks.all { it.done }

    fun refreshWidget() = scope.launch { refreshWidgets(context) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { showAdd = true },
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Icon(Icons.Rounded.Add, contentDescription = "Adicionar tarefa", Modifier.width(32.dp).height(32.dp)) }
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Bell(ring)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Lembra-me", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
                    Text(
                        selected.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR")))
                            .replaceFirstChar { it.uppercase() },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
                Box {
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { menu = true }) {
                        Icon(
                            Icons.Rounded.Widgets,
                            contentDescription = "Adicionar widget à tela inicial",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Widget de hoje") },
                            onClick = { menu = false; TodayWidget().requestPin(context) },
                        )
                        DropdownMenuItem(
                            text = { Text("Widget de amanhã") },
                            onClick = { menu = false; TomorrowWidget().requestPin(context) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            DayPicker(days = days, selected = selected, onSelect = { selected = it })
            Spacer(Modifier.height(18.dp))

            Box(Modifier.weight(1f)) {
                if (tasks.isEmpty()) {
                    EmptyDay(selected)
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                ) {
                    item(key = "done-banner") {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = fell,
                            enter = fadeIn() + slideInVertically { -it / 2 } + scaleIn(initialScale = 0.9f),
                            exit = fadeOut(),
                        ) {
                            Text(
                                "Tudo feito ${dayLabel(selected).lowercase()} ✨",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            )
                        }
                    }
                    items(tasks, key = { it.id }) { task ->
                        val index = tasks.indexOf(task)
                        DismissibleTask(
                            task = task,
                            modifier = Modifier.animateItem().fallWhenDone(index, fell),
                            onDismiss = {
                                scope.launch {
                                    TaskRepository.remove(context, task.id)
                                    refreshWidget()
                                    val r = snackbar.showSnackbar(
                                        "Tarefa apagada", actionLabel = "Desfazer", duration = SnackbarDuration.Short,
                                    )
                                    if (r == SnackbarResult.ActionPerformed) {
                                        TaskRepository.restore(context, task); refreshWidget()
                                    }
                                }
                            },
                        ) {
                            TaskRow(
                                task = task,
                                onToggle = { scope.launch { TaskRepository.toggle(context, task.id); refreshWidget() } },
                            )
                        }
                    }
                    if (tasks.isNotEmpty()) item(key = "clear") {
                        Spacer(Modifier.height(16.dp))
                        SlideToConfirm(
                            label = "Deslize pra limpar o dia",
                            onConfirm = { scope.launch { TaskRepository.clearDay(context, selected); refreshWidget() } },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddTaskSheet(
            days = days,
            initialDay = selected,
            onAdd = { title, day ->
                scope.launch {
                    TaskRepository.add(context, title, day)
                    refreshWidget()
                    selected = day
                    ring++
                }
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleTask(task: Task, modifier: Modifier, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { it != SwipeToDismissBoxValue.StartToEnd },
        positionalThreshold = { it * 0.45f },
    )
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onDismiss()
    }
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = "Apagar", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
        content = { content() },
    )
}

@Composable
private fun EmptyDay(day: LocalDate) {
    // Flutua devagar pra tela vazia não parecer travada.
    val float = rememberInfiniteTransition(label = "float").animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "y",
    )
    val label = dayLabel(day)
    val (emoji, msg) = when (label) {
        "Hoje" -> "☀️" to "Nada pra hoje.\nRespira."
        "Amanhã" -> "🌙" to "Amanhã tá livre.\nO que você não pode esquecer?"
        else -> "📅" to "Nada marcado.\nToque em + pra lembrar."
    }
    Column(
        Modifier.fillMaxSize().padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, fontSize = 56.sp, modifier = Modifier.graphicsLayer { translationY = float.value * density })
        Spacer(Modifier.height(12.dp))
        Text(
            msg,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

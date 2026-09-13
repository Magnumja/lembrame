package dev.mag.lembrame.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID

private val Context.store: DataStore<Preferences> by preferencesDataStore("lembrame")
private val KEY = stringPreferencesKey("tasks")
private val json = Json { ignoreUnknownKeys = true }
private val listSerializer = ListSerializer(Task.serializer())

/**
 * Fonte única das tarefas. Tudo é um JSON pequeno num DataStore — o app
 * guarda no máximo uma semana de coisas, então não precisa de banco.
 */
object TaskRepository {

    /** Dá pra planejar de hoje até 6 dias à frente. */
    const val HORIZON_DAYS = 7

    /** Tarefa com mais de uma semana some sozinha. */
    const val RETENTION_DAYS = 7

    fun tasks(context: Context): Flow<List<Task>> =
        context.store.data.map { prefs -> decode(prefs[KEY]).alive().sorted() }

    suspend fun snapshot(context: Context): List<Task> = tasks(context).first()

    suspend fun add(context: Context, title: String, day: LocalDate): Task {
        val task = Task(id = UUID.randomUUID().toString(), title = title.trim(), date = day.toString())
        mutate(context) { it + task }
        return task
    }

    suspend fun toggle(context: Context, id: String) =
        mutate(context) { list -> list.map { if (it.id == id) it.copy(done = !it.done) else it } }

    suspend fun remove(context: Context, id: String) =
        mutate(context) { list -> list.filterNot { it.id == id } }

    suspend fun restore(context: Context, task: Task) =
        mutate(context) { list -> if (list.any { it.id == task.id }) list else list + task }

    suspend fun clearDay(context: Context, day: LocalDate) =
        mutate(context) { list -> list.filterNot { it.date == day.toString() } }

    /** Roda a limpeza sem mudar mais nada — chamado quando o widget acorda. */
    suspend fun purge(context: Context) = mutate(context) { it }

    private suspend fun mutate(context: Context, change: (List<Task>) -> List<Task>) {
        context.store.edit { prefs ->
            val next = change(decode(prefs[KEY])).alive()
            prefs[KEY] = json.encodeToString(listSerializer, next)
        }
    }

    private fun decode(raw: String?): List<Task> =
        if (raw.isNullOrBlank()) emptyList()
        else runCatching { json.decodeFromString(listSerializer, raw) }.getOrDefault(emptyList())

    private fun List<Task>.alive(): List<Task> {
        val cutoff = LocalDate.now().minusDays(RETENTION_DAYS.toLong())
        return filter { runCatching { it.day.isAfter(cutoff) }.getOrDefault(false) }
    }

    private fun List<Task>.sorted(): List<Task> =
        sortedWith(compareBy<Task> { it.date }.thenBy { it.createdAt })
}

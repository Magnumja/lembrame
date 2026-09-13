package dev.mag.lembrame.data

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Task(
    val id: String,
    val title: String,
    /** Dia da tarefa em ISO (yyyy-MM-dd). */
    val date: String,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val day: LocalDate get() = LocalDate.parse(date)
}

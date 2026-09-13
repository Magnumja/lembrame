package dev.mag.lembrame

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import dev.mag.lembrame.ui.HomeScreen
import dev.mag.lembrame.ui.theme.LembrameTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    /** Pedido pendente de "abrir a folha de adicionar no dia X"; muda a cada intent, incluindo onNewIntent. */
    private val addRequest = mutableStateOf<AddRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readIntent(intent)
        setContent { LembrameTheme { HomeScreen(addRequest.value) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent?) {
        val day = intent?.getStringExtra(EXTRA_ADD_FOR)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
        // Consome o extra: numa recriação (rotação, modo escuro) o intent é relido
        // e a folha não deve abrir de novo.
        intent.removeExtra(EXTRA_ADD_FOR)
        setIntent(intent)
        addRequest.value = AddRequest(day)
    }

    companion object {
        /** O "+" do widget abre o app direto na folha de adicionar, já no dia certo. */
        const val EXTRA_ADD_FOR = "add_for"
    }
}

/** Instância nova a cada pedido, pra dois toques no "+" do widget abrirem a folha duas vezes. */
class AddRequest(val day: LocalDate)

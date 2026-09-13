package dev.mag.lembrame.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * "Hoje" e "Amanhã" mudam à meia-noite, mas o widget só recompõe quando os
 * dados mudam. Um alarme (inexato, sem acordar o aparelho) logo depois da
 * meia-noite força a atualização; o `updatePeriodMillis` fica como reserva.
 */
object MidnightRefresh {
    fun schedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val at = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).plusMinutes(1)
            .toInstant().toEpochMilli()
        alarms.setAndAllowWhileIdle(AlarmManager.RTC, at, pending(context))
    }

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, MidnightReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

class MidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                refreshWidgets(context)
                MidnightRefresh.schedule(context)
            } finally {
                result.finish()
            }
        }
    }
}

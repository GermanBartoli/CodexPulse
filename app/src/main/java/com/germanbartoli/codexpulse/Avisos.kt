package com.germanbartoli.codexpulse

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

object Preferencias {
    private fun datos(contexto: Context) = contexto.getSharedPreferences("ajustes", Context.MODE_PRIVATE)
    fun minutos(contexto: Context) = datos(contexto).getInt("minutos", 30).takeIf { it in listOf(15, 30, 60) } ?: 30
    fun frecuencia(contexto: Context, minutos: Int) { require(minutos in listOf(15, 30, 60)); datos(contexto).edit().putInt("minutos", minutos).apply() }
    fun alertas(contexto: Context) = datos(contexto).getBoolean("alertas", false)
    fun alertas(contexto: Context, activas: Boolean) {
        datos(contexto).edit().putBoolean("alertas", activas).apply()
        if (!activas) Avisos.cancelar(contexto)
    }
}

// Un aviso por umbral y período; una mejora parcial no repite el aviso anterior.
fun umbralPendiente(disponible: Int, anterior: Int?): Int? {
    val umbral = when { disponible <= 10 -> 10; disponible <= 20 -> 20; else -> return null }
    return umbral.takeIf { anterior == null || umbral < anterior }
}

object Avisos {
    private const val CANAL = "cuota"
    private const val IDENTIFICADOR = 701
    fun permitido(contexto: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && contexto.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val gestor = contexto.getSystemService(NotificationManager::class.java)
        return gestor.areNotificationsEnabled() && gestor.getNotificationChannel(CANAL)?.importance != NotificationManager.IMPORTANCE_NONE
    }
    fun cancelar(contexto: Context) = contexto.getSystemService(NotificationManager::class.java).cancel(IDENTIFICADOR)
    fun mostrar(contexto: Context, limites: List<Limite>): Boolean {
        if (!Preferencias.alertas(contexto) || !permitido(contexto) || limites.isEmpty()) return false
        return try {
            val gestor = contexto.getSystemService(NotificationManager::class.java)
            gestor.createNotificationChannel(NotificationChannel(CANAL, "Disponibilidad de Codex", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos opcionales al quedar 20% o 10% de cuota"
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            })
            val abrir = PendingIntent.getActivity(contexto, 0, Intent(contexto, PantallaPrincipal::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val publico = Notification.Builder(contexto, CANAL).setSmallIcon(R.drawable.ic_aviso)
                .setContentTitle("Pulso de Germán").setContentText("Abrí la aplicación para ver un aviso de consumo.").build()
            val texto = limites.joinToString(" · ") { "${it.nombre}: ${it.disponible}% disponible" }
            val aviso = Notification.Builder(contexto, CANAL).setSmallIcon(R.drawable.ic_aviso)
                .setContentTitle("Tu cuota de Codex está bajando").setContentText(texto)
                .setStyle(Notification.BigTextStyle().bigText(texto)).setContentIntent(abrir)
                .setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(publico).build()
            gestor.notify(IDENTIFICADOR, aviso)
            true
        } catch (_: SecurityException) { false }
    }
}
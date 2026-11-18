package com.germanbartoli.codexpulse

import android.app.*
import android.content.Context
import android.content.Intent
import java.time.LocalTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun dentroDeSilencio(hora: Int, desde: Int, hasta: Int): Boolean =
    if (desde <= hasta) hora in desde until hasta else hora >= desde || hora < hasta

fun silenciado(contexto: Context): Boolean {
    val p = Preferencias.datos(contexto)
    val ahora = LocalTime.now()
    return p.getBoolean("silencio", false) && dentroDeSilencio(ahora.hour * 60 + ahora.minute, p.getInt("desde", 1320), p.getInt("hasta", 480))
}

internal const val PLANTILLA = "Semanal: {semanal} · 5 h: {5h}"
fun textoConsumo(plantilla: String, consumo: Consumo?): String {
    fun porcentaje(segundos: Long) = consumo?.limites?.find { it.segundos == segundos }?.let { "${it.disponible}%" } ?: "—"
    val hora = consumo?.let { DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it.fecha)) } ?: "—"
    return plantilla.replace("{semanal}", porcentaje(604800)).replace("{5h}", porcentaje(18000))
        .replace("{hora}", hora).replace("{plan}", consumo?.plan ?: "—")
}

fun cuotaRecuperada(espera: Set<String>, limites: List<Limite>): Boolean =
    espera.isNotEmpty() && limites.all { it.disponible > 0 } &&
        espera.all { clave -> limites.any { it.segundos.toString() == clave } }

object ConsumoVisible {
    const val CANAL = "consumo_visible"
    const val ID = 702
    fun publicar(contexto: Context, estado: Estado) {
        val p = Preferencias.datos(contexto)
        val gestor = contexto.getSystemService(NotificationManager::class.java)
        if (!p.getBoolean("fija", false) || !estado.conectado) {
            gestor.cancel(ID)
            return
        }
        gestor.createNotificationChannel(NotificationChannel(CANAL, "Consumo en pantalla de bloqueo", NotificationManager.IMPORTANCE_LOW))
        if (!Avisos.permitido(contexto, CANAL)) return
        val abrir = PendingIntent.getActivity(contexto, 0, Intent(contexto, PantallaPrincipal::class.java), PendingIntent.FLAG_IMMUTABLE)
        val titulo = p.getString("titulo_fijo", null).orEmpty().ifBlank { "Mi consumo de Codex" }
        val cuerpo = p.getString("texto_fijo", null).orEmpty().ifBlank { PLANTILLA }
        val texto = textoConsumo(cuerpo, estado.consumo) + if (estado.error != null) " · Sin actualizar" else ""
        val aviso = Notification.Builder(contexto, CANAL).setSmallIcon(R.drawable.ic_aviso)
            .setContentTitle(textoConsumo(titulo, estado.consumo)).setContentText(texto)
            .setStyle(Notification.BigTextStyle().bigText(texto)).setContentIntent(abrir)
            .setOnlyAlertOnce(true).setOngoing(true).setVisibility(Notification.VISIBILITY_PUBLIC)
            .setWhen(estado.consumo?.fecha ?: System.currentTimeMillis()).build()
        gestor.notify(ID, aviso)
    }
}

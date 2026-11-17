package com.germanbartoli.codexpulse

import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class Limite(val usado: Double, val reinicio: Long?, val segundos: Long?) {
    val disponible: Int get() = (100 - usado).roundToInt().coerceIn(0, 100)
    val nombre: String get() = when (segundos) {
        18000L -> "Cada 5 horas"
        604800L -> "Semanal"
        null -> "Período informado"
        else -> if (segundos >= 86400) "Cada ${segundos / 86400} días" else "Cada ${segundos / 3600} h"
    }
    fun renovacion(ahora: Long = System.currentTimeMillis() / 1000): String {
        val fecha = reinicio ?: return "Renovación no informada"
        if (fecha <= ahora) return "Renovación pendiente de consultar"
        val minutos = (fecha - ahora + 59) / 60
        return if (minutos < 1440) "Renueva en ${minutos / 60} h ${minutos % 60} min"
        else "Renueva " + DateTimeFormatter.ofPattern("EEE d, HH:mm", Locale.forLanguageTag("es-AR")).withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(fecha))
    }
    fun json() = JSONObject().put("used_percent", usado).put("reset_at", reinicio ?: JSONObject.NULL).put("limit_window_seconds", segundos ?: JSONObject.NULL)
}
data class Consumo(val limites: List<Limite>, val plan: String, val fecha: Long = System.currentTimeMillis()) {
    fun json(): String = JSONObject().put("plan_type", plan).put("fecha", fecha).put("rate_limit", JSONObject().put("primary_window", limites.first().json()).put("secondary_window", limites.getOrNull(1)?.json() ?: JSONObject.NULL)).toString()
    companion object {
        fun leer(texto: String): Consumo {
            val raiz = JSONObject(texto)
            val cuota = raiz.optJSONObject("rate_limit") ?: error("El servicio no informó los límites.")
            val limites = listOfNotNull(cuota.optJSONObject("primary_window"), cuota.optJSONObject("secondary_window")).map {
                val usado = it.getDouble("used_percent")
                require(usado.isFinite() && usado >= 0) { "El servicio informó un porcentaje inválido." }
                Limite(usado, it.optLong("reset_at").takeIf { valor -> valor > 0 }, it.optLong("limit_window_seconds").takeIf { valor -> valor > 0 })
            }
            require(limites.isNotEmpty()) { "No hay límites disponibles para esta cuenta." }
            return Consumo(limites, raiz.optString("plan_type", "Codex"), raiz.optLong("fecha", System.currentTimeMillis()))
        }
        fun ejemplo() = Consumo(listOf(Limite(27.0, System.currentTimeMillis()/1000 + 8100, 18000), Limite(58.0, System.currentTimeMillis()/1000 + 172800, 604800)), "Demostración")
    }
}
fun antiguedad(fecha: Long): String {
    val minutos = ((System.currentTimeMillis() - fecha) / 60000).coerceAtLeast(0)
    return when { minutos < 1 -> "Actualizado ahora"; minutos < 60 -> "Actualizado hace $minutos min"; else -> "Actualizado hace ${minutos / 60} h" }
}
class ErrorHttp(val estado: Int) : Exception("El servicio respondió HTTP $estado.")
data class CodigoDispositivo(val id: String, val codigo: String, val intervalo: Long)

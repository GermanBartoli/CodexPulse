package com.germanbartoli.codexpulse

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class PeriodoHistorial(val titulo: String, val milisegundos: Long?) {
    DIA("24 horas", 86400000L), SEMANA("7 días", 604800000L), TODO("Todo", null)
}
fun filtrarHistorial(historial: List<Consumo>, periodo: PeriodoHistorial, ahora: Long): List<Consumo> =
    historial.filter { periodo.milisegundos == null || it.fecha >= ahora - periodo.milisegundos }.sortedBy { it.fecha }

private fun fechaHistorial(fecha: Long): String = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.forLanguageTag("es-AR"))
    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(fecha))

@Composable fun Grafico(historial: List<Consumo>) {
    var periodo by remember { mutableStateOf(PeriodoHistorial.TODO) }
    val puntos = filtrarHistorial(historial, periodo, System.currentTimeMillis())
    val colores = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PeriodoHistorial.entries.forEach { opcion ->
            FilterChip(selected = periodo == opcion, onClick = { periodo = opcion }, label = { Text(opcion.titulo) })
        }
    }
    if (puntos.size < 2) {
        Text("Todavía no hay suficientes consultas en este período.", style = MaterialTheme.typography.bodySmall)
        return
    }
    val desde = puntos.first().fecha
    val hasta = puntos.last().fecha
    val nombres = puntos.flatMap { it.limites }.map { it.segundos }.distinct().take(2)
    val guias = MaterialTheme.colorScheme.outlineVariant
    val descripcion = "Disponibilidad de cero a cien por ciento. ${puntos.size} consultas, desde ${fechaHistorial(desde)} hasta ${fechaHistorial(hasta)}."
    Text("100% disponible", style = MaterialTheme.typography.labelSmall)
    Canvas(Modifier.fillMaxWidth().height(130.dp).semantics { contentDescription = descripcion }) {
        listOf(0f, .5f, 1f).forEach { fraccion ->
            drawLine(guias, androidx.compose.ui.geometry.Offset(0f, size.height * fraccion), androidx.compose.ui.geometry.Offset(size.width, size.height * fraccion), strokeWidth = 1.dp.toPx())
        }
        // Cada serie sigue su duración informada, aunque cambie el orden de los límites.
        val series = if (nombres.isEmpty()) listOf<Long?>(null) else nombres
        series.forEachIndexed { indice, duracion ->
            val linea = Path()
            var iniciado = false
            puntos.forEach { consumo ->
                val limite = consumo.limites.firstOrNull { it.segundos == duracion }
                if (limite == null) iniciado = false else {
                    val x = ((consumo.fecha - desde).toDouble() / (hasta - desde).coerceAtLeast(1)).toFloat() * size.width
                    val y = (1 - limite.disponible / 100f) * size.height
                    if (!iniciado) { linea.moveTo(x, y); iniciado = true } else linea.lineTo(x, y)
                }
            }
            drawPath(linea, colores[indice], style = Stroke(width = 2.dp.toPx()))
        }
    }
    Text("0% disponible", style = MaterialTheme.typography.labelSmall)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(fechaHistorial(desde), style = MaterialTheme.typography.labelSmall)
        Text(fechaHistorial(hasta), style = MaterialTheme.typography.labelSmall)
    }
    val series = if (nombres.isEmpty()) listOf<Long?>(null) else nombres
    series.forEachIndexed { indice, duracion ->
        val ultimo = puntos.lastOrNull { consumo -> consumo.limites.any { it.segundos == duracion } }?.limites?.firstOrNull { it.segundos == duracion }
        ultimo?.let { Text("● ${it.nombre}: ${it.disponible}% en la última consulta de este límite", color = colores[indice], style = MaterialTheme.typography.bodySmall) }
    }
    Text("${puntos.size} consultas guardadas · las subidas pueden corresponder a una renovación de cuota.", style = MaterialTheme.typography.bodySmall)
}
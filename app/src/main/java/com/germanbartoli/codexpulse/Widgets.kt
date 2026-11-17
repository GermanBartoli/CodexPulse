package com.germanbartoli.codexpulse

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.glance.material3.ColorProviders
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider

class WidgetCompacto : GlanceAppWidgetReceiver() { override val glanceAppWidget = CompactoGlance() }
class WidgetCompleto : GlanceAppWidgetReceiver() { override val glanceAppWidget = CompletoGlance() }
class WidgetMinimo : GlanceAppWidgetReceiver() { override val glanceAppWidget = MinimoGlance() }
class WidgetSemanal : GlanceAppWidgetReceiver() { override val glanceAppWidget = SemanalGlance() }
class WidgetHistorial : GlanceAppWidgetReceiver() { override val glanceAppWidget = HistorialGlance() }
class CompactoGlance : PulseWidget(Variante.COMPACTO)
class CompletoGlance : PulseWidget(Variante.COMPLETO)
class MinimoGlance : PulseWidget(Variante.MINIMO)
class SemanalGlance : PulseWidget(Variante.SEMANAL)
class HistorialGlance : PulseWidget(Variante.HISTORIAL)
enum class Variante { COMPACTO, COMPLETO, MINIMO, SEMANAL, HISTORIAL }
class RefrescarWidget : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) { Actualizacion.solicitar(context) }
}
open class PulseWidget(private val variante: Variante, private val estiloPrevio: EstiloWidgets? = null) : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val estado = Repositorio.leer(context)
        val estilo = estiloPrevio ?: (if (GlanceAppWidgetManager(context).getAppWidgetId(id) > 0) Personalizacion.leerPropio(context, id) else null) ?: Personalizacion.leer(context)
        provideContent {
            val claro = lightColorScheme(primary = Color(0xFF146C4F), background = Color(0xFFF4FAF6), surface = Color(0xFFF4FAF6), onSurface = Color(0xFF13231D), onSurfaceVariant = Color(0xFF41564B), surfaceVariant = Color(0xFFD8EEE2))
            val oscuro = darkColorScheme(primary = Color(0xFF8FE4C4), background = Color(0xFF0C1714), surface = Color(0xFF13231D), onSurface = Color(0xFFE2F4E9), onSurfaceVariant = Color(0xFFB7CDBF), surfaceVariant = Color(0xFF30483C))
            val colores = when {
                estilo.fondo == FondoWidget.OSCURO -> ColorProviders(if (estilo.paleta == PaletaWidget.PULSE) oscuro else dynamicDarkColorScheme(context))
                estilo.paleta == PaletaWidget.PULSE -> ColorProviders(claro, oscuro)
                else -> GlanceTheme.colors
            }
            GlanceTheme(colors = colores) { ContenidoWidget(estado, variante, estilo) }
        }
    }
}
@Composable private fun ContenidoWidget(estado: Estado, variante: Variante, estilo: EstiloWidgets) {
    val minimo = variante == Variante.MINIMO
    Column(GlanceModifier.fillMaxSize().background(when { estilo.fondo == FondoWidget.TRANSPARENTE -> ColorProvider(Color.Transparent); estilo.paleta == PaletaWidget.PULSE || estilo.fondo == FondoWidget.OSCURO -> GlanceTheme.colors.surface; else -> GlanceTheme.colors.widgetBackground }).appWidgetBackground().cornerRadius(24.dp).padding(if(minimo) 6.dp else 12.dp).clickable(actionStartActivity<PantallaPrincipal>())) {
        if(minimo) {
            val limite = estado.consumo?.let { limiteIndicador(it.limites, estilo.cuota) }
            Text(when (estilo.cuota) { CuotaIndicador.MENOR -> "Codex"; CuotaIndicador.CINCO_HORAS -> "5 horas"; CuotaIndicador.SEMANAL -> "Semanal" }, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Text(limite?.let { "${it.disponible}%" } ?: if (estado.consumo == null) "Abrir" else "—", style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 28.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Text(if(estado.demostracion) "DEMO" else if(estado.error != null) "Sin actualizar" else if (limite == null && estado.consumo != null) "No informado" else "disponible", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 9.sp), maxLines = 1)
        } else {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Codex Pulse", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.defaultWeight())
                Text("↻", modifier = GlanceModifier.padding(2.dp).clickable(actionRunCallback<RefrescarWidget>()), style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 20.sp))
            }
            val consumo = estado.consumo
            if(consumo == null) Text("Conectá tu cuenta desde la app", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp))
            else {
                when(variante) {
                    Variante.SEMANAL -> {
                        val semanal = consumo.limites.firstOrNull { it.segundos == 604800L }
                        if(semanal == null) Text("Esta cuenta no informó una cuota semanal", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp))
                        else CuotaPrincipal(semanal)
                    }
                    Variante.COMPLETO, Variante.HISTORIAL -> {
                        if(consumo.limites.size == 1) CuotaPrincipal(consumo.limites.first())
                        else if(LocalSize.current.width >= 250.dp) Row(GlanceModifier.fillMaxWidth()) {
                            consumo.limites.forEach { limite -> Column(GlanceModifier.defaultWeight().padding(end = 10.dp)) { CuotaWidget(limite, true) } }
                        } else consumo.limites.forEach { CuotaWidget(it, false) }
                        if(variante == Variante.HISTORIAL && LocalSize.current.height >= 210.dp) {
                            Spacer(GlanceModifier.height(10.dp))
                            if(estado.historial.size >= 2) {
                                Image(ImageProvider(graficoWidget(estado.historial)), "Historial de cuota primaria disponible, de cero a cien por ciento", modifier = GlanceModifier.fillMaxWidth().height(48.dp))
                                Text("Últimas ${estado.historial.size} consultas · cuota primaria", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp))
                            } else Text("El historial aparecerá con tus próximas consultas", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
                        }
                    }
                    else -> if(consumo.limites.size == 1) CuotaPrincipal(consumo.limites.first()) else consumo.limites.forEach { CuotaWidget(it, false) }
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(if(estado.demostracion) "DEMO · datos de ejemplo" else if(estado.error != null) "Sin actualizar · abrí la app" else antiguedad(consumo.fecha), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp), maxLines = 2)
            }
        }
    }
}
@Composable private fun CuotaPrincipal(limite: Limite) {
    val color = if(limite.disponible <= 20) GlanceTheme.colors.error else GlanceTheme.colors.primary
    val ancho = LocalSize.current.width >= 250.dp
    if (ancho) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                Text(limite.nombre, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
                Text("${limite.disponible}%", style = TextStyle(color = color, fontSize = 48.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            }
            Column(GlanceModifier.defaultWeight().padding(start = 12.dp)) {
                Text("disponible", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp))
                Text(limite.renovacion(), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp), maxLines = 2)
            }
        }
    } else {
        Text(limite.nombre, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
        Text("${limite.disponible}%", style = TextStyle(color = color, fontSize = 40.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
    LinearProgressIndicator(progress = limite.disponible / 100f, modifier = GlanceModifier.fillMaxWidth().height(6.dp), color = color, backgroundColor = GlanceTheme.colors.surfaceVariant)
    if (!ancho) Text(limite.renovacion(), modifier = GlanceModifier.padding(top = 3.dp), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp), maxLines = 2)
}

@Composable private fun CuotaWidget(limite: Limite, completo: Boolean) {
    val color = if(limite.disponible <= 20) GlanceTheme.colors.error else GlanceTheme.colors.primary
    Text(limite.nombre, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp), maxLines = 1)
    Text("${limite.disponible}%", style = TextStyle(color = color, fontSize = if(completo) 28.sp else 19.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    if(completo) Text("disponible", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
    LinearProgressIndicator(progress = limite.disponible/100f, modifier = GlanceModifier.fillMaxWidth().height(4.dp), color = color, backgroundColor = GlanceTheme.colors.surfaceVariant)
    if(completo) Text(limite.renovacion(), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp), maxLines = 2)
    Spacer(GlanceModifier.height(3.dp))
}
private fun graficoWidget(historial: List<Consumo>): Bitmap {
    val imagen = Bitmap.createBitmap(600,100,Bitmap.Config.ARGB_8888)
    val lienzo = Canvas(imagen)
    val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(35,160,115); strokeWidth = 4f; style = Paint.Style.STROKE }
    val linea = android.graphics.Path()
    historial.forEachIndexed { indice, consumo ->
        val x = 4 + indice.toFloat()/(historial.size-1).coerceAtLeast(1)*592
        val y = 4 + (1-consumo.limites.first().disponible/100f)*92
        if(indice == 0) linea.moveTo(x,y) else linea.lineTo(x,y)
    }
    lienzo.drawPath(linea,pincel)
    return imagen
}

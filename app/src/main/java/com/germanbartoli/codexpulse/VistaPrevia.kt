package com.germanbartoli.codexpulse

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.compose
import kotlinx.coroutines.CancellationException

private enum class TipoVista(val titulo: String, val ancho: Int, val alto: Int, val variante: Variante) {
    MINIMO("Indicador 1×1", 80, 80, Variante.MINIMO),
    SEMANAL("Semanal 2×2", 170, 180, Variante.SEMANAL),
    COMPACTO("Dos límites 2×2", 170, 180, Variante.COMPACTO),
    COMPLETO("Horizontal 4×2", 350, 180, Variante.COMPLETO),
    HISTORIAL("Historial 4×3", 350, 270, Variante.HISTORIAL);
    fun widget(estilo: EstiloWidgets): GlanceAppWidget = PulseWidget(variante, estilo)
    fun receptor(): Class<out GlanceAppWidgetReceiver> = when(this) {
        MINIMO -> WidgetMinimo::class.java; SEMANAL -> WidgetSemanal::class.java
        COMPACTO -> WidgetCompacto::class.java; COMPLETO -> WidgetCompleto::class.java
        HISTORIAL -> WidgetHistorial::class.java
    }
}

private fun desactivarAcciones(vista: View) {
    vista.isClickable = false
    vista.isLongClickable = false
    if (vista is ViewGroup) for (indice in 0 until vista.childCount) desactivarAcciones(vista.getChildAt(indice))
}

@Composable fun VistaPreviaWidgets(estado: Estado, estilo: EstiloWidgets, tipoFijo: Variante? = null) {
    val contexto = LocalContext.current
    var indice by remember { mutableIntStateOf(2) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    val tipo = TipoVista.entries.firstOrNull { it.variante == tipoFijo } ?: TipoVista.entries[indice]
    Text("Vista previa", style = MaterialTheme.typography.titleLarge)
    if (tipoFijo == null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { indice = (indice + TipoVista.entries.size - 1) % TipoVista.entries.size }) { Text("Anterior") }
        Text(tipo.titulo, style = MaterialTheme.typography.labelLarge)
        TextButton(onClick = { indice = (indice + 1) % TipoVista.entries.size }) { Text("Siguiente") }
    }
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val ancho = minOf(tipo.ancho.dp, maxWidth)
        var remoto by remember { mutableStateOf<RemoteViews?>(null) }
        var error by remember { mutableStateOf(false) }
        LaunchedEffect(tipo, ancho, estilo, estado.consumo?.fecha, estado.demostracion, estado.error) {
            remoto = null
            error = false
            try { remoto = tipo.widget(estilo).compose(contexto, size = DpSize(ancho, tipo.alto.dp)) }
            catch (cancelacion: CancellationException) { throw cancelacion }
            catch (_: Exception) { error = true }
        }
        Box(Modifier.width(ancho).height(tipo.alto.dp), contentAlignment = Alignment.Center) {
            val contenido = remoto
            if (contenido != null) AndroidView(
                factory = { FrameLayout(it) },
                update = { contenedor ->
                    contenedor.removeAllViews()
                    val vista = contenido.apply(contexto, contenedor)
                    desactivarAcciones(vista)
                    contenedor.addView(vista)
                },
                modifier = Modifier.fillMaxSize().semantics { contentDescription = "Vista previa de ${tipo.titulo}" }
            )
            else if (error) Text("No se pudo generar la vista previa.", style = MaterialTheme.typography.bodySmall)
            else CircularProgressIndicator()
        }
    }
    Text("Usa el mismo diseño del widget. El tamaño final depende del inicio de tu teléfono.", style = MaterialTheme.typography.bodySmall)
    if (tipoFijo == null) Button(onClick = {
        val gestor = AppWidgetManager.getInstance(contexto)
        if (gestor.isRequestPinAppWidgetSupported) gestor.requestPinAppWidget(ComponentName(contexto, tipo.receptor()), null, null)
        else mensaje = "Mantené presionado el inicio de Android y elegí Widgets → Codex Pulse."
    }, modifier = Modifier.fillMaxWidth()) { Text("Agregar este widget") }
    mensaje?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
}
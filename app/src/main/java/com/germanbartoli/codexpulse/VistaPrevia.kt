package com.germanbartoli.codexpulse

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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

class ConfirmacionWidget : BroadcastReceiver() {
    companion object {
        private val revision = MutableStateFlow(0L)
        val cambios: kotlinx.coroutines.flow.StateFlow<Long> = revision
        fun notificar() { revision.update { it + 1 } }
    }
    override fun onReceive(contexto: Context, evento: Intent) { notificar() }
}

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
    val cantidad = TipoVista.entries.size
    val paginas = rememberPagerState(initialPage = (Int.MAX_VALUE / 2 / cantidad) * cantidad + 2) { Int.MAX_VALUE }
    var mensaje by remember { mutableStateOf<String?>(null) }
    val tipo = TipoVista.entries.firstOrNull { it.variante == tipoFijo } ?: TipoVista.entries[paginas.currentPage % cantidad]
    Text("Vista previa", style = MaterialTheme.typography.titleLarge)
    if (tipoFijo == null) {
        Text(tipo.titulo, style = MaterialTheme.typography.labelLarge)
        HorizontalPager(paginas, Modifier.fillMaxWidth()) { pagina ->
            MuestraWidget(TipoVista.entries[pagina % cantidad], estado, estilo)
        }
        Text("Deslizá para ver más widgets · ${tipo.ordinal + 1} de $cantidad", style = MaterialTheme.typography.bodySmall)
    } else MuestraWidget(tipo, estado, estilo)
    Text("Usa el mismo diseño del widget. El tamaño final depende del inicio de tu teléfono.", style = MaterialTheme.typography.bodySmall)
    if (tipoFijo == null) Button(onClick = {
        val gestor = AppWidgetManager.getInstance(contexto)
        if (gestor.isRequestPinAppWidgetSupported) {
            val confirmado = PendingIntent.getBroadcast(contexto, 0, Intent(contexto, ConfirmacionWidget::class.java), PendingIntent.FLAG_IMMUTABLE)
            gestor.requestPinAppWidget(ComponentName(contexto, tipo.receptor()), null, confirmado)
        }
        else mensaje = "Mantené presionado el inicio de Android y elegí Widgets → Pulso de Germán."
    }, modifier = Modifier.fillMaxWidth()) { Text("Agregar este widget") }
    mensaje?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
}

@Composable private fun MuestraWidget(tipo: TipoVista, estado: Estado, estilo: EstiloWidgets) {
    val contexto = LocalContext.current
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
}

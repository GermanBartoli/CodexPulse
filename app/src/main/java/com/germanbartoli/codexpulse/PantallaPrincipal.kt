package com.germanbartoli.codexpulse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*

class PantallaPrincipal : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Aplicacion() }
    }
}
@Composable private fun Aplicacion() {
    val contexto = LocalContext.current
    val actividad = contexto as PantallaPrincipal
    val alcance = rememberCoroutineScope()
    var estado by remember { mutableStateOf(Estado()) }
    var ocupado by remember { mutableStateOf(false) }
    var codigo by remember { mutableStateOf<CodigoDispositivo?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var acceso by remember { mutableStateOf<Job?>(null) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    var detalles by remember { mutableStateOf(false) }
    var mostrarNavegadores by remember { mutableStateOf(false) }
    var navegadores by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val preferencias = remember { contexto.getSharedPreferences("apariencia", 0) }
    var dinamico by remember { mutableStateOf(preferencias.getBoolean("dinamico", true)) }
    val oscuro = isSystemInDarkTheme()
    val colores = if(dinamico) { if(oscuro) dynamicDarkColorScheme(contexto) else dynamicLightColorScheme(contexto) }
        else if(oscuro) darkColorScheme(primary = Color(0xFF8FE4C4), background = Color(0xFF0C1714), surface = Color(0xFF13231D), secondaryContainer = Color(0xFF263D34))
        else lightColorScheme(primary = Color(0xFF146C4F), background = Color(0xFFF4FAF6), surface = Color(0xFFF4FAF6), secondaryContainer = Color(0xFFD8EEE2))
    suspend fun recargar() { estado = Repositorio.leer(contexto) }
    suspend fun refrescar() {
        ocupado = true
        try { Repositorio.actualizar(contexto); actualizarWidgets(contexto); recargar() }
        finally { ocupado = false }
    }
    DisposableEffect(codigo != null) {
        if(codigo != null) actividad.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else actividad.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { actividad.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
    }
    LaunchedEffect(Unit) {
        actividad.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            recargar()
            if(estado.conectado) { Actualizacion.programar(contexto); Actualizacion.solicitar(contexto) }
            while(isActive) { delay(15000); recargar() }
        }
    }
    MaterialTheme(colorScheme = colores) {
        Surface(Modifier.fillMaxSize(), color = colores.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("TU CONSUMO, A MANO", style = MaterialTheme.typography.labelSmall, color = colores.primary, letterSpacing = 2.sp)
                        Text("Codex Pulse", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalIconButton(onClick = { detalles = true }) { Text("?", fontSize = 22.sp) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(onClick = {}, label = { Text(if(estado.demostracion) "Demostración" else if(estado.conectado) "Cuenta conectada" else "Sin conectar") })
                    estado.consumo?.let { SuggestionChip(onClick = {}, label = { Text(it.plan.replaceFirstChar(Char::uppercase)) }) }
                }
                if(estado.demostracion) Mensaje("Datos de ejemplo", "Estos porcentajes son ilustrativos. Conectá tu cuenta para ver tu consumo real.")
                (aviso ?: estado.error)?.let { Mensaje("Estado de la consulta", it) }
                estado.consumo?.let { consumo ->
                    Text("Lo que te queda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    consumo.limites.forEach { TarjetaCuota(it) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if(estado.demostracion) "Vista previa" else antiguedad(consumo.fecha), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                        FilledTonalButton(onClick = { alcance.launch { refrescar() } }, enabled = !ocupado && estado.conectado) { Text(if(ocupado) "Consultando…" else "Actualizar") }
                    }
                    Text("Historial de disponibilidad", style = MaterialTheme.typography.titleMedium)
                    if(estado.historial.size >= 2) Grafico(estado.historial)
                    else Text("El gráfico se construye con tus próximas consultas. Los porcentajes no equivalen a una cantidad fija de tokens.", style = MaterialTheme.typography.bodyMedium, color = colores.onSurfaceVariant)
                    Text("Los tokens exactos por día no están disponibles en esta conexión.", style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                    Text("En tu pantalla de inicio", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val gestor = AppWidgetManager.getInstance(contexto)
                            if(gestor.isRequestPinAppWidgetSupported) gestor.requestPinAppWidget(ComponentName(contexto, WidgetCompacto::class.java), null, null)
                            else aviso = "Mantené presionado el inicio de Android y elegí Widgets → Codex Pulse."
                        }) { Text("Widget 2×2") }
                        OutlinedButton(onClick = {
                            val gestor = AppWidgetManager.getInstance(contexto)
                            if(gestor.isRequestPinAppWidgetSupported) gestor.requestPinAppWidget(ComponentName(contexto, WidgetCompleto::class.java), null, null)
                            else aviso = "Mantené presionado el inicio de Android y elegí Widgets → Codex Pulse."
                        }) { Text("Widget 4×2") }
                    }
                    listOf("Indicador 1×1" to WidgetMinimo::class.java, "Cuota semanal 2×2" to WidgetSemanal::class.java, "Historial 4×3" to WidgetHistorial::class.java).forEach { (nombre, receptor) ->
                        OutlinedButton(onClick = {
                            val gestor = AppWidgetManager.getInstance(contexto)
                            if(gestor.isRequestPinAppWidgetSupported) gestor.requestPinAppWidget(ComponentName(contexto, receptor), null, null)
                            else aviso = "Mantené presionado el inicio y elegí Widgets → Codex Pulse."
                        }) { Text(nombre) }
                    }
                    Text("El indicador pequeño muestra el menor porcentaje disponible entre tus límites. Los tamaños pueden variar según el inicio de tu teléfono.", style = MaterialTheme.typography.bodySmall)
                } ?: Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tu cuota de Codex,\nsin abrir la PC.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text("Consultá cuánto queda y cuándo se renuevan tus límites, desde la app o tu pantalla de inicio.", color = colores.onSurfaceVariant)
                }
                if(!estado.conectado && codigo == null) {
                    Button(onClick = {
                        aviso = null
                        acceso = alcance.launch {
                            ocupado = true
                            val intento = Repositorio.nuevaGeneracion()
                            try {
                                val solicitud = withContext(Dispatchers.IO) { Conexion.iniciar() }
                                codigo = solicitud
                                withTimeout(15 * 60 * 1000L) {
                                    while(isActive) {
                                        delay(solicitud.intervalo * 1000)
                                        val sesion = withContext(Dispatchers.IO) { Conexion.consultarAcceso(solicitud) }
                                        if(sesion != null) {
                                            if(Repositorio.conectar(contexto, sesion, intento)) {
                                                Actualizacion.programar(contexto)
                                                refrescar()
                                            }
                                            break
                                        }
                                    }
                                }
                            } catch (_: TimeoutCancellationException) { aviso = "El código venció. Iniciá la conexión otra vez." }
                            catch (error: CancellationException) { throw error }
                            catch (_: Exception) { aviso = "No se pudo conectar. Revisá tu conexión y que el acceso por código de dispositivo esté habilitado en ChatGPT." }
                            finally { codigo = null; ocupado = false; recargar() }
                        }
                    }, enabled = !ocupado, modifier = Modifier.fillMaxWidth()) { Text(if(ocupado) "Preparando acceso…" else "Conectar con ChatGPT") }
                    Text("Ingresás tus datos solo en auth.openai.com. Esta app guarda una sesión cifrada en el teléfono. No es una aplicación oficial de OpenAI.", style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                    if(!estado.demostracion) OutlinedButton(onClick = { alcance.launch { Repositorio.demostracion(contexto); recargar(); actualizarWidgets(contexto) } }, enabled = !ocupado, modifier = Modifier.fillMaxWidth()) { Text("Explorar la demostración") }
                }
                codigo?.let { solicitud ->
                    Card(shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Autorizá tu dispositivo", style = MaterialTheme.typography.titleLarge)
                            Text("Anotá este código y escribilo en la página oficial:")
                            Text(solicitud.codigo, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            Button(onClick = {
                                val consulta = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE)
                                navegadores = contexto.packageManager.queryIntentActivities(consulta, android.content.pm.PackageManager.MATCH_ALL)
                                    .map { it.loadLabel(contexto.packageManager).toString() to it.activityInfo.packageName }
                                    .distinctBy { it.second }.sortedBy { it.first }
                                mostrarNavegadores = true
                            }) { Text("Elegir navegador") }
                            Text("Volvé a esta app después de autorizar. No compartas el código.", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { acceso?.cancel(); alcance.launch { Repositorio.nuevaGeneracion() } }) { Text("Cancelar") }
                        }
                    }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Colores de la aplicación"); Text("Se adapta al modo claro y oscuro", style = MaterialTheme.typography.bodySmall) }
                    Switch(checked = dinamico, onCheckedChange = { dinamico = it; preferencias.edit().putBoolean("dinamico", it).apply() })
                }
                Text("Actualización automática cada 30 minutos aproximadamente. Android puede demorarla para ahorrar batería. El botón permite consultar manualmente.", style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                if(estado.conectado || estado.demostracion || estado.error != null) TextButton(onClick = { confirmarBorrado = true }, enabled = !ocupado) { Text("Borrar sesión y datos de este teléfono", color = colores.error) }
                Text("Hecho para mirar de un vistazo.", style = MaterialTheme.typography.labelSmall, color = colores.onSurfaceVariant)
            }
            if(mostrarNavegadores) AlertDialog(
                onDismissRequest = { mostrarNavegadores = false },
                title = { Text("Elegí un navegador") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("La autorización se abrirá en auth.openai.com con el navegador que elijas.")
                        if(navegadores.isEmpty()) Text("No se encontró un navegador compatible. Instalá o habilitá uno para continuar.")
                        navegadores.forEach { (nombre, paquete) ->
                            OutlinedButton(onClick = {
                                mostrarNavegadores = false
                                try {
                                    val pagina = Intent(Intent.ACTION_VIEW, Uri.parse(Conexion.PAGINA_ACCESO))
                                        .addCategory(Intent.CATEGORY_BROWSABLE).setPackage(paquete)
                                    contexto.startActivity(pagina)
                                } catch (_: android.content.ActivityNotFoundException) {
                                    aviso = "Ese navegador no pudo abrir la página. Elegí otro o revisá que esté habilitado."
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text(nombre) }
                        }
                        if(navegadores.size == 1) Text("Solo se encontró un navegador habilitado en este perfil del teléfono.", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { mostrarNavegadores = false }) { Text("Cancelar") } }
            )
            if(confirmarBorrado) AlertDialog(onDismissRequest = { confirmarBorrado = false }, title = { Text("¿Borrar los datos locales?") }, text = { Text("Se eliminarán la sesión, el historial y los porcentajes guardados en este teléfono. Esto no revoca otras sesiones de ChatGPT.") }, confirmButton = { TextButton(onClick = { confirmarBorrado = false; acceso?.cancel(); alcance.launch { Actualizacion.cancelar(contexto); Repositorio.borrar(contexto); aviso = null; recargar(); actualizarWidgets(contexto) } }) { Text("Borrar") } }, dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } })
            if(detalles) AlertDialog(onDismissRequest = { detalles = false }, title = { Text("Privacidad y datos") }, text = { Text("La conexión va directamente a OpenAI por HTTPS. La sesión y el historial se cifran con una clave de Android Keystore y no se incluyen en copias de seguridad.\n\nLa consulta de cuotas utiliza un servicio interno que puede cambiar. No hay servidor propio, publicidad ni analítica. Los porcentajes son límites de uso, no tokens exactos.\n\nVersión 0.1.2 · Aplicación comunitaria.") }, confirmButton = { TextButton(onClick = { detalles = false }) { Text("Entendido") } })
        }
    }
}
@Composable private fun Mensaje(titulo: String, texto: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(titulo, fontWeight = FontWeight.SemiBold); Text(texto, style = MaterialTheme.typography.bodySmall) }
    }
}
@Composable private fun TarjetaCuota(limite: Limite) {
    val colores = MaterialTheme.colorScheme
    val color = if(limite.disponible <= 20) colores.error else colores.primary
    Surface(color = colores.surfaceContainer, shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(limite.nombre, style = MaterialTheme.typography.titleMedium)
                Text("${limite.usado.toInt()}% usado", style = MaterialTheme.typography.labelMedium, color = colores.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${limite.disponible}%", fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold, color = color)
                Text(" disponible", Modifier.padding(bottom = 8.dp), color = colores.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = { limite.disponible/100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = color, trackColor = colores.surfaceContainerHighest)
            Text(limite.renovacion(), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
        }
    }
}
@Composable private fun Grafico(historial: List<Consumo>) {
    val color = MaterialTheme.colorScheme.primary
    val secundario = MaterialTheme.colorScheme.tertiary
    Canvas(Modifier.fillMaxWidth().height(110.dp).padding(vertical = 12.dp)) {
        for(indice in 0..1) {
            val puntos = historial.map { it.limites.getOrNull(indice)?.disponible }
            val linea = Path()
            var iniciado = false
            puntos.forEachIndexed { posicion, valor ->
                if(valor == null) iniciado = false else {
                    val x = posicion.toFloat()/(puntos.size-1).coerceAtLeast(1) * size.width
                    val y = (1 - valor/100f) * size.height
                    if(!iniciado) { linea.moveTo(x,y); iniciado = true } else linea.lineTo(x,y)
                }
            }
            drawPath(linea, if(indice == 0) color else secundario, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
        }
    }
    Text("Últimas ${historial.size} consultas · 0–100% disponible\nPrimario y secundario, según los límites de tu cuenta.", style = MaterialTheme.typography.bodySmall)
}

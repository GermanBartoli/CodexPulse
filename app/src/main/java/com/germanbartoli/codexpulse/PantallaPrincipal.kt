package com.germanbartoli.codexpulse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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

private enum class Seccion(val titulo: String, val simbolo: String) { INICIO("Inicio", "◉"), HISTORIAL("Historial", "↗"), AJUSTES("Ajustes", "⚙") }
class PantallaPrincipal : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Aplicacion() }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun Aplicacion() {
    val contexto = LocalContext.current
    val actividad = contexto as PantallaPrincipal
    val alcance = rememberCoroutineScope()
    val paginas = rememberPagerState { Seccion.entries.size }
    fun abrir(seccion: Seccion) {
        paginas.requestScrollToPage(seccion.ordinal)
    }
    var instalados by remember { mutableStateOf<List<WidgetInstalado>>(emptyList()) }
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
    var estiloWidgets by remember { mutableStateOf(Personalizacion.leer(contexto)) }
    fun cambiarWidgets(estilo: EstiloWidgets) {
        estiloWidgets = estilo; Personalizacion.guardar(contexto, estilo)
        alcance.launch { actualizarWidgets(contexto) }
    }
    var minutos by remember { mutableIntStateOf(Preferencias.minutos(contexto)) }
    var alertas by remember { mutableStateOf(Preferencias.alertas(contexto) && Avisos.permitido(contexto)) }
    val permisoAvisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        alertas = concedido && Avisos.permitido(contexto)
        Preferencias.alertas(contexto, alertas)
        if (!alertas) aviso = "Las alertas siguen apagadas. Podés habilitar las notificaciones desde Ajustes de Android."
    }
    val oscuro = isSystemInDarkTheme()
    val colores = if(dinamico) { if(oscuro) dynamicDarkColorScheme(contexto) else dynamicLightColorScheme(contexto) }
        else if(oscuro) darkColorScheme(primary = Color(0xFF8FE4C4), background = Color(0xFF0C1714), surface = Color(0xFF13231D), secondaryContainer = Color(0xFF263D34))
        else lightColorScheme(primary = Color(0xFF146C4F), background = Color(0xFFF4FAF6), surface = Color(0xFFF4FAF6), secondaryContainer = Color(0xFFD8EEE2))
    suspend fun recargar() { estado = Repositorio.leer(contexto); instalados = listarWidgets(contexto) }
    var actualizando by remember { mutableStateOf(false) }
    suspend fun refrescar() {
        if (actualizando) return
        actualizando = true
        ocupado = true
        try { Repositorio.actualizar(contexto); actualizarWidgets(contexto); recargar() }
        finally { actualizando = false; ocupado = false }
    }
    DisposableEffect(codigo != null) {
        if(codigo != null) actividad.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else actividad.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { actividad.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
    }
    LaunchedEffect(contexto) {
        ConfirmacionWidget.cambios.collect { instalados = listarWidgets(contexto) }
    }
    LaunchedEffect(Unit) {
        actividad.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            recargar()
            alertas = Preferencias.alertas(contexto) && Avisos.permitido(contexto)
            if(estado.conectado) { Actualizacion.programar(contexto); Actualizacion.solicitar(contexto) }
            while(isActive) { delay(15000); recargar() }
        }
    }
    MaterialTheme(colorScheme = colores) {
        Scaffold(containerColor = colores.background, bottomBar = {
            NavigationBar {
                Seccion.entries.forEach { destino ->
                    NavigationBarItem(selected = paginas.currentPage == destino.ordinal, onClick = { abrir(destino) },
                        icon = { Text(destino.simbolo, fontSize = 22.sp) }, label = { Text(destino.titulo) })
                }
            }
        }) { margenes ->
            HorizontalPager(paginas, Modifier.fillMaxSize().padding(margenes).consumeWindowInsets(margenes), beyondViewportPageCount = 2, verticalAlignment = Alignment.Top) { pagina ->
            val seccion = Seccion.entries[pagina]
            val desplazamiento = rememberScrollState()
            val contenido: @Composable () -> Unit = {
            Column(Modifier.fillMaxSize().verticalScroll(desplazamiento).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("TU CONSUMO, A MANO", style = MaterialTheme.typography.labelSmall, color = colores.primary, letterSpacing = 2.sp)
                        Text("Pulso de Germán", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalIconButton(onClick = { detalles = true }) { Text("?", fontSize = 22.sp) }
                }
                if (seccion == Seccion.INICIO) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(onClick = {}, label = { Text(if(estado.demostracion) "Demostración" else if(estado.conectado) "Cuenta conectada" else "Sin conectar") })
                    estado.consumo?.let { SuggestionChip(onClick = {}, label = { Text(it.plan.replaceFirstChar(Char::uppercase)) }) }
                }
                if(estado.demostracion) Mensaje("Datos de ejemplo", "Estos porcentajes son ilustrativos. Conectá tu cuenta para ver tu consumo real.")
                (aviso ?: estado.error)?.let { Mensaje("Estado de la consulta", it) }
                estado.consumo?.let { consumo ->
                    Text("Lo que te queda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    consumo.limites.forEach { TarjetaCuota(it) }
                    AvisarRecuperacion(estado)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if(estado.demostracion) "Vista previa" else antiguedad(consumo.fecha), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                        FilledTonalButton(onClick = { alcance.launch { refrescar() } }, enabled = !ocupado && estado.conectado) { Text(if(ocupado) "Consultando…" else "Actualizar") }
                    }
                    Button(onClick = { abrir(Seccion.AJUSTES) }, modifier = Modifier.fillMaxWidth()) { Text("Elegir y agregar widgets") }
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
                }
                if (seccion == Seccion.INICIO) {
                    HorizontalDivider()
                    Text("Tus widgets agregados", style = MaterialTheme.typography.titleLarge)
                    if (instalados.isEmpty()) {
                        Text("Todavía no agregaste widgets de Pulso de Germán a tu pantalla de inicio.", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { abrir(Seccion.AJUSTES) }) { Text("Ver los widgets disponibles") }
                    } else {
                        Text(if (instalados.size == 1) "Tenés 1 widget en tu inicio" else "Tenés ${instalados.size} widgets en tu inicio", style = MaterialTheme.typography.bodySmall)
                        instalados.forEachIndexed { indice, widget ->
                            OutlinedCard(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("${widget.nombre} · ${indice + 1}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                    TextButton(onClick = {
                                        contexto.startActivity(Intent(contexto, ConfiguracionWidget::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget.id))
                                    }) { Text("Configurar") }
                                }
                            }
                        }
                    }
                }
                if (seccion == Seccion.HISTORIAL) {
                    Text("Historial de disponibilidad", style = MaterialTheme.typography.headlineSmall)
                    if (estado.demostracion) Mensaje("Datos de ejemplo", "El historial es ilustrativo y no corresponde a una cuenta real.")
                    if (estado.historial.size >= 2) Grafico(estado.historial)
                    else Mensaje("Todavía no hay historial", "Se construye con las consultas de esta app. Volvé después de las próximas actualizaciones.")
                    Text("Se guardan hasta 96 consultas en este teléfono. Los porcentajes no representan una cantidad fija de tokens.", style = MaterialTheme.typography.bodySmall)
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
                if (seccion == Seccion.AJUSTES) {
                Text("Widgets de tu inicio", style = MaterialTheme.typography.titleMedium)
                if (instalados.isEmpty()) Text("Agregá un widget desde la vista previa. Después podrás configurarlo por separado.", style = MaterialTheme.typography.bodySmall)
                instalados.forEachIndexed { indice, widget ->
                    OutlinedButton(onClick = {
                        contexto.startActivity(Intent(contexto, ConfiguracionWidget::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget.id))
                    }, modifier = Modifier.fillMaxWidth()) { Text("${widget.nombre} · ${indice + 1}") }
                }
                VistaPreviaWidgets(estado, estiloWidgets)
                HorizontalDivider()
                AjustesNotificacionFija(estado)
                HorizontalDivider()
                AjustesAcceso()
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Colores de la aplicación"); Text("Se adapta al modo claro y oscuro", style = MaterialTheme.typography.bodySmall) }
                    Switch(checked = dinamico, onCheckedChange = { dinamico = it; preferencias.edit().putBoolean("dinamico", it).apply() })
                }
                Text("Estilo por defecto", style = MaterialTheme.typography.titleMedium)
                Text("Se aplica a los widgets que no tienen un estilo propio.", style = MaterialTheme.typography.bodySmall)
                EditorEstiloWidgets(estiloWidgets) { cambiarWidgets(it) }
                Text("Frecuencia de actualización", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60).forEach { frecuencia ->
                        FilterChip(selected = minutos == frecuencia, onClick = {
                            minutos = frecuencia
                            Preferencias.frecuencia(contexto, frecuencia)
                            if (estado.conectado) Actualizacion.programar(contexto)
                        }, label = { Text("$frecuencia min") })
                    }
                }
                Text("Android puede demorar las consultas para ahorrar batería. El botón Actualizar permite consultar manualmente.", style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Avisarme cuando quede poco")
                        Text("Elegí cuándo recibir avisos para cada límite.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = alertas, onCheckedChange = { activar ->
                        if (activar && android.os.Build.VERSION.SDK_INT >= 33 &&
                            contexto.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            permisoAvisos.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            alertas = activar && Avisos.permitido(contexto)
                            Preferencias.alertas(contexto, alertas)
                            if (activar && !alertas) aviso = "Android bloquea las notificaciones. Habilitalas en Ajustes → Aplicaciones → Pulso de Germán → Notificaciones."
                        }
                    })
                }
                AjustesAlertas()
                Text("Se revisa el consumo en cada consulta; las alertas no son instantáneas. La pantalla bloqueada oculta el porcentaje.", style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                if(estado.conectado || estado.demostracion || estado.error != null) TextButton(onClick = { confirmarBorrado = true }, enabled = !ocupado) { Text("Borrar sesión y datos de este teléfono", color = colores.error) }
                Text("Hecho para mirar de un vistazo.", style = MaterialTheme.typography.labelSmall, color = colores.onSurfaceVariant)
                }
            }
            }
            if (seccion == Seccion.INICIO && estado.conectado) {
                PullToRefreshBox(
                    isRefreshing = actualizando,
                    onRefresh = { if (!ocupado) alcance.launch { refrescar() } },
                    modifier = Modifier.fillMaxSize()
                ) { contenido() }
            } else contenido()
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
            if(detalles) AlertDialog(onDismissRequest = { detalles = false }, title = { Text("Privacidad y datos") }, text = { Text("La conexión va directamente a OpenAI por HTTPS. La sesión y el historial se cifran con una clave de Android Keystore y no se incluyen en copias de seguridad.\n\nLa consulta de cuotas utiliza un servicio interno que puede cambiar. No hay servidor propio, publicidad ni analítica. Los porcentajes son límites de uso, no tokens exactos.\n\nVersión 0.8.1 · Aplicación comunitaria.") }, confirmButton = { TextButton(onClick = { detalles = false }) { Text("Entendido") } })
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

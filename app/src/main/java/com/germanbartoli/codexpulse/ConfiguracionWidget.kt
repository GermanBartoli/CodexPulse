package com.germanbartoli.codexpulse

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class WidgetInstalado(val id: Int, val nombre: String, val variante: Variante)
fun varianteProveedor(nombre: String): Variante? = when(nombre) {
    WidgetMinimo::class.java.name -> Variante.MINIMO
    WidgetSemanal::class.java.name -> Variante.SEMANAL
    WidgetCompacto::class.java.name -> Variante.COMPACTO
    WidgetCompleto::class.java.name -> Variante.COMPLETO
    WidgetHistorial::class.java.name -> Variante.HISTORIAL
    else -> null
}
fun listarWidgets(contexto: Context): List<WidgetInstalado> {
    val gestor = AppWidgetManager.getInstance(contexto)
    return gestor.installedProviders.filter { it.provider.packageName == contexto.packageName }.flatMap { proveedor ->
        val variante = varianteProveedor(proveedor.provider.className) ?: return@flatMap emptyList()
        gestor.getAppWidgetIds(proveedor.provider).map { id ->
            val nombre = contexto.getSharedPreferences("nombres_widgets", 0).getString(id.toString(), null)
            WidgetInstalado(id, nombre ?: proveedor.loadLabel(contexto.packageManager).toString(), variante)
        }
    }.sortedWith(compareBy({ it.variante.ordinal }, { it.id }))
}
private fun widgetReal(variante: Variante): GlanceAppWidget = when(variante) {
    Variante.MINIMO -> MinimoGlance(); Variante.SEMANAL -> SemanalGlance()
    Variante.COMPACTO -> CompactoGlance(); Variante.COMPLETO -> CompletoGlance()
    Variante.HISTORIAL -> HistorialGlance()
}

class ConfiguracionWidget : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val numero = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val proveedor = AppWidgetManager.getInstance(this).getAppWidgetInfo(numero)
        val variante = proveedor?.takeIf { it.provider.packageName == packageName }?.let { varianteProveedor(it.provider.className) }
        if (numero == AppWidgetManager.INVALID_APPWIDGET_ID || variante == null) { finish(); return }
        val id = GlanceAppWidgetManager(this).getGlanceIdBy(numero)
        enableEdgeToEdge()
        setContent {
            val alcance = rememberCoroutineScope()
            var estado by remember { mutableStateOf(Estado()) }
            val predeterminado = remember { Personalizacion.leer(this) }
            var estilo by remember { mutableStateOf(predeterminado) }
            var heredar by remember { mutableStateOf(true) }
            val nombres = remember { getSharedPreferences("nombres_widgets", 0) }
            var nombre by remember { mutableStateOf(nombres.getString(numero.toString(), "") ?: "") }
            var explicarRetiro by remember { mutableStateOf(false) }
            var listo by remember { mutableStateOf(false) }
            var guardando by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                try {
                    estado = Repositorio.leer(this@ConfiguracionWidget)
                    val propio = Personalizacion.leerPropio(this@ConfiguracionWidget, id)
                    heredar = propio == null
                    estilo = propio ?: predeterminado
                    listo = true
                } catch (cancelacion: CancellationException) { throw cancelacion }
                catch (_: Exception) { error = "No se pudo abrir la configuración de este widget." }
            }
            val colores = if (isSystemInDarkTheme()) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
            MaterialTheme(colorScheme = colores) {
                if (explicarRetiro) AlertDialog(
                    onDismissRequest = { explicarRetiro = false },
                    title = { Text("Quitar del inicio") },
                    text = { Text("En la pantalla de inicio del teléfono, mantené presionado el widget y elegí Quitar o arrastralo hasta Eliminar.") },
                    dismissButton = { TextButton(onClick = { explicarRetiro = false }) { Text("Volver") } },
                    confirmButton = {
                        TextButton(onClick = {
                            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
                            finish()
                        }) { Text("Ir al inicio del teléfono") }
                    }
                )
                Scaffold(containerColor = colores.background, bottomBar = {
                    Surface {
                        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { finish() }, enabled = !guardando, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                            Button(onClick = {
                                alcance.launch {
                                    guardando = true
                                    try {
                                        Personalizacion.guardarPropio(this@ConfiguracionWidget, id, if (heredar) null else estilo)
                                        widgetReal(variante).update(this@ConfiguracionWidget, id)
                                        nombres.edit().putString(numero.toString(), nombre.trim().ifEmpty { null }).apply()
                                        ConfirmacionWidget.notificar()
                                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, numero))
                                        finish()
                                    } catch (cancelacion: CancellationException) { throw cancelacion }
                                    catch (_: Exception) { error = "No se pudo aplicar el cambio. Intentá nuevamente." }
                                    finally { guardando = false }
                                }
                            }, enabled = listo && !guardando, modifier = Modifier.weight(1f)) { Text(if (guardando) "Guardando…" else "Guardar widget") }
                        }
                    }
                }) { margenes ->
                    Column(Modifier.fillMaxSize().padding(margenes).consumeWindowInsets(margenes).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Solo este widget", style = MaterialTheme.typography.headlineMedium)
                        Text(proveedor.loadLabel(packageManager).toString(), style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(nombre, { nombre = it.take(60) }, singleLine = true, label = { Text("Nombre de este widget") })
                        if (intent.action != AppWidgetManager.ACTION_APPWIDGET_CONFIGURE) {
                            TextButton(onClick = { explicarRetiro = true }, enabled = !guardando) { Text("Cómo quitar este widget") }
                        }
                        error?.let { Text(it, color = colores.error) }
                        if (listo) {
                            VistaPreviaWidgets(estado, estilo, tipoFijo = variante)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Usar el estilo por defecto", modifier = Modifier.weight(1f))
                                Switch(checked = heredar, onCheckedChange = { heredar = it; if (it) estilo = predeterminado })
                            }
                            Text("Los cambios se aplican al guardar. Los otros widgets conservan su configuración.", style = MaterialTheme.typography.bodySmall)
                            EditorEstiloWidgets(estilo, mostrarCuota = variante == Variante.MINIMO) { estilo = it; heredar = false }
                        } else if (error == null) CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
package com.germanbartoli.codexpulse

import android.Manifest
import android.app.TimePickerDialog
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable private fun permisoNotificacion(canal: String, activar: () -> Unit): () -> Unit {
    val contexto = LocalContext.current
    val accion by rememberUpdatedState(activar)
    fun comprobar() {
        if (Avisos.permitido(contexto, canal)) accion()
        else contexto.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName))
    }
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) comprobar() else Toast.makeText(contexto, "Habilitá las notificaciones en los ajustes del teléfono.", Toast.LENGTH_LONG).show()
    }
    return {
        if (Build.VERSION.SDK_INT >= 33 && contexto.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        else comprobar()
    }
}

@Composable fun AvisarRecuperacion(estado: Estado) {
    val contexto = LocalContext.current
    val p = Preferencias.datos(contexto)
    var espera by remember { mutableStateOf(!p.getStringSet("espera", emptySet()).isNullOrEmpty()) }
    DisposableEffect(p) {
        val escucha = SharedPreferences.OnSharedPreferenceChangeListener { _, clave ->
            if (clave == "espera") espera = !p.getStringSet("espera", emptySet()).isNullOrEmpty()
        }
        p.registerOnSharedPreferenceChangeListener(escucha)
        onDispose { p.unregisterOnSharedPreferenceChangeListener(escucha) }
    }
    val agotados = estado.consumo?.limites.orEmpty().filter { it.disponible == 0 }.map { it.segundos.toString() }.toSet()
    val activar = permisoNotificacion("cuota") {
        p.edit().putStringSet("espera", agotados).apply()
    }
    if (estado.conectado && (agotados.isNotEmpty() || espera)) {
        OutlinedButton(onClick = { if (espera) p.edit().remove("espera").apply() else activar() }) {
            Text(if (espera) "Cancelar aviso de recuperación" else "Avisame cuando vuelva a tener cuota")
        }
    }
}

@Composable private fun Interruptor(titulo: String, activo: Boolean, cambiar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, Modifier.weight(1f))
        Switch(checked = activo, onCheckedChange = cambiar)
    }
}

@Composable fun AjustesAlertas() {
    val contexto = LocalContext.current
    val p = Preferencias.datos(contexto)
    listOf(18000L to "5 horas", 604800L to "Semanal").forEach { (duracion, nombre) ->
        val clave = "umbral_$duracion"
        var umbral by remember { mutableIntStateOf(p.getInt(clave, 0)) }
        var editar by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { editar = true }) { Text("$nombre · " + if (umbral == 0) "20% y 10%" else "$umbral%") }
        if (editar) {
            var valor by remember { mutableIntStateOf(if (umbral == 0) 20 else umbral) }
            fun guardar(n: Int) { umbral = n; p.edit().putInt(clave, n).apply(); editar = false }
            AlertDialog(onDismissRequest = { editar = false }, title = { Text("Umbral · $nombre") },
                text = { Column { Text("Avisarme al llegar al $valor%")
                    Slider(valor.toFloat(), { valor = kotlin.math.round(it).toInt() }, valueRange = 1f..99f, steps = 97)
                    TextButton(onClick = { guardar(0) }) { Text("Usar 20% y 10%") }
                } }, confirmButton = { TextButton(onClick = { guardar(valor) }) { Text("Guardar") } },
                dismissButton = { TextButton(onClick = { editar = false }) { Text("Cancelar") } })
        }
    }
    var silencio by remember { mutableStateOf(p.getBoolean("silencio", false)) }
    Interruptor("Horario de silencio", silencio) { silencio = it; p.edit().putBoolean("silencio", it).apply() }
    if (silencio) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("desde" to 1320, "hasta" to 480).forEach { (clave, inicial) ->
            var minutos by remember { mutableIntStateOf(p.getInt(clave, inicial)) }
            OutlinedButton(onClick = {
                TimePickerDialog(contexto, { _, h, m -> minutos = h * 60 + m; p.edit().putInt(clave, minutos).apply() }, minutos / 60, minutos % 60, true).show()
            }) { Text(String.format(Locale.ROOT, "%s %02d:%02d", clave.replaceFirstChar(Char::uppercase), minutos / 60, minutos % 60)) }
        }
    }
}

@Composable fun AjustesAcceso() {
    val contexto = LocalContext.current
    val p = Preferencias.datos(contexto)
    Text("Centro de control", style = MaterialTheme.typography.titleLarge)
    var duracion by remember { mutableLongStateOf(p.getLong("cuota_acceso", 604800)) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(18000L to "5 horas", 604800L to "Semanal").forEach { (valor, nombre) ->
            FilterChip(duracion == valor, { duracion = valor; p.edit().putLong("cuota_acceso", valor).apply(); AccesoCuota.refrescar(contexto) }, label = { Text(nombre) })
        }
    }
    if (Build.VERSION.SDK_INT >= 33) OutlinedButton(onClick = {
        contexto.getSystemService(StatusBarManager::class.java).requestAddTileService(
            ComponentName(contexto, AccesoCuota::class.java), "Cuota de Codex", Icon.createWithResource(contexto, R.drawable.ic_aviso),
            contexto.mainExecutor) { resultado ->
                val mensaje = when (resultado) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "Acceso agregado."
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "El acceso ya está agregado."
                    else -> "Podés agregar Cuota de Codex al editar el centro de control."
                }
                Toast.makeText(contexto, mensaje, Toast.LENGTH_SHORT).show()
            }
    }) { Text("Agregar al centro de control") }
    else Text("Editá los accesos del centro de control y agregá Cuota de Codex.")
}

@Composable fun AjustesNotificacionFija(estado: Estado) {
    val contexto = LocalContext.current
    val p = Preferencias.datos(contexto)
    var fija by remember(estado.conectado) { mutableStateOf(p.getBoolean("fija", false)) }
    var titulo by remember { mutableStateOf(p.getString("titulo_fijo", null) ?: "Mi consumo de Codex") }
    var cuerpo by remember { mutableStateOf(p.getString("texto_fijo", null) ?: PLANTILLA) }
    val activar = permisoNotificacion(ConsumoVisible.CANAL) {
        fija = true
        p.edit().putBoolean("fija", true).apply()
        ConsumoVisible.publicar(contexto, estado)
    }
    Text("Pantalla de bloqueo", style = MaterialTheme.typography.titleLarge)
    Interruptor("Mostrar mi consumo", fija) {
        if (it && estado.conectado) activar()
        else if (it) Toast.makeText(contexto, "Conectá tu cuenta para mostrar tu consumo.", Toast.LENGTH_SHORT).show()
        else { fija = false; p.edit().putBoolean("fija", false).apply(); ConsumoVisible.publicar(contexto, estado) }
    }
    Text("Al activarla, tu consumo queda visible con el teléfono bloqueado. Android controla la visibilidad final.", style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(titulo, { titulo = it.take(90) }, label = { Text("Título de la notificación") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(cuerpo, { cuerpo = it.take(500) }, label = { Text("Texto de la notificación") }, modifier = Modifier.fillMaxWidth())
    Text("En el título y el texto podés usar {semanal}, {5h}, {plan} y {hora}.", style = MaterialTheme.typography.bodySmall)
    Text(textoConsumo(titulo.ifBlank { "Mi consumo de Codex" }, estado.consumo), style = MaterialTheme.typography.titleSmall)
    Text(textoConsumo(cuerpo.ifBlank { PLANTILLA }, estado.consumo))
    OutlinedButton(onClick = {
        p.edit().putString("titulo_fijo", titulo).putString("texto_fijo", cuerpo).apply()
        ConsumoVisible.publicar(contexto, estado)
        Toast.makeText(contexto, "Notificación guardada.", Toast.LENGTH_SHORT).show()
    }) { Text("Guardar notificación") }
}

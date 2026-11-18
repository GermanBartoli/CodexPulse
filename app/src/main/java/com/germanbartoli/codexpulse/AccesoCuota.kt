package com.germanbartoli.codexpulse

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.*

class AccesoCuota : TileService() {
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override fun onTileAdded() {
        super.onTileAdded()
        refrescar(this)
    }
    override fun onStartListening() {
        super.onStartListening()
        alcance.launch {
            val estado = Repositorio.leer(this@AccesoCuota)
            val duracion = Preferencias.datos(this@AccesoCuota).getLong("cuota_acceso", 604800)
            val limite = estado.consumo?.limites?.find { it.segundos == duracion }
            qsTile?.apply {
                val periodo = if (duracion == 604800L) "Semanal" else "5 horas"
                label = if (estado.conectado && limite != null) "$periodo · ${limite.disponible}%" else periodo
                subtitle = when {
                    !estado.conectado -> "Conectá tu cuenta"
                    limite == null -> "Sin dato"
                    estado.error != null -> "${limite.disponible}% · sin actualizar"
                    else -> "${limite.disponible}% disponible"
                }
                state = if (estado.conectado) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                updateTile()
            }
        }
    }
    override fun onClick() {
        super.onClick()
        Actualizacion.solicitar(this)
    }
    override fun onDestroy() { alcance.cancel(); super.onDestroy() }
    companion object {
        fun refrescar(contexto: Context) {
            requestListeningState(contexto, ComponentName(contexto, AccesoCuota::class.java))
        }
    }
}

package com.germanbartoli.codexpulse

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import kotlinx.coroutines.CancellationException

data class Estado(val consumo: Consumo? = null, val conectado: Boolean = false, val demostracion: Boolean = false, val error: String? = null, val historial: List<Consumo> = emptyList())
object Repositorio {
    private val bloqueo = Mutex()
    private var generacion = 0L
    private fun estado(datos: JSONObject) = Estado(
        consumo = datos.optString("consumo").takeIf { it.isNotBlank() }?.let(Consumo::leer),
        conectado = datos.has("sesion"),
        demostracion = datos.optBoolean("demostracion"),
        error = datos.optString("error").takeIf { it.isNotBlank() },
        historial = datos.optJSONArray("historial")?.let { lista -> (0 until lista.length()).map { Consumo.leer(lista.getString(it)) } } ?: emptyList()
    )
    suspend fun leer(contexto: Context): Estado = withContext(Dispatchers.IO) { bloqueo.withLock {
        try { estado(AlmacenSeguro(contexto).leer()) } catch (_: Exception) { Estado(error = "No se pudo abrir el almacenamiento seguro. Borrá los datos locales y conectá nuevamente.") }
    } }
    suspend fun nuevaGeneracion(): Long = bloqueo.withLock { ++generacion }
    suspend fun conectar(contexto: Context, sesion: JSONObject, intento: Long): Boolean = withContext(Dispatchers.IO) { bloqueo.withLock {
        if (intento != generacion) return@withLock false
        AlmacenSeguro(contexto).guardar(JSONObject().put("sesion", sesion))
        true
    } }
    suspend fun demostracion(contexto: Context) = withContext(Dispatchers.IO) { bloqueo.withLock {
        ++generacion
        val ejemplo = Consumo.ejemplo()
        AlmacenSeguro(contexto).guardar(JSONObject().put("demostracion", true).put("consumo", ejemplo.json()))
    } }
    suspend fun borrar(contexto: Context) = withContext(Dispatchers.IO) { bloqueo.withLock { ++generacion; AlmacenSeguro(contexto).borrar() } }
    suspend fun actualizar(contexto: Context): Boolean = withContext(Dispatchers.IO) { bloqueo.withLock {
        val almacen = AlmacenSeguro(contexto)
        val datos = try { almacen.leer() } catch (_: Exception) { return@withLock false }
        if (datos.optBoolean("demostracion") || !datos.has("sesion")) return@withLock true
        try {
            var sesion = datos.getJSONObject("sesion")
            if (sesion.optLong("exp") <= System.currentTimeMillis()/1000 + 300) {
                sesion = Conexion.renovar(sesion)
                datos.put("sesion", sesion)
                almacen.guardar(datos)
            }
            val consumo = try { Conexion.consumo(sesion) } catch (error: ErrorHttp) {
                if (error.estado != 401) throw error
                sesion = Conexion.renovar(sesion)
                datos.put("sesion", sesion)
                almacen.guardar(datos)
                Conexion.consumo(sesion)
            }
            val anterior = datos.optJSONArray("historial") ?: JSONArray()
            val lista = JSONArray()
            // Se retienen como máximo 96 consultas reales, sin inferir tokens.
            for (indice in (anterior.length() - 95).coerceAtLeast(0) until anterior.length()) lista.put(anterior.getString(indice))
            lista.put(consumo.json())
            datos.put("consumo", consumo.json()).put("historial", lista).remove("error")
            almacen.guardar(datos)
            true
        } catch (error: CancellationException) { throw error }
        catch (error: Exception) {
            val mensaje = when (error) {
                is ErrorHttp -> when(error.estado) {
                    400, 401 -> "La sesión necesita renovarse. Volvé a conectar tu cuenta."
                    403 -> "ChatGPT rechazó la consulta. Revisá el acceso de tu cuenta."
                    429 -> "Hay demasiadas consultas. Esperá unos minutos."
                    else -> "El servicio no está disponible. Se conserva el último dato."
                }
                is IOException -> "Sin conexión. Se conserva el último dato."
                else -> "No se pudo actualizar el consumo. Se conserva el último dato."
            }
            if (error is ErrorHttp && error.estado in listOf(400,401)) datos.remove("sesion")
            datos.put("error", mensaje)
            almacen.guardar(datos)
            false
        }
    } }
}

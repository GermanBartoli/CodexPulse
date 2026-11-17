package com.germanbartoli.codexpulse

import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.net.URLEncoder
import java.util.Base64

/** Integración comunitaria: los destinos están fijos y nunca se siguen redirecciones con credenciales. */
object Conexion {
    private const val CLIENTE = "app_EMoamEEZ73f0CkXaXp7hrann"
    const val PAGINA_ACCESO = "https://auth.openai.com/codex/device"
    private fun solicitar(url: String, cuerpo: String? = null, formulario: Boolean = false, sesion: JSONObject? = null): JSONObject {
        val destino = URL(url)
        require(destino.protocol == "https" && destino.host in setOf("auth.openai.com", "chatgpt.com"))
        val conexion = (destino.openConnection() as HttpsURLConnection).apply {
            requestMethod = if (cuerpo == null) "GET" else "POST"
            instanceFollowRedirects = false
            connectTimeout = 15000
            readTimeout = 20000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "CodexPulse/0.1.0")
            sesion?.let {
                require(destino.host == "chatgpt.com")
                setRequestProperty("Authorization", "Bearer ${it.getString("access_token")}")
                setRequestProperty("ChatGPT-Account-Id", it.getString("cuenta"))
            }
        }
        try {
            if (cuerpo != null) {
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", if (formulario) "application/x-www-form-urlencoded" else "application/json")
                conexion.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(cuerpo) }
            }
            val estado = conexion.responseCode
            if (estado !in 200..299) { conexion.errorStream?.close(); throw ErrorHttp(estado) }
            return conexion.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        } finally { conexion.disconnect() }
    }
    private fun datosToken(token: String): JSONObject {
        val partes = token.split('.')
        require(partes.size == 3) { "La sesión recibida no tiene el formato esperado." }
        // Solo se leen metadatos; estos datos no se usan como prueba de autorización.
        return JSONObject(String(Base64.getUrlDecoder().decode(partes[1]), Charsets.UTF_8))
    }
    private fun completar(respuesta: JSONObject, anterior: JSONObject? = null): JSONObject {
        val acceso = respuesta.getString("access_token")
        require(acceso.isNotBlank())
        val datos = datosToken(acceso)
        val identidad = respuesta.optString("id_token").takeIf { it.isNotBlank() }?.let(::datosToken)
        val cuenta = identidad?.optJSONObject("https://api.openai.com/auth")?.optString("chatgpt_account_id")?.takeIf { it.isNotBlank() }
            ?: datos.optJSONObject("https://api.openai.com/auth")?.optString("chatgpt_account_id")?.takeIf { it.isNotBlank() }
            ?: anterior?.optString("cuenta")?.takeIf { it.isNotBlank() }
            ?: error("No se pudo identificar la cuenta de Codex.")
        val refresco = respuesta.optString("refresh_token").takeIf { it.isNotBlank() } ?: anterior?.getString("refresh_token") ?: error("No se recibió la sesión renovable.")
        return JSONObject().put("access_token", acceso).put("refresh_token", refresco).put("cuenta", cuenta).put("exp", datos.optLong("exp"))
    }
    fun iniciar(): CodigoDispositivo {
        val respuesta = solicitar("https://auth.openai.com/api/accounts/deviceauth/usercode", JSONObject().put("client_id", CLIENTE).toString())
        val codigo = respuesta.optString("user_code", respuesta.optString("usercode"))
        require(codigo.isNotBlank()) { "No se recibió un código de acceso." }
        return CodigoDispositivo(respuesta.getString("device_auth_id"), codigo, respuesta.optLong("interval", 5).coerceIn(5, 60))
    }
    fun consultarAcceso(codigo: CodigoDispositivo): JSONObject? {
        val pendiente = try {
            solicitar("https://auth.openai.com/api/accounts/deviceauth/token", JSONObject().put("device_auth_id", codigo.id).put("user_code", codigo.codigo).toString())
        } catch (error: ErrorHttp) { if (error.estado in listOf(403, 404)) return null else throw error }
        val valores = mapOf("grant_type" to "authorization_code", "code" to pendiente.getString("authorization_code"), "redirect_uri" to "https://auth.openai.com/deviceauth/callback", "client_id" to CLIENTE, "code_verifier" to pendiente.getString("code_verifier"))
        val cuerpo = valores.entries.joinToString("&") { (clave, valor) -> "$clave=${URLEncoder.encode(valor, "UTF-8")}" }
        return completar(solicitar("https://auth.openai.com/oauth/token", cuerpo, true))
    }
    fun renovar(sesion: JSONObject): JSONObject = completar(solicitar("https://auth.openai.com/oauth/token", JSONObject().put("client_id", CLIENTE).put("grant_type", "refresh_token").put("refresh_token", sesion.getString("refresh_token")).toString()), sesion)
    fun consumo(sesion: JSONObject): Consumo = Consumo.leer(solicitar("https://chatgpt.com/backend-api/wham/usage", sesion = sesion).toString())
}

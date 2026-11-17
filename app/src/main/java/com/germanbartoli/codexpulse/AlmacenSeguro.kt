package com.germanbartoli.codexpulse

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

/** Sesión, cuota e historial quedan cifrados y excluidos de las copias de seguridad. */
class AlmacenSeguro(contexto: Context) {
    private val archivo = AtomicFile(File(contexto.noBackupFilesDir, "pulse.cifrado"))
    private fun clave(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey("codexpulse.v1", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("codexpulse.v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    fun leer(): JSONObject {
        if (!archivo.baseFile.exists()) return JSONObject()
        val bytes = archivo.openRead().use { it.readBytes() }
        require(bytes.size > 28) { "No se pudo abrir el almacenamiento seguro." }
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return JSONObject(String(cifrador.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
    }
    fun guardar(datos: JSONObject) {
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.ENCRYPT_MODE, clave())
        val salida = archivo.startWrite()
        try { salida.write(cifrador.iv + cifrador.doFinal(datos.toString().toByteArray(Charsets.UTF_8))); archivo.finishWrite(salida) }
        catch (error: Exception) { archivo.failWrite(salida); throw error }
    }
    fun borrar() { archivo.delete() }
}

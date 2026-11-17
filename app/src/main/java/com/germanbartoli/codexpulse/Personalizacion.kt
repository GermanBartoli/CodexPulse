package com.germanbartoli.codexpulse

import android.content.Context

enum class PaletaWidget(val titulo: String) { TELEFONO("Del teléfono"), PULSE("Verde Pulse") }
enum class FondoWidget(val titulo: String) { ADAPTABLE("Adaptable"), OSCURO("Oscuro"), TRANSPARENTE("Transparente") }
enum class CuotaIndicador(val titulo: String) { MENOR("Menor disponible"), CINCO_HORAS("Límite de 5 horas"), SEMANAL("Límite semanal") }
data class EstiloWidgets(
    val paleta: PaletaWidget = PaletaWidget.TELEFONO,
    val fondo: FondoWidget = FondoWidget.ADAPTABLE,
    val cuota: CuotaIndicador = CuotaIndicador.MENOR
)
object Personalizacion {
    fun leer(contexto: Context): EstiloWidgets {
        val datos = contexto.getSharedPreferences("widgets", Context.MODE_PRIVATE)
        return EstiloWidgets(
            PaletaWidget.entries.firstOrNull { it.name == datos.getString("paleta", null) } ?: PaletaWidget.TELEFONO,
            FondoWidget.entries.firstOrNull { it.name == datos.getString("fondo", null) } ?: FondoWidget.ADAPTABLE,
            CuotaIndicador.entries.firstOrNull { it.name == datos.getString("cuota", null) } ?: CuotaIndicador.MENOR
        )
    }
    fun guardar(contexto: Context, estilo: EstiloWidgets) {
        contexto.getSharedPreferences("widgets", Context.MODE_PRIVATE).edit()
            .putString("paleta", estilo.paleta.name).putString("fondo", estilo.fondo.name).putString("cuota", estilo.cuota.name).apply()
    }
}
fun limiteIndicador(limites: List<Limite>, cuota: CuotaIndicador): Limite? = when (cuota) {
    CuotaIndicador.MENOR -> limites.minByOrNull { it.disponible }
    CuotaIndicador.CINCO_HORAS -> limites.firstOrNull { it.segundos == 18000L }
    CuotaIndicador.SEMANAL -> limites.firstOrNull { it.segundos == 604800L }
}
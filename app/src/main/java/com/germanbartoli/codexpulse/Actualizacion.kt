package com.germanbartoli.codexpulse

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import androidx.glance.appwidget.updateAll

class Actualizacion(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {
    override suspend fun doWork(): Result {
        val correcto = Repositorio.actualizar(applicationContext)
        actualizarWidgets(applicationContext)
        return if (correcto || runAttemptCount >= 2) Result.success() else Result.retry()
    }
    companion object {
        private fun restricciones() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        fun programar(contexto: Context) {
            WorkManager.getInstance(contexto).enqueueUniquePeriodicWork("consumo-periodico", ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<Actualizacion>(Preferencias.minutos(contexto).toLong(), TimeUnit.MINUTES).setConstraints(restricciones()).build())
        }
        fun solicitar(contexto: Context) {
            WorkManager.getInstance(contexto).enqueueUniqueWork("consumo-manual", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<Actualizacion>().setConstraints(restricciones()).build())
        }
        fun cancelar(contexto: Context) {
            WorkManager.getInstance(contexto).cancelUniqueWork("consumo-periodico")
            WorkManager.getInstance(contexto).cancelUniqueWork("consumo-manual")
        }
    }
}
suspend fun actualizarWidgets(contexto: Context) {
    ConsumoVisible.publicar(contexto, Repositorio.leer(contexto))
    AccesoCuota.refrescar(contexto)
    CompactoGlance().updateAll(contexto); CompletoGlance().updateAll(contexto); MinimoGlance().updateAll(contexto); SemanalGlance().updateAll(contexto); HistorialGlance().updateAll(contexto) }

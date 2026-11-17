package com.germanbartoli.codexpulse

import org.junit.Assert.*
import org.junit.Test

class PersonalizacionTest {
    private val limites = listOf(Limite(20.0, null, 18000), Limite(70.0, null, 604800))
    @Test fun indicadorAutomaticoUsaLaMenorCuota() { assertEquals(30, limiteIndicador(limites, CuotaIndicador.MENOR)?.disponible) }
    @Test fun permiteElegirCincoHoras() { assertEquals(80, limiteIndicador(limites, CuotaIndicador.CINCO_HORAS)?.disponible) }
    @Test fun semanalNoDependeDelOrden() { assertEquals(30, limiteIndicador(limites.reversed(), CuotaIndicador.SEMANAL)?.disponible) }
    @Test fun limiteAusenteNoSeSustituyePorOtro() { assertNull(limiteIndicador(limites.take(1), CuotaIndicador.SEMANAL)) }
    @Test fun historialRespetaElPeriodoYOrdenaLasFechas() {
        val ahora = 1000000000L
        val datos = listOf(Consumo(limites, "Ejemplo", ahora), Consumo(limites, "Ejemplo", ahora - 86400001), Consumo(limites, "Ejemplo", ahora - 1000))
        assertEquals(listOf(ahora - 1000, ahora), filtrarHistorial(datos, PeriodoHistorial.DIA, ahora).map { it.fecha })
        assertEquals(3, filtrarHistorial(datos, PeriodoHistorial.TODO, ahora).size)
    }
    @Test fun historialVacioNoInventaDatos() { assertTrue(filtrarHistorial(emptyList(), PeriodoHistorial.SEMANA, 100).isEmpty()) }
}
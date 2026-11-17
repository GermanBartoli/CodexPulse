package com.germanbartoli.codexpulse

import org.junit.Assert.*
import org.junit.Test

class ConsumoTest {
    @Test fun interpretaLimitesYRedondeaDisponible() {
        val valor = Consumo.leer("""{"plan_type":"plus","rate_limit":{"primary_window":{"used_percent":27.3,"reset_at":2000000000,"limit_window_seconds":18000},"secondary_window":{"used_percent":58,"limit_window_seconds":604800}}}""")
        assertEquals(73, valor.limites[0].disponible)
        assertEquals("Semanal", valor.limites[1].nombre)
        assertEquals(42, valor.limites[1].disponible)
    }
    @Test fun noInventaUnLimiteSemanal() {
        val valor = Consumo.leer("""{"rate_limit":{"primary_window":{"used_percent":0},"secondary_window":null}}""")
        assertEquals(1, valor.limites.size)
        assertNull(valor.limites[0].reinicio)
        assertEquals("Período informado", valor.limites[0].nombre)
    }
    @Test fun unaCuotaAgotadaNoTieneSaldoNegativo() {
        assertEquals(0, Limite(120.0, null, null).disponible)
    }
    @Test(expected = Exception::class) fun rechazaRespuestaSinPorcentaje() {
        Consumo.leer("""{"rate_limit":{"primary_window":{}}}""")
    }
    @Test(expected = Exception::class) fun rechazaRespuestaSinLimites() { Consumo.leer("""{"rate_limit":{}}""") }
    @Test(expected = Exception::class) fun rechazaPorcentajeNegativo() { Consumo.leer("""{"rate_limit":{"primary_window":{"used_percent":-2}}}""") }
    @Test fun noDaPorRenovadaUnaCuotaSinConsultar() {
        assertEquals("Renovación pendiente de consultar", Limite(98.0, 1000, 18000).renovacion(2000))
    }
    @Test fun conservaFechasAlGuardarHistorial() {
        val original = Consumo(listOf(Limite(12.5, 2000000000, 18000), Limite(50.0, null, 604800)), "plus", 1800000000000)
        assertEquals(original, Consumo.leer(original.json()))
    }
}

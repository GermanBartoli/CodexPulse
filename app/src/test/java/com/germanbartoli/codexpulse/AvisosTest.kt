package com.germanbartoli.codexpulse

import org.junit.Assert.*
import org.junit.Test

class AvisosTest {
    @Test fun silencioRespetaMedianocheYExtremos() {
        assertTrue(dentroDeSilencio(23 * 60, 1320, 480))
        assertTrue(dentroDeSilencio(0, 1320, 480))
        assertFalse(dentroDeSilencio(480, 1320, 480))
        assertTrue(dentroDeSilencio(600, 600, 720))
        assertFalse(dentroDeSilencio(400, 600, 720))
    }
    @Test fun recuperacionRequiereTodosLosLimitesEsperados() {
        val espera = setOf("604800")
        val semanal = Limite(99.0, null, 604800)
        val corto = Limite(0.0, null, 18000)
        assertTrue(cuotaRecuperada(espera, listOf(semanal, corto)))
        assertFalse(cuotaRecuperada(espera, listOf(corto)))
        assertFalse(cuotaRecuperada(espera, listOf(semanal, corto.copy(usado = 100.0))))
        assertFalse(cuotaRecuperada(emptySet(), listOf(semanal)))
    }
    @Test fun plantillaConLimiteAusente() {
        val consumo = Consumo(listOf(Limite(35.0, null, 604800)), "Personal")
        assertEquals("Personal · 65% · —", textoConsumo("{plan} · {semanal} · {5h}", consumo))
    }
    @Test fun cuotaAltaNoAvisa() { assertNull(umbralPendiente(21, null)) }
    @Test fun primerUmbralIncluyeVeinte() { assertEquals(20, umbralPendiente(20, null)) }
    @Test fun saltoDirectoAlCriticoSoloAvisaDiez() { assertEquals(10, umbralPendiente(3, null)) }
    @Test fun repeticionesNoNotifican() {
        assertNull(umbralPendiente(18, 20))
        assertNull(umbralPendiente(0, 10))
    }
    @Test fun descensoAlSegundoUmbralAvisa() { assertEquals(10, umbralPendiente(10, 20)) }
    @Test fun recuperacionParcialNoRepiteAvisos() { assertNull(umbralPendiente(15, 10)) }
    @Test fun periodoNuevoPuedeAvisar() { assertEquals(20, umbralPendiente(19, null)) }
}
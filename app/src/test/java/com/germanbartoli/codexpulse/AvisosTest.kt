package com.germanbartoli.codexpulse

import org.junit.Assert.*
import org.junit.Test

class AvisosTest {
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
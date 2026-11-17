package com.germanbartoli.codexpulse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable fun EditorEstiloWidgets(estilo: EstiloWidgets, mostrarCuota: Boolean = true, cambiar: (EstiloWidgets) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PaletaWidget.entries.forEach { paleta ->
            FilterChip(selected = estilo.paleta == paleta, onClick = { cambiar(estilo.copy(paleta = paleta)) }, label = { Text(paleta.titulo) })
        }
    }
    Text("Fondo del widget", style = MaterialTheme.typography.labelLarge)
    FondoWidget.entries.forEach { fondo ->
        Row(Modifier.fillMaxWidth().selectable(selected = estilo.fondo == fondo, role = Role.RadioButton, onClick = { cambiar(estilo.copy(fondo = fondo)) }), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = estilo.fondo == fondo, onClick = null)
            Text(fondo.titulo)
        }
    }
    if (estilo.fondo == FondoWidget.TRANSPARENTE) Text("Si el texto se pierde sobre tu fondo de pantalla, elegí Adaptable u Oscuro.", style = MaterialTheme.typography.bodySmall)
    if (mostrarCuota) {
    Text("Cuota del indicador 1×1", style = MaterialTheme.typography.labelLarge)
    CuotaIndicador.entries.forEach { cuota ->
        Row(Modifier.fillMaxWidth().selectable(selected = estilo.cuota == cuota, role = Role.RadioButton, onClick = { cambiar(estilo.copy(cuota = cuota)) }), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = estilo.cuota == cuota, onClick = null)
            Text(cuota.titulo)
        }
    }
    Text("Si tu cuenta no informa la cuota elegida, el indicador muestra «No informado».", style = MaterialTheme.typography.bodySmall)
    }

}
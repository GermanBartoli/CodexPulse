package com.germanbartoli.codexpulse

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import android.appwidget.AppWidgetManager
import android.graphics.Bitmap
import androidx.glance.appwidget.compose
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.*
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import java.io.File

@RunWith(AndroidJUnit4::class)
class AplicacionTest {
    @get:Rule val interfaz = createAndroidComposeRule<PantallaPrincipal>()
    @Test fun demostracionWidgetsYBorradoLocal() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        Assume.assumeFalse("No se ejecutan pruebas destructivas con una sesión real", runBlocking { Repositorio.leer(contexto).conectado })
        interfaz.onNodeWithText("Explorar la demostración").performScrollTo().performClick()
        interfaz.waitUntil(10000) { interfaz.onAllNodesWithText("73%").fetchSemanticsNodes().isNotEmpty() }
        interfaz.onNodeWithText("73%").performScrollTo().assertIsDisplayed()
        interfaz.onNodeWithText("42%").assertExists()
        val proveedores = AppWidgetManager.getInstance(contexto).installedProviders.filter { it.provider.packageName == contexto.packageName }
        Assert.assertEquals(5, proveedores.size)
        val captura = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(contexto.getExternalFilesDir(null), "demostracion.png").outputStream().use { captura.compress(Bitmap.CompressFormat.PNG, 100, it) }
        captura.recycle()
        val variantes = listOf(
            Triple(MinimoGlance(), 80, 80), Triple(SemanalGlance(), 170, 180),
            Triple(CompactoGlance(), 170, 180), Triple(CompletoGlance(), 350, 180),
            Triple(HistorialGlance(), 350, 270)
        )
        variantes.forEachIndexed { indice, (widget, ancho, alto) ->
            val remoto = runBlocking { widget.compose(contexto, size = DpSize(ancho.dp, alto.dp)) }
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val densidad = contexto.resources.displayMetrics.density
                val contenedor = android.widget.FrameLayout(contexto)
                val vista = remoto.apply(contexto, contenedor)
                contenedor.addView(vista)
                val w = (ancho * densidad).toInt()
                val h = (alto * densidad).toInt()
                contenedor.measure(android.view.View.MeasureSpec.makeMeasureSpec(w, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(h, android.view.View.MeasureSpec.EXACTLY))
                contenedor.layout(0, 0, w, h)
                val imagen = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                contenedor.draw(android.graphics.Canvas(imagen))
                File(contexto.getExternalFilesDir(null), "widget-$indice.png").outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) }
                imagen.recycle()
                Assert.assertTrue(vista.width > 0)
            }
        }
        val bytes = File(contexto.noBackupFilesDir, "pulse.cifrado").readBytes()
        Assert.assertFalse(String(bytes, Charsets.ISO_8859_1).contains("demostracion"))
        Assert.assertTrue(runBlocking { Repositorio.leer(contexto).demostracion })
        interfaz.onNodeWithText("Borrar sesión y datos de este teléfono").performScrollTo().performClick()
        interfaz.onNodeWithText("Borrar", useUnmergedTree = true).performClick()
        interfaz.waitUntil(10000) { interfaz.onAllNodesWithText("Explorar la demostración").fetchSemanticsNodes().isNotEmpty() }
        Assert.assertFalse(File(contexto.noBackupFilesDir,"pulse.cifrado").exists())
        Assert.assertNull(runBlocking { Repositorio.leer(contexto).consumo })
    }
}

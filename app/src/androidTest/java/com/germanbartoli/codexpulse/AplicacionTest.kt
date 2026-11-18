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
import androidx.glance.appwidget.provideContent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidBitmap
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
        val captura = interfaz.onRoot().captureToImage().asAndroidBitmap()
        File(contexto.getExternalFilesDir(null), "demostracion.png").outputStream().use { captura.compress(Bitmap.CompressFormat.PNG, 100, it) }
        captura.recycle()
        val variantes = listOf(
            Triple(MinimoGlance(), 80, 80), Triple(SemanalGlance(), 170, 180),
            Triple(CompactoGlance(), 170, 180), Triple(CompletoGlance(), 350, 180),
            Triple(HistorialGlance(), 350, 270)
        )
        fun capturarWidget(widget: androidx.glance.appwidget.GlanceAppWidget, ancho: Int, alto: Int, nombre: String) {
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
                File(contexto.getExternalFilesDir(null), "widget-$nombre.png").outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) }
                imagen.recycle()
                Assert.assertTrue(vista.width > 0)
            }
        }
        variantes.forEachIndexed { indice, (widget, ancho, alto) -> capturarWidget(widget, ancho, alto, indice.toString()) }
        interfaz.onNodeWithText("Historial", substring = false).performClick()
        interfaz.onNodeWithText("24 horas").performScrollTo().performClick()
        interfaz.onNodeWithText("Todo").performClick()
        interfaz.onNodeWithText("0% disponible", substring = false).performScrollTo()
        val historial = interfaz.onRoot().captureToImage().asAndroidBitmap()
        File(contexto.getExternalFilesDir(null), "historial.png").outputStream().use { historial.compress(Bitmap.CompressFormat.PNG, 100, it) }
        historial.recycle()
        interfaz.onNodeWithText("Ajustes", substring = false).performClick()
        interfaz.onNodeWithText("Agregar este widget").assertExists()
        interfaz.waitUntil(10000) { interfaz.onAllNodesWithContentDescription("Vista previa de Dos límites 2×2").fetchSemanticsNodes().isNotEmpty() }
        interfaz.onNodeWithContentDescription("Vista previa de Dos límites 2×2").performScrollTo().performTouchInput { swipeLeft() }
        interfaz.onNodeWithText("Horizontal 4×2").assertExists()
        interfaz.waitUntil(10000) { interfaz.onAllNodesWithContentDescription("Vista previa de Horizontal 4×2").fetchSemanticsNodes().isNotEmpty() }
        val previa = interfaz.onRoot().captureToImage().asAndroidBitmap()
        File(contexto.getExternalFilesDir(null), "vista-previa.png").outputStream().use { previa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        previa.recycle()
        interfaz.onNodeWithText("Verde Pulse").performScrollTo().performClick()
        interfaz.onNodeWithText("Oscuro").performScrollTo().performClick()
        interfaz.onNodeWithText("Límite de 5 horas").performScrollTo().performClick()
        Assert.assertEquals(EstiloWidgets(PaletaWidget.PULSE, FondoWidget.OSCURO, CuotaIndicador.CINCO_HORAS), Personalizacion.leer(contexto))
        capturarWidget(MinimoGlance(), 80, 80, "pulse-minimo")
        capturarWidget(HistorialGlance(), 350, 270, "pulse-historial")
        val almacen = AlmacenSeguro(contexto)
        val original = almacen.leer()
        val unaCuota = Consumo(listOf(Limite(56.0, System.currentTimeMillis() / 1000 + 84420, 604800)), "Demostración")
        almacen.guardar(org.json.JSONObject().put("demostracion", true).put("consumo", unaCuota.json()))
        capturarWidget(CompactoGlance(), 170, 180, "una-cuota-compacto")
        capturarWidget(CompletoGlance(), 350, 180, "una-cuota-horizontal")
        capturarWidget(SemanalGlance(), 170, 180, "una-cuota-semanal")
        almacen.guardar(original)
        interfaz.onNodeWithText("Transparente").performScrollTo().performClick()
        Assert.assertEquals(FondoWidget.TRANSPARENTE, Personalizacion.leer(contexto).fondo)
        capturarWidget(CompactoGlance(), 170, 180, "transparente")
        interfaz.onNodeWithText("Adaptable").performScrollTo().performClick()
        interfaz.onNodeWithText("Del teléfono").performScrollTo().performClick()
        interfaz.onNodeWithText("Menor disponible").performScrollTo().performClick()
        Assert.assertEquals(12, runBlocking { Repositorio.leer(contexto).historial.size })
        Assert.assertFalse(Preferencias.alertas(contexto))
        interfaz.onNodeWithText("15 min").performScrollTo().performClick()
        Assert.assertEquals(15, Preferencias.minutos(contexto))
        interfaz.onNodeWithText("60 min").performClick()
        Assert.assertEquals(60, Preferencias.minutos(contexto))
        interfaz.onNodeWithText("30 min").performClick()
        Assert.assertEquals(30, Preferencias.minutos(contexto))
        val ajustes = interfaz.onRoot().captureToImage().asAndroidBitmap()
        File(contexto.getExternalFilesDir(null), "ajustes.png").outputStream().use { ajustes.compress(Bitmap.CompressFormat.PNG, 100, it) }
        ajustes.recycle()
        val automatizacion = InstrumentationRegistry.getInstrumentation().uiAutomation
        automatizacion.grantRuntimePermission(contexto.packageName, android.Manifest.permission.POST_NOTIFICATIONS)
        val preferencias = Preferencias.datos(contexto)
        preferencias.edit().putBoolean("fija", true).putString("titulo_fijo", "Mi {plan}")
            .putString("texto_fijo", "Semanal {semanal}").apply()
        val gestor = contexto.getSystemService(android.app.NotificationManager::class.java)
        val muestra = Estado(consumo = Consumo.ejemplo(), conectado = true)
        ConsumoVisible.publicar(contexto, muestra)
        interfaz.waitUntil(5000) { gestor.activeNotifications.any { it.id == ConsumoVisible.ID } }
        val fija = gestor.activeNotifications.first { it.id == ConsumoVisible.ID }.notification
        Assert.assertEquals(android.app.Notification.VISIBILITY_PUBLIC, fija.visibility)
        Assert.assertTrue(fija.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0)
        Assert.assertEquals("Mi Demostración", fija.extras.getCharSequence(android.app.Notification.EXTRA_TITLE))
        Assert.assertEquals("Semanal 42%", fija.extras.getCharSequence(android.app.Notification.EXTRA_TEXT))
        preferencias.edit().putBoolean("fija", false).remove("titulo_fijo").remove("texto_fijo").apply()
        ConsumoVisible.publicar(contexto, muestra)
        interfaz.waitUntil(5000) { gestor.activeNotifications.none { it.id == ConsumoVisible.ID } }
        Preferencias.alertas(contexto, true)
        runBlocking { Repositorio.actualizar(contexto) }
        val notificaciones = contexto.getSystemService(android.app.NotificationManager::class.java)
        Assert.assertTrue("La demostración no debe emitir alertas", notificaciones.activeNotifications.isEmpty())
        Assert.assertTrue(Avisos.mostrar(contexto, listOf(Limite(90.0, null, 18000))))
        interfaz.waitUntil(5000) { notificaciones.activeNotifications.size == 1 }
        val notificacion = notificaciones.activeNotifications.single().notification
        Assert.assertEquals(android.app.Notification.VISIBILITY_PRIVATE, notificacion.visibility)
        Assert.assertFalse(notificacion.publicVersion.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString().contains("%"))
        Preferencias.alertas(contexto, false)
        interfaz.waitUntil(5000) { notificaciones.activeNotifications.isEmpty() }
        suspend fun identificadorPrueba(): androidx.glance.GlanceId {
            var capturado: androidx.glance.GlanceId? = null
            val widget = object : androidx.glance.appwidget.GlanceAppWidget() {
                override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
                    capturado = id
                    provideContent { androidx.glance.text.Text("Prueba aislada") }
                }
            }
            widget.compose(contexto, size = DpSize(80.dp, 80.dp))
            return requireNotNull(capturado)
        }
        val primero = runBlocking { identificadorPrueba() }
        val segundo = runBlocking { identificadorPrueba() }
        Assert.assertNotEquals(primero, segundo)
        val estiloPrimero = EstiloWidgets(PaletaWidget.PULSE, FondoWidget.OSCURO, CuotaIndicador.SEMANAL)
        val estiloSegundo = EstiloWidgets(PaletaWidget.TELEFONO, FondoWidget.TRANSPARENTE, CuotaIndicador.MENOR)
        runBlocking {
            Personalizacion.guardarPropio(contexto, primero, estiloPrimero)
            Assert.assertNull(Personalizacion.leerPropio(contexto, segundo))
            Personalizacion.guardarPropio(contexto, segundo, estiloSegundo)
            Assert.assertEquals(estiloPrimero, Personalizacion.leerPropio(contexto, primero))
            Assert.assertEquals(estiloSegundo, Personalizacion.leerPropio(contexto, segundo))
            Personalizacion.guardarPropio(contexto, primero, null)
            Assert.assertNull(Personalizacion.leerPropio(contexto, primero))
            Assert.assertEquals(estiloSegundo, Personalizacion.leerPropio(contexto, segundo))
            Personalizacion.guardarPropio(contexto, segundo, null)
        }
        val bytes = File(contexto.noBackupFilesDir, "pulse.cifrado").readBytes()
        Assert.assertFalse(String(bytes, Charsets.ISO_8859_1).contains("demostracion"))
        Assert.assertTrue(runBlocking { Repositorio.leer(contexto).demostracion })
        interfaz.onNodeWithText("Borrar sesión y datos de este teléfono").performScrollTo().performClick()
        interfaz.onNodeWithText("Borrar", useUnmergedTree = true).performClick()
        interfaz.onNodeWithText("Inicio", substring = false).performClick()
        interfaz.waitUntil(10000) { interfaz.onAllNodesWithText("Explorar la demostración").fetchSemanticsNodes().isNotEmpty() }
        Assert.assertFalse(File(contexto.noBackupFilesDir,"pulse.cifrado").exists())
        Assert.assertNull(runBlocking { Repositorio.leer(contexto).consumo })
    }
}

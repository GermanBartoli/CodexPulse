# Guía de uso

Pulso de Germán permite consultar la cuota disponible de Codex y mostrarla en widgets de Android. Es una app comunitaria en desarrollo, no oficial ni afiliada a OpenAI.

## Instalar y actualizar

1. Usá un teléfono con Android 12 o posterior; Android 16 está incluido.
2. Abrí [Releases](https://github.com/GermanBartoli/CodexPulse/releases) y elegí una versión que incluya un archivo `.apk`. Las publicaciones de documentación no incluyen instalador.
3. Descargá y abrí la APK. Si Android lo solicita, autorizá la instalación desde la aplicación que abrió el archivo. No hace falta desactivar Play Protect.
4. Para actualizar, instalá la nueva APK sobre la anterior, sin desinstalar. Cada publicación con instalador incluye `SHA256SUMS.txt` para comprobar la descarga.

## Probar o conectar tu cuenta

Elegí **Explorar la demostración** para conocer la interfaz con datos de ejemplo. No requiere cuenta y sus porcentajes no representan tu consumo.

Para consultar tus datos, tocá **Conectar con ChatGPT**, elegí un navegador y seguí el flujo de código de dispositivo en la página oficial que se abre. Ingresá tus credenciales solamente en la página oficial. No compartas el código. La app no recibe tu contraseña.

Si la conexión falla, revisá el mensaje que muestra la app. El acceso por código de dispositivo puede requerir habilitación en la configuración de tu cuenta. No publiques códigos ni archivos de sesión para pedir ayuda.

## Agregar y configurar widgets

Deslizá hacia los lados para cambiar entre Inicio, Historial y Ajustes. En **Ajustes**, deslizá sobre la vista previa para elegir el diseño y tocá **Agregar este widget**. Confirmá la incorporación cuando lo solicite el lanzador del teléfono.

| Widget | Qué muestra |
| --- | --- |
| Indicador 1×1 | Un porcentaje; permite elegir menor disponibilidad, cinco horas o semanal. |
| Semanal 2×2 | La cuota semanal, si el servicio la informa. |
| Dos límites 2×2 | Una vista compacta de los límites recibidos. |
| Horizontal 4×2 | Porcentajes, barras y próximas renovaciones. |
| Historial 4×3 | Cuotas y evolución local de la cuota primaria. |

Los tamaños y las opciones de colocación dependen del lanzador. Las cuotas que el servicio no informa no se reemplazan por datos inventados.

En **Inicio → Tus widgets agregados**, revisá las instancias instaladas y tocá **Configurar** para personalizar una. También podés encontrarlas en Ajustes. Guardar modifica esa instancia; Cancelar conserva su estilo anterior. Los widgets sin configuración propia usan el estilo por defecto.

## Actualizaciones, historial y alertas

Podés actualizar manualmente o elegir consultas automáticas cada 15, 30 o 60 minutos. Android puede demorar las consultas en segundo plano por sus reglas de ahorro de batería. Revisá siempre la hora del último dato.

El historial guarda las últimas 96 consultas locales y permite filtrar por 24 horas, 7 días o todas las guardadas. No recupera el consumo anterior a la instalación ni convierte porcentajes en tokens exactos.

Las alertas de disponibilidad al 20% y 10% son opcionales y requieren permiso de notificaciones. Están desactivadas inicialmente y no se generan en la demostración.

## Borrar los datos del teléfono

La opción de borrar datos elimina la sesión y el historial locales y cancela las actualizaciones. No cierra otras sesiones de ChatGPT. Consultá las [preguntas frecuentes](PREGUNTAS_FRECUENTES.md) y la [política de seguridad](../SECURITY.md) para más información.

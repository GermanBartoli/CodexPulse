# Cómo colaborar

Podés ayudar a Pulso de Germán probando los widgets, informando errores, proponiendo mejoras o enviando cambios de código.

## Informar un error o proponer una mejora

Revisá primero los [issues existentes](https://github.com/GermanBartoli/CodexPulse/issues). Si no encontrás el tema, abrí uno con una descripción concreta.

Para un error, indicá la versión de la app, la versión de Android, el modelo del teléfono y los pasos para reproducirlo. Si afecta a un widget, agregá el nombre del lanzador y el tipo de widget. Explicá qué esperabas y qué ocurrió. Las capturas son opcionales: revisá que no incluyan datos personales ni de acceso.

Para una propuesta, contá qué problema resolvería y cómo te gustaría usarla. No hace falta saber programar.

Las vulnerabilidades se reportan siguiendo la [política de seguridad](SECURITY.md), sin publicar detalles sensibles.

## Preparar un cambio de código

1. Creá un fork y una rama para un cambio concreto.
2. Consultá los requisitos y comandos de [Desarrollo](README.md#desarrollo). Usá el Gradle Wrapper incluido y JDK 21.
3. Mantené el cambio enfocado y explicá las nuevas dependencias o cambios de comportamiento.
4. Ejecutá las verificaciones relacionadas antes de abrir el pull request.
5. Describí el problema, la solución y las pruebas realizadas. Para cambios visuales, adjuntá capturas con datos de demostración.

Verificaciones generales desde PowerShell:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Las pruebas de interfaz y widgets están documentadas en el README. Usá un emulador o dispositivo de pruebas sin una sesión real; la demostración permite revisar la interfaz sin conectar una cuenta.

Nunca incluyas archivos de sesión, claves de firma ni credenciales en un cambio. Respetá los finales de línea definidos en `.gitattributes` y conservá los avisos de terceros.

## Revisión y convivencia

Los cambios se revisan antes de incorporarse; abrir una propuesta no implica su aceptación ni un plazo de respuesta. Escribí las explicaciones y documentación en español y seguí el [código de conducta](CODE_OF_CONDUCT.md).

El código del proyecto se distribuye bajo [MIT](LICENSE.md). Conservá las atribuciones y licencias que correspondan a cualquier material que aportes.

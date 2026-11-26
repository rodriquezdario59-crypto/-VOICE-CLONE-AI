# VOICE CLONE AI

Aplicación Android nativa, con interfaz oscura y prioridad de funcionamiento sin conexión. Diseñada inicialmente para ARM64 y Android 12 o superior.

## Versión 0.3.0 — Biblioteca de voces, Pocket TTS y transferencia ZIP

- Interfaz con tema azul noche y acentos azules.
- Biblioteca local de referencias WAV/MP3 con nombre, reproducción previa, selección y eliminación.
- Exportación e importación de la biblioteca mediante un ZIP para trasladarla a otro teléfono.
- Vinculación de perfiles con las voces que expone el motor Android TTS.
- La aplicación intenta utilizar directamente Pocket TTS Android Engine (`org.pockettts.android.engine`) cuando está instalado.
- Conserva la importación y grabación WAV del estudio principal.
- Texto de generación limitado a 3.000 caracteres.
- Compilación APK automatizada con GitHub Actions.

## Importante sobre la clonación

Pocket TTS Android Engine es un motor separado que expone su servicio mediante Android TextToSpeech. Para generar con una referencia personalizada, esa misma grabación debe importarse también dentro de Pocket TTS y guardarse allí. Después, vuelve a VOICE CLONE AI, actualiza las voces disponibles y vincula el perfil con el identificador de voz que expone el motor.

La copia de referencia que guarda VOICE CLONE AI es una biblioteca organizativa local; Android no permite que una aplicación lea directamente los archivos privados de otra. Por eso, esta versión no copia silenciosamente una referencia al espacio privado de Pocket TTS ni afirma clonar una voz por el simple hecho de importar un WAV.

Los modelos de idioma de Pocket TTS se distribuyen por separado y no están incluidos en esta APK. Para funcionamiento completamente local se necesita instalar Pocket TTS, importar el paquete de modelo español y registrar las referencias autorizadas. No se utiliza ElevenLabs ni un servidor externo.

## Uso previsto

1. Instalar Pocket TTS Android Engine y su paquete de modelo español.
2. Importar allí la referencia WAV de la persona que autorizó su uso y guardar la voz.
3. Abrir VOICE CLONE AI e importar el mismo audio en la Biblioteca de voces.
4. Pulsar **Vincular** y elegir el identificador correspondiente que ofrece Pocket TTS.
5. Pulsar **Usar** y generar texto desde el estudio.

## Compilación

- JDK 17
- Android Gradle Plugin 8.6.1
- Kotlin 2.0.20
- Android SDK 35
- `gradle assembleDebug`

El workflow de GitHub Actions genera `app-debug.apk` como artefacto descargable.

## Privacidad y consentimiento

Las referencias se guardan en el almacenamiento privado de VOICE CLONE AI. Utiliza solamente grabaciones propias o muestras para las que tengas autorización explícita y legal. No presentes audio sintético como si fuera una grabación auténtica de otra persona.

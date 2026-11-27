# CLON AI PRO

Aplicación Android nativa en desarrollo, con prioridad de funcionamiento local y una interfaz oscura. El objetivo de compatibilidad es Android 12 o superior; la compilación y el rendimiento deben verificarse en el dispositivo real.

## Estado actual del proyecto

La versión del repositorio conserva estas funciones:

- Biblioteca local de referencias de voz WAV/MP3 con nombre, vista previa, selección y eliminación.
- Grabación de audio desde el micrófono e importación desde el teléfono.
- Exportación e importación de la biblioteca mediante ZIP.
- Texto de hasta 3.000 caracteres.
- Controles separados de velocidad y tono.
- Generación y exportación WAV mediante Android TextToSpeech, cuando el motor instalado lo admite.
- Integración opcional con Pocket TTS Android Engine como servicio TTS externo a esta aplicación.

## Limitación importante: clonación de voz

Guardar o importar un WAV en la biblioteca **no clona por sí solo una voz**. En la versión actual, la referencia debe registrarse también dentro de Pocket TTS y luego vincularse con la voz que ese motor expone a Android TextToSpeech. La aplicación no puede leer directamente el almacenamiento privado de otra aplicación.

Esta versión tampoco incluye todavía una integración nativa de Qwen3-TTS. La integración Qwen está en evaluación técnica y no debe considerarse disponible hasta que el motor, el procesamiento de audio de referencia y la generación se integren en este proyecto y pasen pruebas en un teléfono ARM64 real.

## Próxima integración evaluada: Qwen3-TTS

Se está evaluando el proyecto comunitario Qwen3 TTS Android y su runtime nativo qwen3-tts.cpp. La documentación del proyecto describe inferencia local por CPU, arquitectura arm64-v8a, Android 12 o superior, y modelos cuantizados Q4_K_M que pueden ocupar aproximadamente 1,2 GB en conjunto. Su clase nativa expone métodos para procesar WAV de referencia y extraer embeddings, pero su propio plan documenta que aún no tiene una interfaz de clonación completa y que falta validación en dispositivos reales.

Por tanto, Qwen es un candidato técnico prometedor, no una promesa de rendimiento o calidad en este teléfono. Antes de adoptarlo se deben comprobar compatibilidad, memoria disponible, temperatura, tiempo de generación, calidad del español y condiciones de licencia de todos los componentes y pesos del modelo.

Consulta [ENGINE_EVALUATION.md](ENGINE_EVALUATION.md) para el análisis y los criterios de aceptación.

## Compilación

La compilación automatizada de CLON AI PRO construye el código de este repositorio, no una aplicación ajena renombrada. El workflow de GitHub Actions publica una APK de depuración como artefacto temporal.

## Privacidad y consentimiento

Utiliza únicamente grabaciones propias o muestras para las que tengas autorización expresa y legal. No presentes audio sintético como si fuera una grabación auténtica de otra persona.

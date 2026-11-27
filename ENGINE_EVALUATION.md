# Evaluación técnica del motor para CLON AI PRO

Fecha de revisión: 2026-11-27

## Candidato principal: Qwen3-TTS Android + qwen3-tts.cpp

Repositorio comunitario revisado: https://github.com/Danmoreng/qwen3-tts-android

### Lo que está confirmado en el código/documentación consultados

- Proyecto Android nativo escrito en Kotlin.
- Runtime nativo C++ expuesto a Kotlin mediante JNI.
- Objetivo nativo arm64-v8a y Android 12 / API 31 o superior.
- Ruta de inferencia por CPU.
- Paquete Q4_K_M documentado con los archivos qwen-tokenizer-12hz-Q4_K_M.gguf y qwen-talker-0.6b-base-Q4_K_M.gguf.
- La documentación estima aproximadamente 1,2 GB para el paquete de modelo.
- La API Kotlin QwenEngine expone synthesize(text, referenceWav, speakerEmbeddingPath, params), extractSpeakerEmbedding(referenceWav, outputPath), control de hilos CPU y capacidades del modelo.
- La licencia del repositorio de la aplicación comunitaria se declara MIT.

### Lo que todavía NO está demostrado

- Que compile e integre sin cambios dentro de nuestro proyecto actual.
- Que el modelo y todas sus dependencias permitan la redistribución que queremos. Hay que revisar por separado la licencia del runtime, submódulos, pesos y conversiones cuantizadas.
- Que el Samsung Galaxy S10+ concreto pueda cargar el modelo sin quedarse sin memoria, calentarse en exceso o tardar demasiado.
- Que la clonación de una voz española desde un WAV funcione con la calidad requerida.
- Que los controles de velocidad y tono puedan aplicarse de forma independiente en el camino nativo.
- Que la aplicación tenga un flujo completo de perfiles de voz clonados con importación, guardado, selección y generación. El proyecto comunitario reconoce en su plan que su interfaz de clonación aún no está implementada como función completa.

## Plan de integración por fases

1. **Congelar una compilación base:** compilar el código actual de CLON AI PRO y conservar el artefacto como referencia.
2. **Validar el runtime por separado:** construir el proyecto Qwen comunitario sin cambiarle el nombre y revisar el resultado, dependencias y licencias.
3. **Prueba real en el teléfono:** comprobar descarga/carga del modelo, generación corta en español, memoria, temperatura y duración.
4. **Prueba de referencia autorizada:** integrar un WAV de prueba con consentimiento, extraer embedding y comparar el timbre en varias frases españolas.
5. **Adaptador nativo:** incorporar JNI/C++ al proyecto CLON AI PRO sin perder biblioteca, importación, grabación ni controles actuales.
6. **Perfiles de voz:** cada perfil debe guardar referencia, embedding, nombre, consentimiento registrado y metadatos del motor; poder previsualizarse, seleccionarse, exportarse e importarse.
7. **Estudio de audio:** progreso, cancelar, reproducir, guardar WAV, conversión MP3, historial y eliminación de resultados.
8. **Pruebas de regresión:** verificar cada función en Android 12 y revisar fallos, cierres y recuperación después de reiniciar.
9. **APK candidata:** publicar solo después de que la compilación y las pruebas de dispositivo estén verificadas.

## Criterios mínimos de aceptación

- No depender de ElevenLabs, créditos, suscripciones ni un servidor remoto para generar.
- No afirmar clonación cuando solo se esté usando TTS genérico.
- No perder referencias o perfiles al cerrar y volver a abrir la app.
- No bloquear la interfaz durante la inferencia.
- Informar con claridad si faltan modelos, memoria o compatibilidad.
- Usar solo voces para las que exista autorización.

## Decisión provisional

Qwen3-TTS es el candidato que merece la primera prueba de integración porque su API nativa contempla audio de referencia y embeddings, y tiene una ruta CPU para ARM64. La adopción queda condicionada a la prueba real en el teléfono y a la revisión completa de licencias. Pocket TTS se conserva como alternativa de respaldo mientras se valida el nuevo motor.

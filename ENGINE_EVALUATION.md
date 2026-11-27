# Evaluación técnica del motor para CLON AI PRO

Fecha de revisión: 2026-11-27

## Motor elegido para la primera integración: Qwen3-TTS Base 12Hz

La decisión ya no es probar motores al azar: el motor objetivo será **Qwen3-TTS Base 12Hz**, empezando por la variante **0.6B cuantizada Q4_K_M** para evaluar su viabilidad en el Galaxy S10+. La variante 1.7B se conserva como referencia de calidad para equipos con más memoria, no como objetivo inicial del teléfono.

La razón principal es que el modelo oficial ofrece clonación desde audio de referencia, soporte explícito de español y generación condicionada por texto. La documentación oficial muestra que el modo de clonación de mayor fidelidad utiliza tanto `ref_audio` como `ref_text` (transcripción exacta del audio de referencia). El modelo también publica métricas de consistencia del texto (WER) y similitud de hablante para español. Esas cifras son resultados de laboratorio del fabricante, no una garantía para esta APK.

**Requisito obligatorio para corregir el problema que describes:** no basta con extraer un embedding de la voz. CLON AI PRO debe implementar el flujo de clonación con referencia completa (audio + transcripción exacta, cuando el backend lo permita), y la generación debe verificar que el texto se procese íntegro. La interfaz Android comunitaria que hemos revisado documenta extracción de embedding, pero no demuestra por sí sola que ya exponga todo el modo ICL con `ref_text`; esa parte debe desarrollarse y probarse antes de adoptarla.

## Base Android evaluada: Qwen3-TTS Android + qwen3-tts.cpp

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
- Que el port Android comunitario implemente el modo ICL completo con transcripción `ref_text`; su README solo confirma perfiles y extracción de embeddings. No debemos confundir esto con clonación ICL completa.
- Que los resultados de WER/SIM publicados por Qwen se reproduzcan con el runtime Android cuantizado Q4_K_M.
- Que los controles de velocidad y tono puedan aplicarse de forma independiente en el camino nativo.
- Que la aplicación tenga un flujo completo de perfiles de voz clonados con importación, guardado, selección y generación. El proyecto comunitario reconoce en su plan que su interfaz de clonación aún no está implementada como función completa.

## Plan de integración por fases

1. **Congelar una compilación base:** compilar el código actual de CLON AI PRO y conservar el artefacto como referencia.
2. **Validar el runtime por separado:** construir el proyecto Qwen comunitario sin cambiarle el nombre y revisar el resultado, dependencias y licencias.
3. **Prueba real en el teléfono:** comprobar descarga/carga del modelo, generación corta en español, memoria, temperatura y duración.
4. **Prueba de referencia autorizada:** registrar un audio autorizado y su transcripción literal; implementar referencia ICL (audio + texto) cuando sea posible, comparar con modo embedding-only y verificar que no se pierdan palabras en frases largas en español.
5. **Adaptador nativo:** incorporar JNI/C++ al proyecto CLON AI PRO sin perder biblioteca, importación, grabación ni controles actuales.
6. **Perfiles de voz:** cada perfil debe guardar referencia, embedding, nombre, consentimiento registrado y metadatos del motor; poder previsualizarse, seleccionarse, exportarse e importarse.
7. **Estudio de audio:** progreso, cancelar, reproducir, guardar WAV, conversión MP3, historial y eliminación de resultados.
8. **Pruebas de regresión:** verificar cada función en Android 12 y revisar fallos, cierres y recuperación después de reiniciar.
9. **APK candidata:** publicar solo después de que la compilación y las pruebas de dispositivo estén verificadas.

## Criterios mínimos de aceptación

- No depender de ElevenLabs, créditos, suscripciones ni un servidor remoto para generar.
- No afirmar clonación cuando solo se esté usando TTS genérico.
- No aceptar como motor final un backend que solo copie el timbre por embedding si eso provoca pérdida de contenido; priorizar el modo ICL con texto de referencia y pruebas de integridad textual.
- Probar frases de control con conteo de palabras y transcripción automática del audio generado; bloquear la publicación si faltan palabras o se repiten fragmentos.
- No perder referencias o perfiles al cerrar y volver a abrir la app.
- No bloquear la interfaz durante la inferencia.
- Informar con claridad si faltan modelos, memoria o compatibilidad.
- Usar solo voces para las que exista autorización.

## Decisión de producto

El motor objetivo es **Qwen3-TTS Base 12Hz**. Para el Galaxy S10+ se inicia la evaluación con **0.6B Q4_K_M** por sus requisitos más moderados. La calidad de referencia es la variante 1.7B, pero no se dará por viable en el teléfono sin medir memoria, velocidad y temperatura.

La integración se considerará terminada solo cuando CLON AI PRO genere con voz autorizada y demuestre lectura íntegra en español. Se exigirá conservar el texto completo, sin palabras omitidas, repeticiones ni fragmentos saltados. Si la versión Q4_K_M Android no supera esas pruebas, no se publicará como reemplazo definitivo; se ajustará el backend o se buscará otra ruta compatible. Pocket TTS deja de ser el motor objetivo y se mantiene únicamente como referencia de comparación durante la migración.

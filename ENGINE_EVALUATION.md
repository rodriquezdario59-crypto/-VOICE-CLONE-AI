# Evaluación técnica del motor para CLON AI PRO

Fecha de revisión: 2026-11-27

## Decisión de integración: MOSS-TTS-Nano ONNX CPU

El motor objetivo pasa a ser **MOSS-TTS-Nano (100M) con ONNX Runtime CPU**. Es el candidato que mejor equilibra tamaño, español, clonación por audio de referencia y ejecución local en CPU.

La documentación oficial del proyecto declara 20 idiomas, incluido español; generación de voz por referencia; salida de audio estéreo a 48 kHz; y una ruta ONNX CPU sin dependencia de PyTorch durante la inferencia. El equipo informa mejoras de eficiencia de casi 2x en sus propias pruebas y uso fluido con un solo núcleo en un MacBook Air M4. Esos resultados son del fabricante y NO garantizan el mismo rendimiento en un Samsung Galaxy S10+.

Fuentes oficiales:
- https://github.com/OpenMOSS/MOSS-TTS-Nano
- https://huggingface.co/OpenMOSS-Team/MOSS-TTS-Nano-100M-ONNX
- https://huggingface.co/OpenMOSS-Team/MOSS-Audio-Tokenizer-Nano-ONNX

## Por qué se descarta Qwen3-TTS como motor inicial

La variante Qwen3-TTS 0.6B Q4_K_M que se probó en el teléfono tardó varios minutos sin completar una frase breve. No se seguirá usando como motor predeterminado de CLON AI PRO. El rendimiento observado en ese teléfono no satisface el requisito básico de respuesta.

## Hechos confirmados y límites de la integración Android

### Confirmado en el proyecto oficial MOSS
- Modelo MOSS-TTS-Nano de aproximadamente 100 millones de parámetros.
- Soporte declarado para español y otros 19 idiomas.
- ONNX Runtime CPU para inferencia sin PyTorch.
- El flujo ONNX oficial admite audio de referencia directo para clonación.
- El flujo completo requiere tanto los modelos TTS como el MOSS-Audio-Tokenizer-Nano.
- La inferencia oficial utiliza SentencePiece para convertir texto en IDs y el encoder del audio tokenizer para convertir la referencia de voz en códigos de audio.
- El repositorio fuente MOSS-TTS-Nano declara licencia Apache-2.0. La licencia de cada conjunto de pesos y de cada dependencia debe verificarse antes de redistribuirlos.

### Limitación crítica del ejemplo Android oficial
El ejemplo Android publicado por OpenMOSS es una prueba mínima: genera WAV desde prompts pre-tokenizados y voces de muestra ya incluidas en el manifiesto. No implementa por sí mismo texto arbitrario, tokenizer SentencePiece ni codificación de una grabación propia para clonación.

Por tanto, **copiar el ejemplo Android sin ampliarlo NO entrega clonación real**. La integración de CLON AI PRO debe portar el flujo completo de ONNX documentado por el proyecto oficial: tokenizer de texto, preprocesamiento de referencia, encoder de audio, generación autoregresiva y decoder de audio.

## Diseño de integración Android

1. **Conservar la aplicación existente:** biblioteca local, grabación, importación WAV/MP3, nombres de perfiles, selección, previsualización, exportación ZIP, historial y controles de velocidad/tono.
2. **Añadir ONNX Runtime Android:** utilizar CPU como backend inicial, con hilos configurables y generación fuera del hilo principal.
3. **Descarga de modelos bajo demanda:** guardar los pesos en almacenamiento privado de la app, mostrar progreso real, validar tamaño/hash cuando estén publicados y permitir reanudar o repetir descargas. No incluir cientos de MB de pesos dentro del APK.
4. **Tokenizer de texto:** integrar una implementación Android compatible con el archivo tokenizer.model y verificar que los IDs coincidan con SentencePiece oficial.
5. **Audio de referencia:** decodificar WAV/MP3/M4A, convertir a PCM float, remuestrear al formato requerido por el codec y normalizar canales sin alterar el timbre.
6. **Clonación real:** ejecutar el encoder del MOSS-Audio-Tokenizer-Nano sobre el audio importado y pasar sus códigos de referencia al modelo TTS. No marcar una voz como clonada si solo se guardó el archivo.
7. **Generación larga:** dividir el texto por límites de tokens y puntuación, mantener el mismo audio de referencia entre fragmentos, concatenar con pausas naturales y conservar el orden íntegro del texto.
8. **Estudio de audio:** progreso, cancelación, reproducción, exportación WAV y MP3, historial y eliminación.
9. **Perfiles persistentes:** guardar nombre, referencia, consentimiento confirmado, parámetros y versión del motor. Las voces deben seguir disponibles al reiniciar la aplicación.
10. **Pruebas de dispositivo:** medir primera carga, memoria, temperatura, tiempo hasta el primer audio, velocidad real de generación y fidelidad de voz en el Samsung Galaxy S10+ con Android 12.

## Motor complementario

**Kokoro-82M / LiteRT** puede evaluarse como sintetizador rápido de voces predefinidas en español (por ejemplo, ef_dora, em_alex y em_santa). No es un clonador de voz y nunca debe presentarse como tal. Solo se añadirá como alternativa de TTS normal si pasa pruebas reales en el dispositivo.

## Criterios obligatorios antes de publicar una APK candidata

- La generación debe ejecutarse localmente y sin servicios remotos obligatorios.
- Debe poder sintetizar texto arbitrario en español, no solo prompts de demostración.
- Debe aceptar una grabación de referencia autorizada y codificarla con el audio tokenizer.
- La salida debe conservar todas las palabras, sin omisiones ni repeticiones.
- El perfil debe sonar reconociblemente similar a su referencia en pruebas de escucha; la similitud no se debe afirmar sin evaluación.
- La interfaz no debe congelarse y debe permitir cancelar una generación.
- El motor debe informar claramente si faltan modelos, espacio o memoria.
- No publicar como clonación funcional una compilación que solo use voces predefinidas o TTS de Android.

## Estado

- Motor elegido: MOSS-TTS-Nano ONNX CPU.
- Qwen3-TTS: descartado como motor inicial por el resultado observado en el teléfono.
- Kokoro: candidato secundario solo para TTS normal.
- Integración Android completa: pendiente de implementación y pruebas.
- APK de producción: NO aprobada hasta superar todos los criterios anteriores.

La prioridad es una aplicación que funcione de verdad. No se sustituirá la clonación real por una pantalla que solo guarde audios o cambie el nombre de una voz genérica.

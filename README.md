# CLON AI PRO

Aplicación Android nativa en desarrollo, con prioridad de funcionamiento local y una interfaz oscura. El objetivo de compatibilidad es Android 12 o superior; la compilación y el rendimiento deben verificarse en el dispositivo real.

## Funciones existentes

- Biblioteca local de referencias WAV/MP3 con nombre, vista previa, selección y eliminación.
- Grabación de audio desde el micrófono e importación desde el teléfono.
- Exportación e importación de la biblioteca mediante ZIP.
- Texto de hasta 3.000 caracteres.
- Controles separados de velocidad y tono.
- Generación y exportación WAV mediante Android TextToSpeech cuando el motor instalado lo admite.
- Integración opcional con Pocket TTS Android Engine.

**Importante:** en esta versión, importar y guardar un audio no clona por sí solo una voz. Pocket TTS requiere registrar la referencia en su propio motor y vincular la voz que expone al sistema Android. Esta limitación se eliminará únicamente cuando el nuevo backend de clonación esté integrado y probado.

## Motor objetivo: MOSS-TTS-Nano ONNX CPU

El motor seleccionado para la siguiente integración es MOSS-TTS-Nano de 100M parámetros, con ONNX Runtime CPU. Su documentación oficial declara soporte para español, clonación mediante audio de referencia y una ruta ONNX sin PyTorch durante la inferencia.

La integración Android publicada por OpenMOSS es solo una demostración mínima basada en prompts pre-tokenizados. Para CLON AI PRO hay que completar el tokenizer SentencePiece, la codificación de audio de referencia mediante MOSS-Audio-Tokenizer-Nano, la generación de texto arbitrario y el decoder de audio. No se afirmará que la clonación funciona hasta completar y probar ese flujo.

## Diseño del producto

- Procesamiento local, sin suscripción ni servidor obligatorio.
- Biblioteca de varias voces con nombres independientes.
- Grabación e importación de referencias autorizadas.
- Texto de hasta 3.000 caracteres, segmentado de forma segura.
- Controles separados de velocidad y tono cuando el backend lo permita.
- Previsualización, historial, cancelación y exportación WAV/MP3.
- Modelos descargados bajo demanda con progreso y almacenamiento privado, para evitar un APK excesivamente grande.

Kokoro-82M/LiteRT se mantiene como candidato secundario para TTS rápido con voces predefinidas en español; no realiza clonación de voces y no se presentará como tal.

## Compilación

El workflow de GitHub Actions construye el código de este repositorio. Una APK de depuración publicada como artefacto temporal no equivale a una versión de producción ni demuestra que el motor de clonación esté integrado.

## Privacidad y consentimiento

Utiliza únicamente grabaciones propias o muestras para las que tengas autorización expresa y legal. No presentes audio sintético como si fuera una grabación auténtica de otra persona.

Consulta [ENGINE_EVALUATION.md](ENGINE_EVALUATION.md) para la decisión y los criterios técnicos de aceptación.

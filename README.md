# VOICE CLONE AI

Aplicación Android nativa, diseñada con prioridad offline para el Samsung Galaxy S10+ (SM-G975F, Exynos 9820, Android 12).

## Estado de desarrollo

### Fase 1 — Base Android
- Proyecto nativo Kotlin.
- Interfaz inicial.
- Importación de muestras de audio al almacenamiento privado.
- Grabación de muestras desde el micrófono.
- Vista previa y exportación WAV mediante el motor TTS instalado en Android.
- Flujo de compilación APK con GitHub Actions.

### Fase 2 — Motor neuronal de clonación local (pendiente)
- Selección e integración de un modelo de clonación compatible con ARM64/Android.
- Inferencia completamente local, sin ElevenLabs ni servidores externos.
- Biblioteca persistente de perfiles de voz.
- Conversión de texto a voz usando la voz clonada.
- Pruebas de memoria, temperatura, latencia y calidad en el SM-G975F.

> Importante: la base inicial utiliza el TTS del sistema para la vista previa. Eso no es clonación de voz. La clonación neuronal no se considerará terminada hasta integrar un modelo local real y verificarlo en el dispositivo.

## Compilación
El workflow Build Android APK compila app-debug.apk y lo publica como artefacto de GitHub Actions.

## Requisitos
- JDK 17.
- Android SDK 35.
- Android 8.0 (API 26) o superior.

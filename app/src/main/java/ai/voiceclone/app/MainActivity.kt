package ai.voiceclone.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.media.MediaRecorder
import android.media.AudioRecord
import android.media.AudioFormat
import java.io.FileInputStream
import java.io.FileOutputStream
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private lateinit var textInput: EditText
    private lateinit var samplesSummary: TextView
    private var recorder: AudioRecord? = null
    @Volatile private var recordingActive = false
    private var recordingThread: Thread? = null
    private var recordingPcmFile: File? = null
    private var recordingFile: File? = null
    private var selectedSampleFile: File? = null
    private var sampleCount = 0
    private val requestRecordAudio = 410
    private val requestImportAudio = 411
    private val requestSaveWav = 412
    private val requestExportSample = 413
    private var pendingWavUri: Uri? = null
    private var pendingWavTemp: File? = null
    private val maxCharacters = 3000
    private val pocketEnginePackage = "org.pockettts.android.engine"
    private var usingPocketEngine = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val engineInstalled = try {
            packageManager.getApplicationInfo(pocketEnginePackage, 0)
            true
        } catch (_: Exception) { false }
        usingPocketEngine = engineInstalled
        tts = if (engineInstalled) TextToSpeech(this, this, pocketEnginePackage) else TextToSpeech(this, this)
        buildScreen()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFF101014.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        content.addView(label("VOICE CLONE AI", 28, 0xFFFFFFFF.toInt()))
        content.addView(label("Motor local • Sin servicios externos", 14, 0xFFBDB8C8.toInt()))
        content.addView(space(18))
        status = label("Preparando motor de voz del dispositivo…", 14, 0xFFBDB8C8.toInt())
        content.addView(status)
        content.addView(space(18))
        content.addView(label("TEXTO PARA CONVERTIR EN VOZ", 13, 0xFFBDB8C8.toInt()))
        textInput = EditText(this).apply {
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF88838F.toInt())
            setHint("Escribe aquí hasta 3.000 caracteres")
            setBackgroundColor(0xFF24232A.toInt())
            setPadding(14, 14, 14, 14)
            minLines = 5
            maxLines = 9
            gravity = Gravity.TOP
            filters = arrayOf(android.text.InputFilter.LengthFilter(maxCharacters))
        }
        content.addView(textInput, matchWidth())
        content.addView(space(8))
        content.addView(label("Pocket TTS funciona como motor local cuando está instalado.", 12, 0xFFAAA5B1.toInt()))
        content.addView(space(16))
        content.addView(actionButton("Abrir biblioteca de voces") {
            startActivity(Intent(this, VoiceLibraryActivity::class.java))
        })
        content.addView(actionButton("Importar muestra de voz") { importAudio() })
        content.addView(actionButton("Grabar muestra con micrófono") { startRecording() })
        content.addView(actionButton("Detener grabación") { stopRecording() })
        content.addView(actionButton("Exportar última muestra grabada") { exportLatestSample() })
        content.addView(actionButton("Leer texto con voz local del teléfono") { speakText() })
        content.addView(actionButton("Guardar audio WAV") { saveWav() })
        content.addView(actionButton("Detener reproducción") { tts.stop() })
        content.addView(space(18))
        content.addView(label("MUESTRAS IMPORTADAS", 13, 0xFFBDB8C8.toInt()))
        samplesSummary = label("Muestras guardadas localmente: 0", 12, 0xFFAAA5B1.toInt())
        content.addView(samplesSummary)
        content.addView(label("Carpeta de grabaciones: almacenamiento interno de la aplicación / voice_samples", 12, 0xFFAAA5B1.toInt()))
        content.addView(label("Pulsa «Importar muestra de voz» para elegir una grabación guardada aquí o buscar un audio en el teléfono. La selección no clona por sí sola una voz.", 12, 0xFFAAA5B1.toInt()))
        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
    }

    private fun label(value: String, size: Int, color: Int) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
    }
    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, height) }
    private fun matchWidth() = LinearLayout.LayoutParams(-1, -2)
    private fun actionButton(title: String, action: () -> Unit) = Button(this).apply {
        text = title
        isAllCaps = false
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 }
    }

    override fun onInit(result: Int) {
        if (result == TextToSpeech.SUCCESS) {
            val languageResult = tts.setLanguage(Locale("es", "ES"))
            val savedVoiceId = getSharedPreferences("voiceclone", MODE_PRIVATE)
                .getString("selected_tts_voice", null)
            val savedVoice = tts.voices?.firstOrNull { it.name == savedVoiceId }
            if (savedVoice != null) runCatching { tts.voice = savedVoice }
            status.text = if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                "Instala una voz española compatible para uso sin conexión en los ajustes del sistema."
            } else if (usingPocketEngine) {
                "Pocket TTS conectado. Usa la biblioteca para elegir una voz clonada."
            } else {
                "Motor local del teléfono listo. Instala Pocket TTS para usar voces clonadas."
            }
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) { runOnUiThread { status.text = "Generando audio local…" } }
                override fun onDone(utteranceId: String) {
                    if (utteranceId == "voiceclone-wav") {
                        val uri = pendingWavUri
                        val temp = pendingWavTemp
                        try {
                            if (uri == null || temp == null || !temp.exists() || temp.length() == 0L) {
                                throw IllegalStateException("El motor no produjo un archivo WAV.")
                            }
                            contentResolver.openOutputStream(uri, "w")!!.use { output ->
                                temp.inputStream().use { input -> input.copyTo(output) }
                            }
                            runOnUiThread { status.text = "Audio WAV guardado correctamente."; toast("WAV guardado correctamente.") }
                        } catch (e: Exception) {
                            runOnUiThread { status.text = "No se pudo guardar el WAV."; toast("Error al guardar el WAV: " + e.message) }
                        } finally {
                            temp?.delete()
                            pendingWavUri = null
                            pendingWavTemp = null
                        }
                    } else {
                        runOnUiThread { status.text = "Audio terminado." }
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String) {
                    if (utteranceId == "voiceclone-wav") {
                        pendingWavTemp?.delete()
                        pendingWavTemp = null
                        pendingWavUri = null
                    }
                    runOnUiThread { status.text = "No se pudo generar el audio."; toast("No se pudo generar el audio.") }
                }
            })
        } else status.text = "No se pudo iniciar el motor TTS del teléfono."
    }

    private fun speakText() {
        val text = textInput.text.toString().trim()
        if (text.isEmpty()) return toast("Escribe primero el texto.")
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voiceclone-preview")
    }

    private fun saveWav() {
        val text = textInput.text.toString().trim()
        if (text.isEmpty()) return toast("Escribe primero el texto.")
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/wav"
            putExtra(Intent.EXTRA_TITLE, "voiceclone_${System.currentTimeMillis()}.wav")
        }
        startActivityForResult(intent, requestSaveWav)
    }


    private var pendingSampleExport: File? = null

    private fun exportLatestSample() {
        val dir = File(filesDir, "voice_samples")
        val sample = selectedSampleFile?.takeIf { it.exists() && it.extension.lowercase() == "wav" }
            ?: dir.listFiles()
                ?.filter { it.isFile && it.extension.lowercase() == "m4a" }
                ?.maxByOrNull { it.lastModified() }
        if (sample == null || !sample.exists()) return toast("No se encontró una grabación WAV.")
        openSampleSavePicker(sample)
    }

    private fun openSampleSavePicker(sample: File) {
        pendingSampleExport = sample
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/wav"
            putExtra(Intent.EXTRA_TITLE, sample.nameWithoutExtension + ".wav")
        }
        try {
            startActivityForResult(intent, requestExportSample)
        } catch (e: Exception) {
            pendingSampleExport = null
            toast("No se pudo abrir la selección de carpeta: " + e.message)
        }
    }

    private fun importAudio() {
        val dir = File(filesDir, "voice_samples").apply { mkdirs() }
        val savedSamples = dir.listFiles()
            ?.filter { it.isFile && it.length() > 0L && it.extension.lowercase() in listOf("m4a", "wav", "mp3", "audio", "aac", "ogg") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        val options = mutableListOf<String>()
        savedSamples.forEach { file ->
            val kind = if (file.name.startsWith("recording_")) "Grabación" else "Audio importado"
            options.add("$kind • ${file.name}")
        }
        options.add("Buscar un audio en las carpetas del teléfono")

        AlertDialog.Builder(this)
            .setTitle("Seleccionar muestra de voz")
            .setItems(options.toTypedArray()) { _, which ->
                if (which < savedSamples.size) {
                    selectedSampleFile = savedSamples[which]
                    status.text = "Muestra seleccionada: ${savedSamples[which].name}. Ubicación: carpeta interna voice_samples."
                    toast("Muestra seleccionada correctamente.")
                } else {
                    openAudioPicker()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openAudioPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try {
            startActivityForResult(intent, requestImportAudio)
            status.text = "Elige un archivo de audio. Después de seleccionarlo, espera la confirmación."
        } catch (e: Exception) {
            status.text = "No se pudo abrir el selector de audios."
            toast("No se pudo abrir el selector: ${e.message}")
        }
    }

    private fun refreshSampleSummary() {
        val dir = File(filesDir, "voice_samples")
        val imported = dir.listFiles()?.count {
            it.isFile && it.length() > 0L && !it.name.startsWith("recording_") &&
                it.extension.lowercase() in listOf("audio", "m4a", "wav", "mp3", "aac", "ogg")
        } ?: 0
        if (::samplesSummary.isInitialized) {
            samplesSummary.text = "Muestras guardadas localmente: $imported"
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestSaveWav && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val text = textInput.text.toString().trim()
            if (text.isEmpty()) return toast("Escribe primero el texto.")
            try {
                val temp = File(cacheDir, "voiceclone_${System.currentTimeMillis()}.wav")
                pendingWavUri = uri
                pendingWavTemp = temp
                status.text = "Generando WAV… espera sin cerrar la aplicación."
                val result = tts.synthesizeToFile(text, Bundle(), temp, "voiceclone-wav")
                if (result != TextToSpeech.SUCCESS) {
                    temp.delete()
                    pendingWavUri = null
                    pendingWavTemp = null
                    status.text = "El motor no pudo iniciar la creación del WAV."
                    toast("El motor de voz no pudo crear el WAV.")
                }
            } catch (e: Exception) {
                pendingWavTemp?.delete()
                pendingWavUri = null
                pendingWavTemp = null
                toast("No se pudo preparar el WAV: " + e.message)
            }
        }

        if (requestCode == requestExportSample && resultCode == RESULT_OK) {
            val uri = data?.data
            val sample = pendingSampleExport
            try {
                if (uri == null || sample == null || !sample.exists()) {
                    throw IllegalStateException("No se encontró la grabación.")
                }
                contentResolver.openOutputStream(uri, "w")!!.use { output ->
                    sample.inputStream().use { input -> input.copyTo(output) }
                }
                status.text = "Muestra WAV exportada a la carpeta que elegiste."
                toast("Muestra exportada correctamente. Revisa la carpeta que seleccionaste, normalmente Descargas.")
            } catch (e: Exception) {
                toast("No se pudo exportar la muestra: " + e.message)
            } finally {
                pendingSampleExport = null
            }
        }
        if (requestCode == requestImportAudio) {
            if (resultCode != RESULT_OK) {
                status.text = "No se seleccionó ningún audio. Puedes volver a pulsar Importar muestra de voz."
                return
            }
            val uri = data?.data
            if (uri == null) {
                status.text = "El selector no entregó el archivo. Intenta seleccionarlo otra vez."
                toast("No se recibió el archivo de audio.")
                return
            }
            var destination: File? = null
            try {
                val dir = File(filesDir, "voice_samples").apply { mkdirs() }
                destination = File(dir, "sample_${System.currentTimeMillis()}.audio")
                val input = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Android no pudo abrir el archivo seleccionado.")
                input.use { source ->
                    destination.outputStream().use { output -> source.copyTo(output) }
                }
                if (!destination.exists() || destination.length() == 0L) {
                    destination.delete()
                    throw IllegalStateException("El archivo llegó vacío.")
                }
                selectedSampleFile = destination
                sampleCount++
                refreshSampleSummary()
                status.text = "¡Muestra importada correctamente! Archivo guardado en voice_samples."
                toast("Muestra importada correctamente.")
            } catch (e: Exception) {
                destination?.delete()
                status.text = "No se pudo importar la muestra: ${e.message}"
                toast("Error al importar: ${e.message}")
            }
        }
    }

    private fun startRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), requestRecordAudio)
            return
        }
        try {
            val dir = File(filesDir, "voice_samples").apply { mkdirs() }
            val stamp = System.currentTimeMillis()
            recordingFile = File(dir, "recording_${stamp}.wav")
            recordingPcmFile = File(cacheDir, "recording_${stamp}.pcm")
            val rate = 16000
            val minBuffer = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minBuffer <= 0) throw IllegalStateException("No se pudo preparar el micrófono.")
            val audio = AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuffer, 8192))
            if (audio.state != AudioRecord.STATE_INITIALIZED) {
                audio.release()
                throw IllegalStateException("El micrófono no pudo inicializarse.")
            }
            recorder = audio
            audio.startRecording()
            recordingActive = true
            val pcm = recordingPcmFile!!
            recordingThread = Thread {
                val buffer = ByteArray(maxOf(minBuffer, 4096))
                FileOutputStream(pcm).use { output ->
                    while (recordingActive) {
                        val n = audio.read(buffer, 0, buffer.size)
                        if (n > 0) output.write(buffer, 0, n)
                    }
                }
            }.also { it.start() }
            status.text = "Grabando voz en WAV (16 kHz)…"
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            toast("No se pudo iniciar la grabación: ${e.message}")
        }
    }

    private fun writePcmAsWav(pcmFile: File, wavFile: File, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val dataSize = pcmFile.length().toInt()
        val byteRate = sampleRate * channels * bitsPerSample / 8
        FileOutputStream(wavFile).use { output ->
            fun writeInt(v: Int) {
                output.write(v and 255); output.write((v shr 8) and 255)
                output.write((v shr 16) and 255); output.write((v shr 24) and 255)
            }
            fun writeShort(v: Int) { output.write(v and 255); output.write((v shr 8) and 255) }
            output.write("RIFF".toByteArray(Charsets.US_ASCII)); writeInt(36 + dataSize)
            output.write("WAVE".toByteArray(Charsets.US_ASCII)); output.write("fmt ".toByteArray(Charsets.US_ASCII))
            writeInt(16); writeShort(1); writeShort(channels); writeInt(sampleRate)
            writeInt(byteRate); writeShort(channels * bitsPerSample / 8); writeShort(bitsPerSample)
            output.write("data".toByteArray(Charsets.US_ASCII)); writeInt(dataSize)
            FileInputStream(pcmFile).use { it.copyTo(output) }
        }
    }

    private fun stopRecording() {
        val active = recorder ?: return toast("No hay grabación activa.")
        var completedSample: File? = null
        try {
            recordingActive = false
            active.stop()
            recordingThread?.join(2000)
            active.release()
            recorder = null
            val pcm = recordingPcmFile ?: throw IllegalStateException("No se encontró el audio temporal.")
            val wav = recordingFile ?: throw IllegalStateException("No se encontró el archivo WAV.")
            writePcmAsWav(pcm, wav, 16000, 1, 16)
            pcm.delete()
            recordingPcmFile = null
            if (wav.length() <= 44L) throw IllegalStateException("La grabación quedó vacía.")
            sampleCount++
            selectedSampleFile = wav
            completedSample = wav
            status.text = "Grabación WAV guardada internamente. Elige dónde guardar una copia."
            toast("Grabación WAV terminada correctamente.")
        } catch (e: Exception) {
            recordingActive = false
            runCatching { active.stop() }
            active.release()
            recorder = null
            recordingPcmFile?.delete()
            recordingPcmFile = null
            toast("No se pudo finalizar la grabación WAV: ${e.message}")
        }
        completedSample?.let { sample ->
            AlertDialog.Builder(this)
                .setTitle("Guardar grabación")
                .setMessage("La grabación ya está guardada dentro de la aplicación. ¿En qué carpeta quieres guardar una copia?")
                .setPositiveButton("Elegir carpeta") { _, _ -> openSampleSavePicker(sample) }
                .setNegativeButton("Ahora no", null)
                .show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestRecordAudio && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startRecording()
        else if (requestCode == requestRecordAudio) toast("Se necesita permiso de micrófono.")
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        recorder?.runCatching { stop() }
        recorder?.release()
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}

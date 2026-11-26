package ai.voiceclone.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
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
import org.json.JSONArray
import org.json.JSONObject

class VoiceLibraryActivity : Activity() {
    private data class Profile(
        val id: String,
        var name: String,
        val audioPath: String,
        var engineVoiceId: String = ""
    )

    private val pocketEnginePackage = "org.pockettts.android.engine"
    private val pickerCode = 721
    private val profiles = mutableListOf<Profile>()
    private var engineVoices: List<Voice> = emptyList()
    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null
    private lateinit var list: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadProfiles()
        buildUi()
        val installed = try {
            packageManager.getApplicationInfo(pocketEnginePackage, 0)
            true
        } catch (_: Exception) { false }
        tts = if (installed) {
            TextToSpeech(this, { result -> onEngineReady(result) }, pocketEnginePackage)
        } else {
            TextToSpeech(this) { result -> onEngineReady(result) }
        }
        renderProfiles()
    }

    private fun onEngineReady(result: Int) {
        if (result == TextToSpeech.SUCCESS) {
            engineVoices = tts?.voices?.toList()?.sortedBy { it.name } ?: emptyList()
            status.text = if (tts?.defaultEngine == pocketEnginePackage) {
                "Pocket TTS conectado • ${engineVoices.size} voces disponibles"
            } else {
                "Pocket TTS no está instalado • se muestra el motor disponible del teléfono"
            }
        } else {
            status.text = "No se pudo iniciar el motor de voz."
        }
        renderProfiles()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 24, 22, 22)
            setBackgroundColor(0xFF0B1220.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(text("VOICE CLONE AI", 13, 0xFF79C5FF.toInt(), true))
        content.addView(text("Biblioteca de voces", 27, 0xFFF4F7FC.toInt(), true))
        content.addView(text("Guarda, organiza y selecciona tus referencias de voz.", 14, 0xFF9DAEC2.toInt()))
        content.addView(space(14))
        status = text("Comprobando Pocket TTS…", 13, 0xFF9FCFFF.toInt())
        content.addView(status)
        content.addView(space(12))
        content.addView(action("＋  Importar audio WAV / MP3", true) { openPicker() })
        content.addView(action("Actualizar voces disponibles", false) { refreshEngineVoices() })
        content.addView(space(14))
        content.addView(text("MIS VOCES", 12, 0xFF91A6BE.toInt(), true))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list)
        content.addView(space(16))
        content.addView(text("Importante: guarda únicamente grabaciones que tengas autorización para utilizar. Para clonar una referencia, importa también ese WAV en Pocket TTS y luego vincula aquí la voz que el motor exponga.", 12, 0xFF8FA1B6.toInt()))
        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, 5, 0, 5)
    }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, height)
    }

    private fun action(title: String, primary: Boolean, click: () -> Unit) = Button(this).apply {
        text = title
        isAllCaps = false
        textSize = 14f
        setTextColor(if (primary) 0xFFFFFFFF.toInt() else 0xFFBBDFFF.toInt())
        setBackgroundColor(if (primary) 0xFF1769A6.toInt() else 0xFF1A2A3D.toInt())
        setOnClickListener { click() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 }
    }

    private fun openPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(intent, pickerCode)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickerCode || resultCode != RESULT_OK) return
        val uri = data?.data ?: return toast("No se recibió el archivo.")
        try {
            val dir = File(filesDir, "voice_library").apply { mkdirs() }
            val file = File(dir, "reference_${System.currentTimeMillis()}.${extensionFor(uri)}")
            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("No se pudo leer el audio.")
            if (file.length() == 0L) throw IllegalStateException("El archivo está vacío.")
            confirmAndName(file)
        } catch (e: Exception) {
            toast("No se pudo importar el audio: ${e.message}")
        }
    }

    private fun extensionFor(uri: Uri): String {
        val mime = contentResolver.getType(uri)?.lowercase().orEmpty()
        return when {
            mime.contains("wav") -> "wav"
            mime.contains("mpeg") || mime.contains("mp3") -> "mp3"
            mime.contains("mp4") || mime.contains("m4a") -> "m4a"
            mime.contains("ogg") -> "ogg"
            else -> "audio"
        }
    }

    private fun confirmAndName(file: File) {
        val nameInput = EditText(this).apply {
            hint = "Nombre de la voz"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF8FA1B6.toInt())
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
            addView(nameInput)
            addView(text("Confirma que tienes permiso para utilizar esta grabación.", 12, 0xFF9DAEC2.toInt()))
        }
        AlertDialog.Builder(this)
            .setTitle("Guardar nueva voz")
            .setView(panel)
            .setPositiveButton("Guardar") { _, _ ->
                val name = nameInput.text.toString().trim().ifBlank { "Voz ${profiles.size + 1}" }
                profiles.add(Profile(System.currentTimeMillis().toString(), name, file.absolutePath))
                saveProfiles()
                renderProfiles()
                toast("Referencia guardada. Ahora vincúlala con la voz correspondiente de Pocket TTS.")
            }
            .setNegativeButton("Cancelar") { _, _ -> file.delete() }
            .show()
    }

    private fun refreshEngineVoices() {
        engineVoices = tts?.voices?.toList()?.sortedBy { it.name } ?: emptyList()
        status.text = "Voces detectadas: ${engineVoices.size}. Si acabas de importar una referencia en Pocket TTS, vuelve a abrir esta biblioteca."
        renderProfiles()
    }

    private fun linkVoice(profile: Profile) {
        refreshEngineVoices()
        if (engineVoices.isEmpty()) {
            toast("No hay voces del motor disponibles. Comprueba que Pocket TTS esté instalado y tenga un modelo cargado.")
            return
        }
        val labels = engineVoices.map { voice ->
            "${voice.name}  •  ${voice.locale.displayName}"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Vincular voz para ${profile.name}")
            .setItems(labels) { _, which ->
                profile.engineVoiceId = engineVoices[which].name
                saveProfiles()
                renderProfiles()
                toast("Voz vinculada correctamente.")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun selectVoice(profile: Profile) {
        if (profile.engineVoiceId.isBlank()) {
            toast("Primero vincula esta referencia con una voz disponible del motor.")
            return
        }
        getSharedPreferences("voiceclone", MODE_PRIVATE).edit()
            .putString("selected_tts_voice", profile.engineVoiceId)
            .putString("selected_profile_name", profile.name)
            .apply()
        toast("${profile.name} seleccionada. Vuelve al estudio para generar audio.")
        finish()
    }

    private fun preview(profile: Profile) {
        val file = File(profile.audioPath)
        if (!file.exists()) return toast("No se encuentra el audio de referencia.")
        try {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { it.release(); player = null }
                prepare()
                start()
            }
        } catch (e: Exception) {
            toast("No se pudo reproducir la muestra: ${e.message}")
        }
    }

    private fun delete(profile: Profile) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar ${profile.name}")
            .setMessage("Se eliminará esta referencia guardada en VOICE CLONE AI. La copia que tengas en Pocket TTS no se eliminará.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                File(profile.audioPath).delete()
                profiles.removeAll { it.id == profile.id }
                saveProfiles()
                renderProfiles()
            }
            .show()
    }

    private fun renderProfiles() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        if (profiles.isEmpty()) {
            list.addView(text("Todavía no has guardado voces. Importa una grabación para comenzar.", 13, 0xFF9DAEC2.toInt()))
            return
        }
        profiles.forEach { profile ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14, 12, 14, 12)
                setBackgroundColor(0xFF17263A.toInt())
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 }
            }
            card.addView(text(profile.name, 17, 0xFFF1F6FC.toInt(), true))
            val linked = profile.engineVoiceId.isNotBlank()
            card.addView(text(if (linked) "Motor: ${profile.engineVoiceId}" else "Pendiente de vincular con Pocket TTS", 11, if (linked) 0xFF78D6B0.toInt() else 0xFFF0C27A.toInt()))
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(action("Escuchar", false) { preview(profile) }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(action("Vincular", false) { linkVoice(profile) }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(action("Usar", true) { selectVoice(profile) }, LinearLayout.LayoutParams(0, -2, 1f))
            card.addView(row)
            card.addView(action("Eliminar referencia", false) { delete(profile) })
            list.addView(card)
        }
    }

    private fun loadProfiles() {
        profiles.clear()
        val raw = getSharedPreferences("voice_library", MODE_PRIVATE).getString("profiles", "[]") ?: "[]"
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val path = item.optString("audioPath")
                if (File(path).exists()) profiles.add(Profile(
                    item.optString("id"), item.optString("name"), path, item.optString("engineVoiceId")
                ))
            }
        }
    }

    private fun saveProfiles() {
        val array = JSONArray()
        profiles.forEach { profile ->
            array.put(JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("audioPath", profile.audioPath)
                put("engineVoiceId", profile.engineVoiceId)
            })
        }
        getSharedPreferences("voice_library", MODE_PRIVATE).edit().putString("profiles", array.toString()).apply()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        player?.release()
        player = null
        tts?.shutdown()
        super.onDestroy()
    }
}

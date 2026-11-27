package ai.voiceclone.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.drawable.GradientDrawable
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
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
    private var usingPocketEngine = false
    private val pickerCode = 721
    private val exportCode = 722
    private val importCode = 723
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
        usingPocketEngine = installed
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
            status.text = if (usingPocketEngine) {
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
        val brandIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_voice_clone)
            layoutParams = LinearLayout.LayoutParams(76, 76).apply { gravity = Gravity.CENTER_HORIZONTAL }
        }
        content.addView(brandIcon)
        content.addView(text("VOICE CLONE AI", 13, 0xFF79C5FF.toInt(), true))
        content.addView(text("Biblioteca de voces", 27, 0xFFF4F7FC.toInt(), true))
        content.addView(text("Guarda, organiza y selecciona tus referencias de voz.", 14, 0xFF9DAEC2.toInt()))
        content.addView(space(14))
        status = text("Comprobando Pocket TTS…", 13, 0xFF9FCFFF.toInt())
        content.addView(status)
        content.addView(space(12))
        content.addView(action("＋  Importar audio WAV / MP3", true) { openPicker() })
        content.addView(action("Abrir Pocket TTS para registrar la referencia", false) { openPocketTts() })
        content.addView(action("Actualizar voces disponibles", false) { refreshEngineVoices() })
        content.addView(action("Exportar biblioteca para otro celular", false) { openExportPicker() })
        content.addView(action("Importar biblioteca desde ZIP", false) { openImportPicker() })
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
        background = GradientDrawable().apply {
            setColor(if (primary) 0xFF1769A6.toInt() else 0xFF1A2A3D.toInt())
            cornerRadius = 18f
        }
        minHeight = 48
        setPadding(10, 8, 10, 8)
        setOnClickListener { click() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 }
    }

    private fun openPocketTts() {
        val launch = packageManager.getLaunchIntentForPackage(pocketEnginePackage)
        if (launch == null) {
            toast("No se encontró Pocket TTS. Instálalo primero para usar clonación local.")
        } else {
            startActivity(launch)
        }
    }

    override fun onResume() {
        super.onResume()
        if (tts != null) refreshEngineVoices()
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
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return toast("No se recibió el archivo.")
        if (requestCode == exportCode) { exportLibrary(uri); return }
        if (requestCode == importCode) { importLibrary(uri); return }
        if (requestCode != pickerCode) return
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
        val labels = engineVoices.mapIndexed { index, voice ->
            "${index + 1}. ${profile.name}  →  ${voice.name}  •  ${voice.locale.displayName}"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Selecciona la voz del motor para: ${profile.name}")
            .setItems(labels) { _, which ->
                val chosen = engineVoices[which]
                val alreadyUsedBy = profiles.firstOrNull { it.id != profile.id && it.engineVoiceId == chosen.name }
                if (alreadyUsedBy != null) {
                    AlertDialog.Builder(this)
                        .setTitle("Identificador ya utilizado")
                        .setMessage("Pocket TTS ofrece el mismo identificador para «${alreadyUsedBy.name}». No se asignará a «${profile.name}» porque ambas referencias terminarían usando la misma voz del motor. Registra una voz independiente en Pocket TTS y actualiza la lista.")
                        .setPositiveButton("Entendido", null)
                        .show()
                    return@setItems
                }
                profile.engineVoiceId = chosen.name
                saveProfiles()
                renderProfiles()
                toast("«${profile.name}» quedó vinculada al identificador ${chosen.name}.")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun selectVoice(profile: Profile) {
        if (profile.engineVoiceId.isBlank()) {
            toast("Primero vincula esta referencia con una voz disponible del motor.")
            return
        }
        val availableVoice = engineVoices.firstOrNull { it.name == profile.engineVoiceId }
        if (availableVoice == null) {
            toast("La voz vinculada ya no aparece en Pocket TTS. Pulsa Vincular y selecciónala otra vez.")
            return
        }
        getSharedPreferences("voiceclone", MODE_PRIVATE).edit()
            .putString("selected_tts_voice", availableVoice.name)
            .putString("selected_profile_name", profile.name)
            .putString("selected_profile_id", profile.id)
            .apply()
        status.text = "Voz activa: ${profile.name}"
        toast("${profile.name} seleccionada. Esta será la voz que se aplicará al generar.")
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
            card.addView(text(if (linked) "Nombre guardado: ${profile.name}\nVoz del motor: ${profile.engineVoiceId}" else "Nombre guardado: ${profile.name}\nPendiente de vincular con Pocket TTS", 11, if (linked) 0xFF78D6B0.toInt() else 0xFFF0C27A.toInt()))
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


    private fun openExportPicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, "VOICE-CLONE-AI-voces.zip")
        }
        startActivityForResult(intent, exportCode)
    }

    private fun openImportPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(intent, importCode)
    }

    private fun exportLibrary(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { raw ->
                ZipOutputStream(raw).use { zip ->
                    val metadata = JSONArray()
                    profiles.forEach { profile ->
                        val file = File(profile.audioPath)
                        if (file.exists()) {
                            metadata.put(JSONObject().apply {
                                put("id", profile.id)
                                put("name", profile.name)
                                put("fileName", file.name)
                                put("engineVoiceId", profile.engineVoiceId)
                            })
                        }
                    }
                    zip.putNextEntry(ZipEntry("profiles.json"))
                    zip.write(metadata.toString().toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                    profiles.forEach { profile ->
                        val file = File(profile.audioPath)
                        if (file.exists()) {
                            zip.putNextEntry(ZipEntry("audio/${file.name}"))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            } ?: throw IllegalStateException("Android no permitió guardar el ZIP.")
            toast("Biblioteca exportada. Transfiere el ZIP al otro teléfono.")
        } catch (e: Exception) {
            toast("No se pudo exportar la biblioteca: ${e.message}")
        }
    }

    private fun importLibrary(uri: Uri) {
        try {
            val dir = File(filesDir, "voice_library").apply { mkdirs() }
            val importedFiles = mutableMapOf<String, File>()
            var metadataText: String? = null
            contentResolver.openInputStream(uri)?.use { raw ->
                ZipInputStream(raw).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name == "profiles.json") {
                            metadataText = zip.readBytes().toString(Charsets.UTF_8)
                        } else if (!entry.isDirectory && entry.name.startsWith("audio/")) {
                            val originalName = entry.name.substringAfter("audio/")
                            val safeName = originalName.substringAfterLast("/")
                                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                            if (safeName.isNotBlank() && !safeName.contains("..")) {
                                val target = File(dir, "imported_${System.currentTimeMillis()}_${safeName}")
                                target.outputStream().use { output -> zip.copyTo(output) }
                                importedFiles[originalName] = target
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: throw IllegalStateException("No se pudo abrir el ZIP.")
            val rawMetadata = metadataText ?: throw IllegalStateException("El ZIP no contiene perfiles de voz.")
            val metadata = JSONArray(rawMetadata)
            var added = 0
            for (i in 0 until metadata.length()) {
                val item = metadata.getJSONObject(i)
                val originalName = item.optString("fileName")
                val file = importedFiles[originalName] ?: continue
                profiles.add(Profile(
                    "${System.currentTimeMillis()}_${i}",
                    item.optString("name", "Voz importada"),
                    file.absolutePath,
                    item.optString("engineVoiceId", "")
                ))
                added++
            }
            saveProfiles()
            renderProfiles()
            toast("Se importaron ${added} voces. En este teléfono también debes importar las referencias en Pocket TTS y volver a vincularlas si sus identificadores son distintos.")
        } catch (e: Exception) {
            toast("No se pudo importar la biblioteca: ${e.message}")
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

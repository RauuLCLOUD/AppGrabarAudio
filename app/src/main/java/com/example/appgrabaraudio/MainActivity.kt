package com.example.appgrabaraudio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.appgrabaraudio.ui.theme.AppGrabarAudioTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var audioFile: File? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Se necesita el permiso de micrófono para grabar",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasMicPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        val dao = AppDatabase.getDatabase(applicationContext).recordingDao()

        setContent {
            AppGrabarAudioTheme {
                var currentScreen by remember { mutableStateOf("main") }
                var isRecording by remember { mutableStateOf(false) }
                var allRecordings by remember { mutableStateOf<List<AudioRecord>>(emptyList()) }

                val prefs = remember { getSharedPreferences("prefs", MODE_PRIVATE) }
                var lastPlayedId by remember { mutableStateOf(prefs.getInt("last_played_id", -1)) }
                val lastAudio = allRecordings.firstOrNull { it.id == lastPlayedId }

                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    dao.getAllRecordings().collect { list ->
                        allRecordings = list
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (currentScreen == "main") {
                        MainScreen(
                            isRecording = isRecording,
                            lastAudio = lastAudio,
                            onStartRecord = {
                                if (!hasMicPermission()) {
                                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    val file = File(filesDir, "audio_${System.currentTimeMillis()}.m4a")
                                    if (startRecording(file)) {
                                        audioFile = file
                                        isRecording = true
                                    } else {
                                        file.delete()
                                        Toast.makeText(
                                            this@MainActivity,
                                            "No se pudo iniciar la grabación",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onStopRecord = {
                                val ok = stopRecording()
                                isRecording = false
                                val file = audioFile
                                audioFile = null
                                if (file != null) {
                                    if (ok) {
                                        coroutineScope.launch {
                                            dao.insertRecording(
                                                AudioRecord(
                                                    filePath = file.absolutePath,
                                                    fileName = file.name,
                                                    timestamp = System.currentTimeMillis()
                                                )
                                            )
                                        }
                                    } else {
                                        file.delete()
                                        Toast.makeText(
                                            this@MainActivity,
                                            "La grabación fue demasiado corta",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onPlayLastAudio = {
                                lastAudio?.let { playAudio(it.filePath) }
                            },
                            onOpenHistory = { currentScreen = "history" }
                        )
                    } else {
                        HistoryScreen(
                            recordings = allRecordings,
                            onPlayAudio = { record ->
                                lastPlayedId = record.id
                                prefs.edit().putInt("last_played_id", record.id).apply()
                                playAudio(record.filePath)
                            },
                            onBack = { currentScreen = "main" }
                        )
                    }
                }
            }
        }
    }

    private fun startRecording(file: File): Boolean {
        return try {
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            r.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = r
            true
        } catch (e: Exception) {
            e.printStackTrace()
            recorder?.release()
            recorder = null
            false
        }
    }

    private fun stopRecording(): Boolean {
        val r = recorder ?: return false
        recorder = null
        return try {
            r.stop()
            true
        } catch (e: RuntimeException) {
            e.printStackTrace()
            false
        } finally {
            r.release()
        }
    }

    private fun playAudio(path: String) {
        val file = File(path)
        if (!file.exists() || file.length() == 0L) {
            Toast.makeText(this, "El archivo no existe o esta vacio", Toast.LENGTH_LONG).show()
            return
        }
        try {
            player?.release()
            player = null
            val p = MediaPlayer()
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            p.setDataSource(path)
            p.setOnCompletionListener {
                it.release()
                if (player === it) player = null
            }
            p.prepare()
            p.start()
            player = p
            Toast.makeText(this, "Reproduciendo (${file.length() / 1024} KB)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("AppGrabarAudio", "Error al reproducir $path", e)
            player?.release()
            player = null
            Toast.makeText(this, "Error al reproducir: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder?.release()
        recorder = null
        player?.release()
        player = null
    }
}

@Composable
fun MainScreen(
    isRecording: Boolean,
    lastAudio: AudioRecord?,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onPlayLastAudio: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Grabadora de audio")

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = if (isRecording) "Grabando..." else "Listo para grabar")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onStartRecord, enabled = !isRecording) {
            Text("Grabar")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onStopRecord, enabled = isRecording) {
            Text("Parar")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Ultimo audio escuchado:")

        if (lastAudio != null) {
            Text(text = lastAudio.fileName)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onPlayLastAudio) {
                Text("Reproducir")
            }
        } else {
            Text(text = "Todavia no has escuchado ningun audio")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onOpenHistory) {
            Text("Historial")
        }
    }
}

@Composable
fun HistoryScreen(
    recordings: List<AudioRecord>,
    onPlayAudio: (AudioRecord) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Button(onClick = onBack) {
            Text("Volver")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Historial de audios")

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(recordings, key = { it.id }) { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = record.fileName, modifier = Modifier.weight(1f))
                    Button(onClick = { onPlayAudio(record) }) {
                        Text("Play")
                    }
                }
            }
        }
    }
}
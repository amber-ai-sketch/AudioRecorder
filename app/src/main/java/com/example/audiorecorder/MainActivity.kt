package com.example.audiorecorder

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.audiorecorder.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var outputFile: String = ""
    private var amplitudeJob: Job? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            setupUI()
        } else {
            Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestPermissions()
        setupUI()
        observeViewModel()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun setupUI() {
        binding.btnRecord.setOnClickListener { startRecording() }
        binding.btnStop.setOnClickListener { 
            if (viewModel.isRecording.value) stopRecording() 
            else if (viewModel.isPlaying.value) stopPlaying()
        }
        binding.btnPlay.setOnClickListener { playRecording() }
        binding.btnRecordings.setOnClickListener {
            Toast.makeText(this, "录音列表功能即将上线", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.timerText.collect { time ->
                        binding.tvTimer.text = time
                    }
                }
                launch {
                    viewModel.isRecording.collect { isRecording ->
                        updateUIState(isRecording, viewModel.isPlaying.value)
                    }
                }
                launch {
                    viewModel.isPlaying.collect { isPlaying ->
                        updateUIState(viewModel.isRecording.value, isPlaying)
                    }
                }
                launch {
                    viewModel.amplitude.collect { amp ->
                        binding.waveformView.addAmplitude(amp)
                    }
                }
            }
        }
    }

    private fun updateUIState(isRecording: Boolean, isPlaying: Boolean) {
        with(binding) {
            when {
                isRecording -> {
                    tvStatus.text = getString(R.string.recording)
                    tvStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                    tvTimer.visibility = View.VISIBLE
                    waveformView.visibility = View.VISIBLE
                    btnRecord.isEnabled = false
                    btnStop.isEnabled = true
                    btnPlay.isEnabled = false
                    btnRecord.animate().scaleX(1.2f).scaleY(1.2f).setDuration(500).start()
                }
                isPlaying -> {
                    tvStatus.text = getString(R.string.playing)
                    tvStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))
                    tvTimer.visibility = View.INVISIBLE
                    waveformView.visibility = View.GONE
                    btnRecord.isEnabled = false
                    btnStop.isEnabled = true
                    btnPlay.isEnabled = true
                    btnPlay.setImageResource(R.drawable.ic_pause)
                    btnRecord.scaleX = 1.0f
                    btnRecord.scaleY = 1.0f
                }
                else -> {
                    tvStatus.text = getString(R.string.ready)
                    tvStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary_color))
                    tvTimer.visibility = View.INVISIBLE
                    waveformView.visibility = View.GONE
                    waveformView.clear()
                    btnRecord.isEnabled = true
                    btnStop.isEnabled = false
                    btnPlay.isEnabled = outputFile.isNotEmpty() && File(outputFile).exists()
                    btnPlay.setImageResource(R.drawable.ic_play)
                    btnRecord.scaleX = 1.0f
                    btnRecord.scaleY = 1.0f
                }
            }

            if (outputFile.isNotEmpty() && File(outputFile).exists()) {
                tvFileInfo.text = getString(R.string.recent_file, File(outputFile).name)
            }
        }
    }

    private fun startRecording() {
        try {
            stopPlaying()
            
            val directory = File(getExternalFilesDir(null), "Recordings")
            if (!directory.exists()) directory.mkdirs()
            
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(directory, "REC_$timeStamp.mp3")
            outputFile = file.absolutePath

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)
                prepare()
                start()
            }

            viewModel.startTimer()
            startAmplitudePolling()
            Toast.makeText(this, getString(R.string.recording), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.recording_failed), Toast.LENGTH_SHORT).show()
            viewModel.stopTimer()
        }
    }

    private fun startAmplitudePolling() {
        amplitudeJob?.cancel()
        amplitudeJob = lifecycleScope.launch {
            while (isActive && viewModel.isRecording.value) {
                mediaRecorder?.let {
                    viewModel.updateAmplitude(it.maxAmplitude.toFloat())
                }
                delay(100)
            }
        }
    }

    private fun stopRecording() {
        try {
            amplitudeJob?.cancel()
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            viewModel.stopTimer()
            Toast.makeText(this, getString(R.string.recording_saved), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playRecording() {
        if (viewModel.isPlaying.value) {
            stopPlaying()
            return
        }

        if (outputFile.isEmpty() || !File(outputFile).exists()) return

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(outputFile)
                prepare()
                start()
                setOnCompletionListener { stopPlaying() }
            }
            viewModel.setPlaying(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.play_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPlaying() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
            viewModel.setPlaying(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        amplitudeJob?.cancel()
        mediaRecorder?.release()
        mediaPlayer?.release()
    }
}

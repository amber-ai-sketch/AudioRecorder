package com.example.audiorecorder

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var isPlaying = false
    private var outputFile: String = ""
    
    private lateinit var btnRecord: Button
    private lateinit var btnStop: Button
    private lateinit var btnPlay: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvFileInfo: TextView

    companion object {
        private const val REQUEST_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        checkPermissions()
    }

    private fun initViews() {
        btnRecord = findViewById(R.id.btnRecord)
        btnStop = findViewById(R.id.btnStop)
        btnPlay = findViewById(R.id.btnPlay)
        tvStatus = findViewById(R.id.tvStatus)
        tvFileInfo = findViewById(R.id.tvFileInfo)

        btnRecord.setOnClickListener { startRecording() }
        btnStop.setOnClickListener { stopRecording() }
        btnPlay.setOnClickListener { playRecording() }

        updateUI()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val needPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needPermissions.toTypedArray(), REQUEST_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "权限已获取", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "需要权限才能录音", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startRecording() {
        if (isRecording) return

        try {
            // 停止播放
            stopPlaying()

            // 创建录音文件
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "REC_$timeStamp.mp3"
            
            // 使用应用私有目录
            val directory = File(getExternalFilesDir(null), "Recordings")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            val file = File(directory, fileName)
            outputFile = file.absolutePath

            // 配置 MediaRecorder
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)
                prepare()
                start()
            }

            isRecording = true
            updateUI()
            Toast.makeText(this, "开始录音", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "录音失败: ${e.message}", Toast.LENGTH_LONG).show()
            resetRecorder()
        }
    }

    private fun stopRecording() {
        if (!isRecording) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            updateUI()
            Toast.makeText(this, "录音已保存", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "停止录音失败: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            resetRecorder()
        }
    }

    private fun playRecording() {
        if (outputFile.isEmpty() || !File(outputFile).exists()) {
            Toast.makeText(this, "没有可播放的录音", Toast.LENGTH_SHORT).show()
            return
        }

        if (isPlaying) {
            stopPlaying()
            return
        }

        try {
            // 停止录音
            if (isRecording) {
                stopRecording()
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(outputFile)
                prepare()
                start()
                setOnCompletionListener {
                    stopPlaying()
                }
            }

            isPlaying = true
            updateUI()
            Toast.makeText(this, "开始播放", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "播放失败: ${e.message}", Toast.LENGTH_LONG).show()
            resetPlayer()
        }
    }

    private fun stopPlaying() {
        if (!isPlaying) return

        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            isPlaying = false
            updateUI()

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            resetPlayer()
        }
    }

    private fun resetRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
        isRecording = false
        updateUI()
    }

    private fun resetPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        updateUI()
    }

    private fun updateUI() {
        when {
            isRecording -> {
                tvStatus.text = "🔴 正在录音..."
                tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                btnRecord.isEnabled = false
                btnStop.isEnabled = true
                btnPlay.isEnabled = false
                btnRecord.text = "录音中"
                btnStop.text = "⏹️ 停止"
                btnPlay.text = "播放"
            }
            isPlaying -> {
                tvStatus.text = "▶️ 正在播放..."
                tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                btnRecord.isEnabled = false
                btnStop.isEnabled = true
                btnPlay.isEnabled = true
                btnRecord.text = "录音"
                btnStop.text = "⏹️ 停止"
                btnPlay.text = "暂停"
            }
            else -> {
                tvStatus.text = "准备录音"
                tvStatus.setTextColor(getColor(android.R.color.black))
                btnRecord.isEnabled = true
                btnStop.isEnabled = false
                btnPlay.isEnabled = outputFile.isNotEmpty() && File(outputFile).exists()
                btnRecord.text = "🔴 录音"
                btnStop.text = "停止"
                btnPlay.text = "▶️ 播放"
            }
        }

        if (outputFile.isNotEmpty() && File(outputFile).exists()) {
            tvFileInfo.text = "📁 最近文件: ${File(outputFile).name}"
        } else {
            tvFileInfo.text = "暂无录音文件"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        resetRecorder()
        resetPlayer()
    }
}
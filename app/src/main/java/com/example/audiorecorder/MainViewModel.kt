package com.example.audiorecorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _timerText = MutableStateFlow("00:00")
    val timerText: StateFlow<String> = _timerText.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _amplitude = MutableSharedFlow<Float>()
    val amplitude: SharedFlow<Float> = _amplitude.asSharedFlow()

    private var timerJob: Job? = null
    private var seconds = 0

    fun startTimer() {
        seconds = 0
        _isRecording.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                _timerText.value = formatTime(seconds)
                delay(1000)
                seconds++
            }
        }
    }

    fun stopTimer() {
        _isRecording.value = false
        timerJob?.cancel()
        _timerText.value = "00:00"
        seconds = 0
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun updateAmplitude(amp: Float) {
        viewModelScope.launch {
            _amplitude.emit(amp)
        }
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

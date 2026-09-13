package com.haxtech.haxtracker.ui.match

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haxtech.haxtracker.audio.ScoreTtsAnnouncer
import com.haxtech.haxtracker.core.engine.GameEngine
import com.haxtech.haxtracker.core.model.GameAction
import com.haxtech.haxtracker.core.model.MatchConfig
import com.haxtech.haxtracker.core.model.MatchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val announcer = ScoreTtsAnnouncer(application)

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val _matchState = MutableStateFlow(GameEngine.newMatch(MatchConfig.defaultPickleballDoubles()))
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private val _isVolumeControlEnabled = MutableStateFlow(true)
    val isVolumeControlEnabled: StateFlow<Boolean> = _isVolumeControlEnabled.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    init {
        announcer.isEnabled = _isTtsEnabled.value
    }

    fun startMatch(config: MatchConfig) {
        val fresh = GameEngine.newMatch(config)
        _matchState.value = fresh
        triggerHaptic(50)
        speakAnnouncement(fresh.spokenAnnouncement)
    }

    fun onAction(action: GameAction) {
        val prev = _matchState.value
        val updated = GameEngine.process(prev, action)
        _matchState.value = updated

        when (action) {
            is GameAction.Undo -> {
                triggerHaptic(30)
                speakAnnouncement("Undo. ${updated.calloutScore}")
            }
            is GameAction.StartNextGame -> {
                triggerHaptic(100)
                speakAnnouncement("Starting Game ${updated.currentGameIndex + 1}")
            }
            is GameAction.PointTeamA,
            is GameAction.PointTeamB,
            is GameAction.PointServingTeam,
            is GameAction.PointReceivingTeam -> {
                if (updated.isMatchFinished) {
                    triggerPatternHaptic(longArrayOf(0, 150, 100, 250))
                } else if (updated.isGameFinished) {
                    triggerPatternHaptic(longArrayOf(0, 100, 80, 180))
                } else {
                    triggerHaptic(40)
                }
                speakAnnouncement(updated.spokenAnnouncement)
            }
            else -> {
                triggerHaptic(25)
            }
        }
    }

    fun handleVolumeKey(isVolumeUp: Boolean): Boolean {
        if (!_isVolumeControlEnabled.value) return false
        val state = _matchState.value
        if (state.isGameFinished || state.isMatchFinished) return false

        if (isVolumeUp) {
            onAction(GameAction.PointTeamB)
        } else {
            onAction(GameAction.PointTeamA)
        }
        return true
    }

    fun toggleVolumeControl() {
        _isVolumeControlEnabled.value = !_isVolumeControlEnabled.value
    }

    fun toggleTts() {
        val next = !_isTtsEnabled.value
        _isTtsEnabled.value = next
        announcer.isEnabled = next
        if (next) {
            announcer.speak("Voice announcements enabled")
        }
    }

    private fun speakAnnouncement(text: String) {
        if (_isTtsEnabled.value) {
            viewModelScope.launch {
                announcer.speak(text)
            }
        }
    }

    private fun triggerHaptic(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun triggerPatternHaptic(pattern: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    override fun onCleared() {
        super.onCleared()
        announcer.shutdown()
    }
}

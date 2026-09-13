package com.haxtech.haxtracker.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class ScoreTtsAnnouncer(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady: Boolean = false
    var isEnabled: Boolean = true

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(1.1f) // Slightly faster for athletic snappy callouts
            isReady = true
        }
    }

    fun speak(phrase: String) {
        if (!isEnabled || !isReady || phrase.isBlank()) return
        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "score_announcement")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}

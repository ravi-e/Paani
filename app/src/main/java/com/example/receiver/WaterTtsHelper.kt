package com.example.receiver

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Thin wrapper around Android TextToSpeech that:
 *  - Initialises the engine on demand
 *  - Sets AudioAttributes to USAGE_NOTIFICATION so the OS respects
 *    Do Not Disturb, stream volume and Bluetooth routing automatically
 *  - Shuts the engine down cleanly after the utterance finishes
 *    (important when called from a background Worker)
 */
object WaterTtsHelper {

    private const val TAG = "WaterTtsHelper"
    private const val UTTERANCE_ID = "water_reminder_cue"

    /**
     * Speak [text] once using the system TTS engine.
     * Safe to call from a background coroutine / Worker thread.
     * Blocks the calling thread for up to [timeoutMs] ms waiting for
     * TTS initialisation, then returns immediately (speech plays async).
     */
    fun speak(context: Context, text: String, timeoutMs: Long = 4_000L) {
        val latch = java.util.concurrent.CountDownLatch(1)
        var tts: TextToSpeech? = null

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    // Fall back to English if device language isn't supported
                    tts?.setLanguage(Locale.ENGLISH)
                }

                // Route audio through notification stream so volume knob works
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttrs)

                // Shut down cleanly after the utterance ends
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS utterance complete, shutting down engine.")
                        tts?.shutdown()
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.w(TAG, "TTS utterance error, shutting down engine.")
                        tts?.shutdown()
                    }
                })

                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID)
                }
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
                Log.d(TAG, "TTS speaking: \"$text\"")
            } else {
                Log.w(TAG, "TTS initialisation failed with status: $status")
            }
            latch.countDown()
        }

        // Wait briefly for the engine to initialise before returning
        // (the actual speech plays asynchronously after this)
        latch.await(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
    }
}

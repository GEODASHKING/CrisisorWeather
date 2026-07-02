package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class EasAlarmPlayer(private val context: Context) : TextToSpeech.OnInitListener {
    private val TAG = "EasAlarmPlayer"
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var audioTrack: AudioTrack? = null
    private var isPlayingAlarm = false
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var alarmJob: Job? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS Language is not supported or missing data")
            } else {
                isTtsInitialized = true
                Log.d(TAG, "TextToSpeech successfully initialized")
            }
        } else {
            Log.e(TAG, "TTS Initialization failed")
        }
    }

    /**
     * Synthesizes and plays the classic EAS 853 Hz + 960 Hz dual-frequency attention signal.
     */
    fun playEasAlarmAndSpeak(alertText: String, durationSeconds: Double = 3.0) {
        stop() // Stop any current broadcast before starting a new one

        isPlayingAlarm = true
        alarmJob = coroutineScope.launch {
            try {
                // Generate EAS dual tone buffer
                val sampleRate = 44100
                val numSamples = (sampleRate * durationSeconds).toInt()
                val samples = ShortArray(numSamples)
                
                // Classic EAS dual frequencies: 853 Hz and 960 Hz
                val freq1 = 853.0
                val freq2 = 960.0

                for (i in 0 until numSamples) {
                    val angle1 = 2.0 * Math.PI * freq1 * (i.toDouble() / sampleRate)
                    val angle2 = 2.0 * Math.PI * freq2 * (i.toDouble() / sampleRate)
                    // Blend both sine waves and scale to short range
                    val sampleValue = (Math.sin(angle1) + Math.sin(angle2)) / 2.0
                    samples[i] = (sampleValue * Short.MAX_VALUE).toInt().toShort()
                }

                // Initialize AudioTrack for system-wide interruption streaming
                val bufferSize = Math.max(
                    numSamples * 2,
                    AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                )

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM) // Classifies as Alarm to override do-not-disturb if possible
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack?.let { track ->
                    track.write(samples, 0, numSamples)
                    track.play()
                    
                    // Wait for the duration of the tone before speaking
                    val sleepMs = (durationSeconds * 1000).toLong()
                    delay(sleepMs)
                    
                    track.stop()
                    track.release()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing generated EAS tone", e)
            } finally {
                audioTrack = null
                isPlayingAlarm = false
                
                // Speak the alert text immediately following the alarm tone
                speakText(alertText)
            }
        }
    }

    private fun speakText(text: String) {
        if (tts != null && isTtsInitialized) {
            Log.d(TAG, "EAS TTS speaking: $text")
            // Configure parameters for a standard emergency announcement voice (slightly slower, formal)
            tts?.setSpeechRate(0.85f) // Slightly slower for emergency scanning
            tts?.setPitch(0.9f)       // Deep, authoritative tone
            
            // Queue the message
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "EAS_TTS_ID")
        } else {
            Log.e(TAG, "TTS not ready for speech. Initialized: $isTtsInitialized")
        }
    }

    /**
     * Instantly halts both the synthesized tone generator and TextToSpeech output.
     */
    fun stop() {
        try {
            alarmJob?.cancel()
            alarmJob = null
            
            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
            audioTrack = null
            isPlayingAlarm = false

            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping EAS player", e)
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isTtsInitialized = false
    }
}

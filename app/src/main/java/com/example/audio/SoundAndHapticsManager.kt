package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Studio-grade programmatic acoustic synthesizer & haptic engine.
 * Generates warm, organic marimba/ceramic tones using 16-bit PCM synthesis
 * so audio is crisp, responsive, non-irritating, and requires zero external binary assets.
 */
class SoundAndHapticsManager(private val context: Context? = null) {

    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ambientMusicJob: Job? = null

    @Volatile
    var soundEnabled: Boolean = true

    @Volatile
    var soundVolume: Float = 0.8f

    @Volatile
    var musicEnabled: Boolean = false
        set(value) {
            field = value
            if (value) startAmbientMusicIfNeeded() else stopAmbientMusic()
        }

    @Volatile
    var hapticsEnabled: Boolean = true

    fun playMarkX() {
        triggerHaptic(durationMs = 18L, amplitude = 140)
        if (!soundEnabled || soundVolume <= 0.01f) return
        // Warm wooden marimba double-harmonic tap (D5 -> A5)
        audioScope.launch {
            playSynthesizedChord(
                frequencies = listOf(587.33, 880.0),
                durationMs = 115,
                decayRate = 18.0,
                gain = soundVolume * 0.55f
            )
        }
    }

    fun playMarkO() {
        triggerHaptic(durationMs = 22L, amplitude = 120)
        if (!soundEnabled || soundVolume <= 0.01f) return
        // Resonant ceramic chime (F#5 -> C#6)
        audioScope.launch {
            playSynthesizedChord(
                frequencies = listOf(739.99, 1108.73),
                durationMs = 135,
                decayRate = 15.0,
                gain = soundVolume * 0.50f
            )
        }
    }

    fun playButtonTap() {
        triggerHaptic(durationMs = 10L, amplitude = 80)
        if (!soundEnabled || soundVolume <= 0.01f) return
        audioScope.launch {
            playSynthesizedChord(
                frequencies = listOf(523.25),
                durationMs = 55,
                decayRate = 32.0,
                gain = soundVolume * 0.32f
            )
        }
    }

    fun playUndoOrHint() {
        triggerHaptic(durationMs = 14L, amplitude = 95)
        if (!soundEnabled || soundVolume <= 0.01f) return
        audioScope.launch {
            playSynthesizedChord(
                frequencies = listOf(440.0, 659.25),
                durationMs = 95,
                decayRate = 22.0,
                gain = soundVolume * 0.40f
            )
        }
    }

    fun playVictory() {
        triggerHapticPattern(longArrayOf(0, 30, 55, 45, 65, 70))
        if (!soundEnabled || soundVolume <= 0.01f) return
        // Ascending warm studio arpeggio (D Major 9: D5 - F#5 - A5 - E6)
        audioScope.launch {
            val notes = listOf(587.33, 739.99, 880.00, 1174.66)
            for ((idx, freq) in notes.withIndex()) {
                val isLast = idx == notes.lastIndex
                playSynthesizedChord(
                    frequencies = if (isLast) listOf(freq, 587.33) else listOf(freq),
                    durationMs = if (isLast) 320 else 110,
                    decayRate = if (isLast) 6.5 else 14.0,
                    gain = soundVolume * 0.58f
                )
                if (!isLast) delay(85L)
            }
        }
    }

    fun playDefeat() {
        triggerHapticPattern(longArrayOf(0, 45, 80, 60))
        if (!soundEnabled || soundVolume <= 0.01f) return
        // Contemplative descending minor cadence (A4 -> F4 -> D4)
        audioScope.launch {
            val notes = listOf(440.0, 349.23, 293.66)
            for ((idx, freq) in notes.withIndex()) {
                val isLast = idx == notes.lastIndex
                playSynthesizedChord(
                    frequencies = listOf(freq),
                    durationMs = if (isLast) 280 else 120,
                    decayRate = if (isLast) 7.5 else 14.0,
                    gain = soundVolume * 0.48f
                )
                if (!isLast) delay(95L)
            }
        }
    }

    fun playDraw() {
        triggerHaptic(durationMs = 30L, amplitude = 100)
        if (!soundEnabled || soundVolume <= 0.01f) return
        // Balanced suspended fourth resolution (G4 + C5 + D5)
        audioScope.launch {
            playSynthesizedChord(
                frequencies = listOf(392.00, 523.25, 587.33),
                durationMs = 260,
                decayRate = 9.0,
                gain = soundVolume * 0.45f
            )
        }
    }

    private fun startAmbientMusicIfNeeded() {
        if (ambientMusicJob?.isActive == true) return
        ambientMusicJob = audioScope.launch {
            val chords = listOf(
                listOf(293.66, 440.00, 554.37), // Dmaj7 warm pad
                listOf(246.94, 369.99, 493.88), // Bm7
                listOf(196.00, 293.66, 369.99), // Gmaj7
                listOf(220.00, 329.63, 440.00)  // Asus4
            )
            var index = 0
            while (isActive && musicEnabled) {
                if (soundVolume > 0.01f) {
                    playSynthesizedChord(
                        frequencies = chords[index % chords.size],
                        durationMs = 900,
                        decayRate = 2.8,
                        gain = (soundVolume * 0.14f).coerceAtMost(0.22f)
                    )
                }
                index++
                delay(2400L)
            }
        }
    }

    fun stopAmbientMusic() {
        ambientMusicJob?.cancel()
        ambientMusicJob = null
    }

    private fun playSynthesizedChord(
        frequencies: List<Double>,
        durationMs: Int,
        decayRate: Double,
        gain: Float
    ) {
        try {
            val sampleRate = 24000
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(240)
            val pcmBuffer = ShortArray(numSamples)
            val safeFreqCount = frequencies.size.coerceAtLeast(1)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // Smooth attack (first 6ms) + exponential acoustic decay
                val attack = (t / 0.006).coerceAtMost(1.0)
                val envelope = attack * exp(-decayRate * t)

                var sampleSum = 0.0
                for (freq in frequencies) {
                    // Fundamental + subtle 2nd harmonic warmth
                    sampleSum += sin(2.0 * PI * freq * t) + 0.22 * sin(4.0 * PI * freq * t)
                }
                val normalized = (sampleSum / (safeFreqCount * 1.22)) * envelope * gain.coerceIn(0f, 1f)
                pcmBuffer[i] = (normalized * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }

            val byteSize = numSamples * 2
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteSize.coerceAtLeast(AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(pcmBuffer, 0, numSamples)
            audioTrack.play()
            Thread.sleep(durationMs.toLong() + 30L)
            audioTrack.stop()
            audioTrack.release()
        } catch (_: Throwable) {
            // Gracefully ignore in JVM unit tests or devices without audio hardware
        }
    }

    private fun triggerHaptic(durationMs: Long, amplitude: Int) {
        if (!hapticsEnabled) return
        try {
            val vibrator = getVibrator() ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Throwable) {
            // Ignore in test environments
        }
    }

    private fun triggerHapticPattern(timings: LongArray) {
        if (!hapticsEnabled) return
        try {
            val vibrator = getVibrator() ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        } catch (_: Throwable) {
            // Ignore in test environments
        }
    }

    private fun getVibrator(): Vibrator? {
        val ctx = context ?: return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }
}

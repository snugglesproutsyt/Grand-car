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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural Audio Synthesizer & Haptic Generator.
 * Generates crisp tactical gunshots, explosions, reload mechanics, and hit sounds
 * on-the-fly with zero network dependency.
 */
class SoundSynth(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isSoundEnabled: Boolean = true
    var isHapticsEnabled: Boolean = true

    // Pre-computed audio buffers for zero-latency instant playback
    private val pistolBuffer by lazy { generateGunshot(durationMs = 90, baseFreq = 420.0, noiseFactor = 0.85f) }
    private val rifleBuffer by lazy { generateGunshot(durationMs = 70, baseFreq = 340.0, noiseFactor = 0.75f) }
    private val shotgunBuffer by lazy { generateGunshot(durationMs = 150, baseFreq = 180.0, noiseFactor = 0.95f) }
    private val sniperBuffer by lazy { generateSniper(durationMs = 180) }
    private val rocketLaunchBuffer by lazy { generateRocketLaunch(durationMs = 220) }
    private val explosionBuffer by lazy { generateExplosion(durationMs = 380) }
    private val hitBuffer by lazy { generateTone(durationMs = 35, freq = 1200.0) }
    private val killBuffer by lazy { generateKillChime() }
    private val reloadBuffer by lazy { generateReloadClick() }
    private val emptyBuffer by lazy { generateTone(durationMs = 25, freq = 800.0) }

    fun playPistol() {
        if (!isSoundEnabled) return
        playSoundBuffer(pistolBuffer)
        vibrate(25, 120)
    }

    fun playRifle() {
        if (!isSoundEnabled) return
        playSoundBuffer(rifleBuffer)
        vibrate(20, 150)
    }

    fun playShotgun() {
        if (!isSoundEnabled) return
        playSoundBuffer(shotgunBuffer)
        vibrate(55, 255)
    }

    fun playSniper() {
        if (!isSoundEnabled) return
        playSoundBuffer(sniperBuffer)
        vibrate(60, 240)
    }

    fun playRocketLaunch() {
        if (!isSoundEnabled) return
        playSoundBuffer(rocketLaunchBuffer)
        vibrate(40, 180)
    }

    fun playExplosion() {
        if (!isSoundEnabled) return
        playSoundBuffer(explosionBuffer)
        vibrate(85, 255)
    }

    fun playHitMarker() {
        if (!isSoundEnabled) return
        playSoundBuffer(hitBuffer)
        vibrate(12, 100)
    }

    fun playKill() {
        if (!isSoundEnabled) return
        playSoundBuffer(killBuffer)
        vibrate(35, 180)
    }

    fun playReload() {
        if (!isSoundEnabled) return
        playSoundBuffer(reloadBuffer)
        vibrate(15, 60)
    }

    fun playEmptyClick() {
        if (!isSoundEnabled) return
        playSoundBuffer(emptyBuffer)
        vibrate(10, 40)
    }

    fun playPlayerHurt() {
        if (!isSoundEnabled) return
        playSoundBuffer(generateTone(durationMs = 70, freq = 140.0))
        vibrate(65, 230)
    }

    private fun vibrate(durationMs: Long, amplitude: Int = 180) {
        if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {
            // Safe fallback if permission denied
        }
    }

    private fun playSoundBuffer(samples: ShortArray) {
        scope.launch {
            try {
                val sampleRate = 22050
                val minSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = samples.size.coerceAtLeast(minSize)

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
                    .setBufferSizeInBytes(bufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()

                // Release audio track when playback completes
                val sleepTime = (samples.size * 1000L / sampleRate) + 60
                kotlinx.coroutines.delay(sleepTime)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Ignore audio track concurrency limits gracefully
            }
        }
    }

    private fun generateGunshot(durationMs: Int, baseFreq: Double, noiseFactor: Float): ShortArray {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val decay = (1.0 - progress) * (1.0 - progress)
            val freq = baseFreq * (1.0 - progress * 0.7)
            phase += 2.0 * Math.PI * freq / sampleRate

            val sine = sin(phase)
            val noise = (Random.nextDouble() * 2.0 - 1.0)
            val mixed = (sine * (1.0f - noiseFactor) + noise * noiseFactor) * decay
            buffer[i] = (mixed * Short.MAX_VALUE * 0.85).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateSniper(durationMs: Int): ShortArray {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val decay = (1.0 - progress)
            val freq = 850.0 - progress * 550.0
            val sine = sin(2.0 * Math.PI * freq * i / sampleRate)
            val crack = if (i < numSamples / 6) (Random.nextDouble() * 2.0 - 1.0) * 1.5 else (Random.nextDouble() * 0.4 - 0.2)
            val mixed = (sine * 0.4 + crack * 0.6) * decay
            buffer[i] = (mixed * Short.MAX_VALUE * 0.9).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateRocketLaunch(durationMs: Int): ShortArray {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val attackDecay = if (progress < 0.2) progress / 0.2 else (1.0 - progress)
            val noise = (Random.nextDouble() * 2.0 - 1.0)
            val lowRumble = sin(2.0 * Math.PI * (120.0 + progress * 80.0) * i / sampleRate)
            val mixed = (noise * 0.7 + lowRumble * 0.3) * attackDecay
            buffer[i] = (mixed * Short.MAX_VALUE * 0.8).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateExplosion(durationMs: Int): ShortArray {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val decay = (1.0 - progress) * (1.0 - progress)
            val noise = (Random.nextDouble() * 2.0 - 1.0)
            val subBass = sin(2.0 * Math.PI * 65.0 * (1.0 - progress * 0.5) * i / sampleRate)
            val mixed = (noise * 0.6 + subBass * 0.4) * decay
            buffer[i] = (mixed * Short.MAX_VALUE * 0.95).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateTone(durationMs: Int, freq: Double): ShortArray {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val decay = (1.0 - progress)
            val sine = sin(2.0 * Math.PI * freq * i / sampleRate)
            buffer[i] = (sine * decay * Short.MAX_VALUE * 0.6).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateKillChime(): ShortArray {
        val sampleRate = 22050
        val numSamples = (sampleRate * 0.14).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val decay = (1.0 - progress)
            val f = if (progress < 0.5) 880.0 else 1320.0
            val sine = sin(2.0 * Math.PI * f * i / sampleRate)
            buffer[i] = (sine * decay * Short.MAX_VALUE * 0.7).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateReloadClick(): ShortArray {
        val sampleRate = 22050
        val numSamples = (sampleRate * 0.08).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val decay = (1.0 - progress)
            val noise = (Random.nextDouble() * 2.0 - 1.0)
            buffer[i] = (noise * decay * Short.MAX_VALUE * 0.5).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }
}

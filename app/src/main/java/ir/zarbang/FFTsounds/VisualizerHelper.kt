package ir.zarbang.FFTsounds.fft

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.RuntimeException
import kotlin.jvm.JvmField

class VisualizerHelper {

    private var visualizer: Visualizer? = null

    companion object {
        private const val TAG = "UnityAudioBridge_FFT"

        private val _frequencyBands = MutableStateFlow(FrequencyBands())

        @JvmField
        val frequencyBands = _frequencyBands.asStateFlow()

        private const val BASS_LOWER_HZ = 20
        private const val BASS_UPPER_HZ = 250
        private const val MID_LOWER_HZ = 251
        private const val MID_UPPER_HZ = 4000
        private const val TREBLE_LOWER_HZ = 4001
        private const val TREBLE_UPPER_HZ = 20000
    }

    fun start() {
        if (visualizer != null) return

        Log.d(TAG, "Attempting to start Visualizer on global audio output (Session ID 0)")
        try {
            visualizer = Visualizer(0).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                val captureRate = Visualizer.getMaxCaptureRate()

                val listener = object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(vis: Visualizer?, waveform: ByteArray?, rate: Int) {}
                    override fun onFftDataCapture(vis: Visualizer?, fft: ByteArray?, rate: Int) {
                        if (fft != null) {
                            processFft(fft, rate)
                        }
                    }
                }

                val status = setDataCaptureListener(listener, captureRate / 2, false, true)
                if (status != Visualizer.SUCCESS) {
                    Log.e(TAG, "setDataCaptureListener failed with status code: $status")
                    release()
                    return
                }
                enabled = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Visualizer initialization FAILED: ${e.message}", e)
            visualizer = null
        }
    }

    fun stop() {
        if (visualizer == null) return
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error while releasing visualizer: ${e.message}")
        } finally {
            visualizer = null
        }
    }

    private fun processFft(fftData: ByteArray, samplingRate: Int) {
        val fftSize = fftData.size
        val magnitudes = FloatArray(fftSize / 2)
        for (i in 0 until fftSize / 2) {
            val real = fftData[i * 2].toFloat()
            val imag = fftData[i * 2 + 1].toFloat()
            magnitudes[i] = kotlin.math.hypot(real, imag)
        }
        val nyquist = samplingRate / 2000.0
        val freqResolution = nyquist / magnitudes.size
        var bassSum = 0f; var midSum = 0f; var trebleSum = 0f
        var bassCount = 0; var midCount = 0; var trebleCount = 0
        for (i in magnitudes.indices) {
            val freq = i * freqResolution
            when {
                freq >= BASS_LOWER_HZ && freq <= BASS_UPPER_HZ -> { bassSum += magnitudes[i]; bassCount++ }
                freq >= MID_LOWER_HZ && freq <= MID_UPPER_HZ -> { midSum += magnitudes[i]; midCount++ }
                freq >= TREBLE_LOWER_HZ && freq <= TREBLE_UPPER_HZ -> { trebleSum += magnitudes[i]; trebleCount++ }
            }
        }
        _frequencyBands.value = FrequencyBands(
            bass = if (bassCount > 0) bassSum / bassCount else 0f,
            mid = if (midCount > 0) midSum / midCount else 0f,
            treble = if (trebleCount > 0) trebleSum / trebleCount else 0f
        )
    }
}
